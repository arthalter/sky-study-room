package com.sky.study;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SkyServerCoreFlowTests {

    private static final String TOKEN_HEADER = "token";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS reservation_audit_log (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    reservation_id BIGINT NOT NULL,
                    admin_id BIGINT NOT NULL,
                    old_status INT NOT NULL,
                    new_status INT NOT NULL,
                    review_remark VARCHAR(255),
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_reservation_audit_log_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
                    CONSTRAINT fk_reservation_audit_log_admin FOREIGN KEY (admin_id) REFERENCES user (id),
                    KEY idx_reservation_audit_log_reservation_id (reservation_id),
                    KEY idx_reservation_audit_log_admin_id (admin_id)
                )
                """);
        stringRedisTemplate.delete("resource:category");
        Set<String> blacklistKeys = stringRedisTemplate.keys("jwt:blacklist:*");
        if (blacklistKeys != null && !blacklistKeys.isEmpty()) {
            stringRedisTemplate.delete(blacklistKeys);
        }
    }

    @Test
    void loginUnauthorizedAndBlacklistFlow() throws Exception {
        ResponseEntity<String> unauthorized = restTemplate.getForEntity("/api/resource/category", String.class);
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String userToken = login("/api/user/login", "student", "123456");
        ResponseEntity<String> category = exchange("/api/resource/category", HttpMethod.GET, userToken, null);
        assertSuccess(category);

        ResponseEntity<String> forbidden = exchange("/api/admin/reservation/page?page=1&pageSize=10", HttpMethod.GET, userToken, null);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> logout = exchange("/api/user/logout", HttpMethod.POST, userToken, null);
        assertSuccess(logout);

        ResponseEntity<String> blacklisted = exchange("/api/resource/category", HttpMethod.GET, userToken, null);
        assertThat(blacklisted.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resourceCategoryUsesRedisCacheAndValidationErrorsAreFriendly() throws Exception {
        String userToken = login("/api/user/login", "student", "123456");

        ResponseEntity<String> category = exchange("/api/resource/category", HttpMethod.GET, userToken, null);
        assertSuccess(category);
        assertThat(Boolean.TRUE.equals(stringRedisTemplate.hasKey("resource:category"))).isTrue();
        assertThat(stringRedisTemplate.getExpire("resource:category")).isPositive();

        Map<String, Object> missingPurpose = Map.of(
                "resourceId", 1,
                "reserveDate", LocalDate.now().plusDays(5).toString(),
                "startTime", "10:00:00",
                "endTime", "11:00:00"
        );
        ResponseEntity<String> validation = exchange("/api/user/reservation/submit", HttpMethod.POST, userToken, missingPurpose);
        assertBusinessError(validation, "预约用途不能为空");

        Map<String, Object> outsideOpenTime = Map.of(
                "resourceId", 1,
                "reserveDate", LocalDate.now().plusDays(5).toString(),
                "startTime", "23:00:00",
                "endTime", "23:30:00",
                "purpose", uniquePurpose("outside-open-time")
        );
        ResponseEntity<String> openTimeError = exchange("/api/user/reservation/submit", HttpMethod.POST, userToken, outsideOpenTime);
        assertBusinessError(openTimeError, "预约时间不在资源开放时间范围内");
    }

    @Test
    void reservationApprovalConflictRejectionCancellationAndAuditFlow() throws Exception {
        String userToken = login("/api/user/login", "student", "123456");
        String adminToken = login("/api/admin/login", "admin", "123456");
        LocalDate reserveDate = uniqueFutureDate();

        String approvedPurpose = uniquePurpose("approve");
        submitReservation(userToken, 2L, reserveDate, "10:00:00", "10:30:00", approvedPurpose);
        Long approvedId = findReservationId(userToken, approvedPurpose);
        review(adminToken, approvedId, 2, "approved by test");
        assertAuditLogWritten(approvedId, 1, 2);

        ResponseEntity<String> invalidTransition = reviewResponse(adminToken, approvedId, 3, "cannot reject approved");
        assertBusinessError(invalidTransition, "预约状态不合法");

        Map<String, Object> conflict = reservationPayload(2L, reserveDate, "10:10:00", "10:20:00", uniquePurpose("conflict"));
        ResponseEntity<String> conflictResponse = exchange("/api/user/reservation/submit", HttpMethod.POST, userToken, conflict);
        assertBusinessError(conflictResponse, "预约时间冲突");

        String rejectedPurpose = uniquePurpose("reject");
        submitReservation(userToken, 3L, reserveDate, "11:00:00", "11:30:00", rejectedPurpose);
        Long rejectedId = findReservationId(userToken, rejectedPurpose);
        review(adminToken, rejectedId, 3, "rejected by test");
        assertAuditLogWritten(rejectedId, 1, 3);

        String cancelPurpose = uniquePurpose("cancel");
        submitReservation(userToken, 4L, reserveDate, "12:00:00", "12:30:00", cancelPurpose);
        Long cancelId = findReservationId(userToken, cancelPurpose);
        ResponseEntity<String> cancel = exchange("/api/user/reservation/cancel/" + cancelId, HttpMethod.POST, userToken, null);
        assertSuccess(cancel);

        ResponseEntity<String> cancelAgain = exchange("/api/user/reservation/cancel/" + cancelId, HttpMethod.POST, userToken, null);
        assertBusinessError(cancelAgain, "预约状态不合法");
    }

    private String login(String path, String name, String password) throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(path, Map.of("name", name, "password", password), String.class);
        assertSuccess(response);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("data").path("token").asText();
    }

    private void submitReservation(String token, Long resourceId, LocalDate reserveDate, String startTime, String endTime, String purpose) throws Exception {
        ResponseEntity<String> response = exchange(
                "/api/user/reservation/submit",
                HttpMethod.POST,
                token,
                reservationPayload(resourceId, reserveDate, startTime, endTime, purpose)
        );
        assertSuccess(response);
    }

    private void review(String token, Long reservationId, Integer status, String remark) throws Exception {
        assertSuccess(reviewResponse(token, reservationId, status, remark));
    }

    private ResponseEntity<String> reviewResponse(String token, Long reservationId, Integer status, String remark) {
        return exchange(
                "/api/admin/reservation/review",
                HttpMethod.POST,
                token,
                Map.of("reservationId", reservationId, "status", status, "reviewRemark", remark)
        );
    }

    private Long findReservationId(String token, String purpose) throws Exception {
        ResponseEntity<String> response = exchange("/api/user/reservation/page?page=1&pageSize=100", HttpMethod.GET, token, null);
        assertSuccess(response);
        JsonNode records = objectMapper.readTree(response.getBody()).path("data").path("records");
        for (JsonNode record : records) {
            if (purpose.equals(record.path("purpose").asText())) {
                return record.path("id").asLong();
            }
        }
        throw new AssertionError("Reservation not found for purpose: " + purpose);
    }

    private void assertAuditLogWritten(Long reservationId, Integer oldStatus, Integer newStatus) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from reservation_audit_log where reservation_id = ? and old_status = ? and new_status = ?",
                Integer.class,
                reservationId,
                oldStatus,
                newStatus
        );
        assertThat(count).isNotNull().isPositive();
    }

    private Map<String, Object> reservationPayload(Long resourceId, LocalDate reserveDate, String startTime, String endTime, String purpose) {
        return Map.of(
                "resourceId", resourceId,
                "reserveDate", reserveDate.toString(),
                "startTime", startTime,
                "endTime", endTime,
                "purpose", purpose
        );
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set(TOKEN_HEADER, token);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private void assertSuccess(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("code").asInt()).isEqualTo(1);
    }

    private void assertBusinessError(ResponseEntity<String> response, String message) throws Exception {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("code").asInt()).isEqualTo(0);
        assertThat(root.path("msg").asText()).isEqualTo(message);
    }

    private String uniquePurpose(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private LocalDate uniqueFutureDate() {
        long offset = Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100000);
        return LocalDate.now().plusDays(365 + offset);
    }
}
