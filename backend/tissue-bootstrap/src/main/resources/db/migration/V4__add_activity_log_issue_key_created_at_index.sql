-- Add index for per-issue MAX(created_at) aggregation behind an issue's lastActivity read
CREATE INDEX idx_activity_log_issue_key_created_at
    ON activity_log (issue_key, created_at DESC);
