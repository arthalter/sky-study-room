ALTER TABLE reservation
    ADD INDEX idx_reservation_conflict (resource_id, reserve_date, status, start_time, end_time);
