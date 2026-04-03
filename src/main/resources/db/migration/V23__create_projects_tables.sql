CREATE TABLE projects (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    name VARCHAR(15) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_project_user_id (user_id),
    INDEX idx_project_user_active (user_id, deleted_at)
);