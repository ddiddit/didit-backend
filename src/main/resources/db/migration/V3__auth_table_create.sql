CREATE TABLE users (
    id BINARY(16) NOT NULL,
    provider VARCHAR(50),
    social_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    nickname VARCHAR(255),
    job VARCHAR(50),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_social_id UNIQUE (social_id)
);

CREATE TABLE refresh_token (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_user_id UNIQUE (user_id)
);