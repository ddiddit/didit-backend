CREATE TABLE admins (
    id         BINARY(16)   NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    position   VARCHAR(20)  NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admins_email (email)
);

CREATE TABLE admin_invites (
    id          BINARY(16)   NOT NULL,
    token       BINARY(16)   NOT NULL,
    email       VARCHAR(255) NOT NULL,
    position    VARCHAR(20)  NOT NULL,
    invited_by  BINARY(16)   NOT NULL,
    expired_at  DATETIME(6)  NOT NULL,
    used_at     DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_invites_token (token)
);

CREATE TABLE admin_refresh_tokens (
    id         BINARY(16)    NOT NULL,
    admin_id   BINARY(16)    NOT NULL,
    token      VARCHAR(512)  NOT NULL,
    expires_at DATETIME(6)   NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_refresh_tokens_token (token)
);
