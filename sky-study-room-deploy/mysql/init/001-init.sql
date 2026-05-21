CREATE TABLE IF NOT EXISTS user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resource (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resource_code VARCHAR(64) NOT NULL UNIQUE,
    resource_name VARCHAR(128) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    floor VARCHAR(32),
    open_time VARCHAR(64),
    description VARCHAR(255),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    reserve_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    purpose VARCHAR(255),
    status INT NOT NULL DEFAULT 1,
    review_remark VARCHAR(255),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_reservation_resource FOREIGN KEY (resource_id) REFERENCES resource (id),
    KEY idx_reservation_conflict (resource_id, reserve_date, status, start_time, end_time)
);

INSERT INTO user (username, password, role)
VALUES
    ('student', 'e10adc3949ba59abbe56e057f20f883e', 'USER'),
    ('admin', 'e10adc3949ba59abbe56e057f20f883e', 'ADMIN')
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    role = VALUES(role);

INSERT INTO resource (resource_code, resource_name, resource_type, status, floor, open_time, description)
VALUES
    ('A-101', 'Window Focus Seat', 'PUBLIC_SEAT', 1, '1F', '08:00-22:00', 'Single seat near the window for quiet study.'),
    ('A-102', 'Silent Corner Seat', 'PUBLIC_SEAT', 1, '1F', '08:00-22:00', 'Low-traffic public seat for long reading sessions.'),
    ('B-201', 'Private Review Room', 'PRIVATE_ROOM', 1, '2F', '09:00-21:00', 'Private room suitable for solo review and interview practice.'),
    ('C-301', 'Team Discussion Room', 'MEETING_ROOM', 1, '3F', '09:00-21:00', 'Small meeting room for group discussion and presentation rehearsal.')
ON DUPLICATE KEY UPDATE
    resource_name = VALUES(resource_name),
    resource_type = VALUES(resource_type),
    status = VALUES(status),
    floor = VALUES(floor),
    open_time = VALUES(open_time),
    description = VALUES(description);
