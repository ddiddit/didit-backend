CREATE TABLE inquiries (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    email VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    type_etc VARCHAR(255) NULL,
    content TEXT NOT NULL,
    is_agreed BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_answer TEXT NULL,
    admin_id BINARY(16) NULL,
    answered_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL
    PRIMARY KEY (id)
)