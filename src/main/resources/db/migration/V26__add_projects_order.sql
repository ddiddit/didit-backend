ALTER TABLE projects
    ADD COLUMN display_order INT NULL,
    ADD INDEX idx_project_user_order (user_id, display_order)