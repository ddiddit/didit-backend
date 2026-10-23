CREATE INDEX idx_retrospectives_user_status_deleted_completed
    ON retrospectives (user_id, status, deleted_at, completed_at DESC, id DESC);
