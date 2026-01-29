-- roles/status checks
ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('USER','ADMIN'));

ALTER TABLE tracking_sessions
    ADD CONSTRAINT chk_sessions_status
        CHECK (status IN ('ACTIVE','STOPPED','ARCHIVED'));

-- 1 user = 1 ACTIVE session (partial unique index)
CREATE UNIQUE INDEX IF NOT EXISTS ux_sessions_user_active
    ON tracking_sessions(user_id)
    WHERE status = 'ACTIVE';

-- basic sanity checks for points (optional but recommended)
ALTER TABLE tracking_points
    ADD CONSTRAINT chk_points_accuracy
        CHECK (accuracy_m IS NULL OR accuracy_m >= 0);

ALTER TABLE tracking_points
    ADD CONSTRAINT chk_points_speed
        CHECK (speed_mps IS NULL OR speed_mps >= 0);

ALTER TABLE tracking_points
    ADD CONSTRAINT chk_points_heading
        CHECK (heading_deg IS NULL OR (heading_deg >= 0 AND heading_deg < 360));
