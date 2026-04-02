ALTER TABLE retrospectives
    ADD COLUMN project_id BINARY(16) DEFAULT NULL AFTER user_id,
    ADD INDEX idx_project_id (project_id);