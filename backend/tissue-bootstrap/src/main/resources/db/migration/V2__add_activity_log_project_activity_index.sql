-- Add Index for per-project MAX(created_at) aggregation behind a project's lastActivity read
CREATE INDEX idx_activity_log_project_key_created_at
    ON activity_log (project_key, created_at DESC);
