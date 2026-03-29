ALTER TABLE retrospectives
    ADD COLUMN completed_at DATETIME DEFAULT NULL,
    ADD INDEX idx_completed_at (completed_at);