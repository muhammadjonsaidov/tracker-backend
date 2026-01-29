ALTER TABLE tracking_sessions
    DROP CONSTRAINT IF EXISTS chk_sessions_status;

ALTER TABLE tracking_sessions
    ADD CONSTRAINT chk_sessions_status
        CHECK (status IN ('ACTIVE','STOPPED','ARCHIVED','EXPIRED','ABORTED'));
