-- points delete tez bo‘lishi uchun time index
CREATE INDEX IF NOT EXISTS ix_points_device_ts
    ON tracking_points(device_timestamp);

-- session cutoff query uchun
CREATE INDEX IF NOT EXISTS ix_sessions_stop_time
    ON tracking_sessions(stop_time);

-- points prune bo‘lganini belgilash (raw points o‘chirilgan bo‘lsa)
ALTER TABLE session_summary
    ADD COLUMN IF NOT EXISTS raw_points_pruned_at TIMESTAMPTZ;
