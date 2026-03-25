CREATE TABLE users (
    id                      BINARY(16)   NOT NULL,
    nickname                VARCHAR(50),
    email                   VARCHAR(255),
    job                     VARCHAR(20),
    provider                VARCHAR(20)  NOT NULL,
    provider_id             VARCHAR(255) NOT NULL,
    onboarding_completed_at DATETIME,
    deleted_at              DATETIME,
    created_at              DATETIME     NOT NULL,
    updated_at              DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_provider_provider_id (provider, provider_id)
);

CREATE TABLE user_consents (
    user_id                 BINARY(16) NOT NULL,
    service_terms_agreed_at DATETIME   NOT NULL,
    privacy_agreed_at       DATETIME   NOT NULL,
    marketing_agreed        BOOLEAN    NOT NULL DEFAULT FALSE,
    marketing_agreed_at     DATETIME,
    marketing_revoked_at    DATETIME,
    PRIMARY KEY (user_id)
);

CREATE TABLE refresh_tokens (
    id         BINARY(16)   NOT NULL,
    user_id    BINARY(16)   NOT NULL,
    token      VARCHAR(512) NOT NULL,
    expires_at DATETIME     NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_token (token)
);