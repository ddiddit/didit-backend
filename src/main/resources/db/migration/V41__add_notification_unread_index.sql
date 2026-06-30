CREATE INDEX idx_notification_histories_unread
    ON notification_histories (user_id, is_read, created_at);
