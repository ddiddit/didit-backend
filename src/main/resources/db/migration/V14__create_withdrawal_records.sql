CREATE TABLE withdrawal_records (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    reason VARCHAR(30) NOT NULL,
    reason_detail TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id)
);