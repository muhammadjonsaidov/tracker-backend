-- Indexes for session queries
CREATE INDEX IF NOT EXISTS idx_sessions_user_status
  ON tracking_sessions(user_id, status);

CREATE INDEX IF NOT EXISTS idx_sessions_user_active
  ON tracking_sessions(user_id, status)
  WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_points_session_timestamp
  ON tracking_points(session_id, device_timestamp DESC);

-- Indexes for audit logs
CREATE INDEX IF NOT EXISTS idx_audit_admin_created
  ON admin_audit_logs(admin_id, created_at DESC);

-- Partial index for active/stopped sessions
CREATE INDEX IF NOT EXISTS idx_sessions_active_only
  ON tracking_sessions(id)
  WHERE status IN ('ACTIVE', 'STOPPED');
