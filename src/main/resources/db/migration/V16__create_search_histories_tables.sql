CREATE TABLE search_histories (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    searched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_keyword UNIQUE (user_id, keyword),
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_user_searched_at (user_id, searched_at DESC)
);