-- Drop the old constraint and add a new one with ON DELETE SET NULL
ALTER TABLE admin_audit_logs
DROP CONSTRAINT admin_audit_logs_admin_id_fkey,
ADD CONSTRAINT admin_audit_logs_admin_id_fkey FOREIGN KEY (admin_id) REFERENCES users (id) ON DELETE SET NULL;