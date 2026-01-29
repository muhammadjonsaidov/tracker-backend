-- Enable UUID + PostGIS
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS postgis;

-- users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- tracking_sessions
CREATE TABLE IF NOT EXISTS tracking_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    start_time TIMESTAMPTZ NOT NULL,
    stop_time TIMESTAMPTZ NULL,
    status VARCHAR(20) NOT NULL,

    last_point_at TIMESTAMPTZ NULL,
    start_point geography(Point, 4326) NULL,
    stop_point geography(Point, 4326) NULL,
    last_point geography(Point, 4326) NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tracking_sessions_user_start
    ON tracking_sessions(user_id, start_time DESC);

CREATE INDEX IF NOT EXISTS idx_tracking_sessions_status
    ON tracking_sessions(status);

-- tracking_points (append-only)
CREATE TABLE IF NOT EXISTS tracking_points (
    id BIGSERIAL PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES tracking_sessions(id) ON DELETE CASCADE,

    event_id UUID NOT NULL DEFAULT gen_random_uuid(),
    device_timestamp TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    point geography(Point, 4326) NOT NULL,
    accuracy_m REAL NULL,
    speed_mps REAL NULL,
    heading_deg REAL NULL,
    provider VARCHAR(16) NULL,
    is_mock BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_points_session_event UNIQUE (session_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_points_session_device_ts
    ON tracking_points(session_id, device_timestamp);

CREATE INDEX IF NOT EXISTS idx_points_received_at
    ON tracking_points(received_at);

CREATE INDEX IF NOT EXISTS gist_points_point
    ON tracking_points USING GIST (point);

-- session_summary (precomputed for fast history view)
CREATE TABLE IF NOT EXISTS session_summary (
    session_id UUID PRIMARY KEY REFERENCES tracking_sessions(id) ON DELETE CASCADE,

    polyline TEXT NULL,
    simplified_polyline TEXT NULL,

    points_count INTEGER NULL,
    distance_m DOUBLE PRECISION NULL,
    duration_s INTEGER NULL,
    avg_speed_mps DOUBLE PRECISION NULL,
    max_speed_mps DOUBLE PRECISION NULL,

    start_point geography(Point, 4326) NULL,
    end_point geography(Point, 4326) NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- admin audit logs
CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NULL,
    target_id UUID NULL,
    metadata JSONB NULL,

    ip_address INET NULL,
    user_agent TEXT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_admin_ts
    ON admin_audit_logs(admin_id, created_at DESC);

-- updated_at trigger helper
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_tracking_sessions_updated_at ON tracking_sessions;
CREATE TRIGGER trg_tracking_sessions_updated_at
BEFORE UPDATE ON tracking_sessions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_session_summary_updated_at ON session_summary;
CREATE TRIGGER trg_session_summary_updated_at
BEFORE UPDATE ON session_summary
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
