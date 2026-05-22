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
);
