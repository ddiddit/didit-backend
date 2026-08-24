CREATE TABLE social_identities (
    id          BINARY(16)   NOT NULL,
    user_id     BINARY(16)   NOT NULL,
    provider    VARCHAR(20)  NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_social_identity_provider_id (provider, provider_id),
    KEY idx_social_identity_user_id (user_id),
    CONSTRAINT fk_social_identity_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 기존 활성 회원은 현재 식별자를 그대로 로그인에 사용할 수 있도록 새 테이블에 이관한다.
INSERT INTO social_identities (id, user_id, provider, provider_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), id, provider, provider_id, created_at, updated_at
FROM users
WHERE deleted_at IS NULL
  AND provider_id IS NOT NULL;

CREATE TABLE social_login_sessions (
    id             BINARY(16)   NOT NULL,
    token_hash     VARCHAR(64)  NOT NULL,
    provider       VARCHAR(20)  NOT NULL,
    provider_id    VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    expires_at     DATETIME     NOT NULL,
    consumed_at    DATETIME,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_social_login_session_token_hash (token_hash),
    KEY idx_social_login_session_expires_at (expires_at)
);

CREATE TABLE email_verification_challenges (
    id            BINARY(16)   NOT NULL,
    session_id    BINARY(16)   NOT NULL,
    email         VARCHAR(255) NOT NULL,
    otp_hash      VARCHAR(255) NOT NULL,
    attempt_count INT          NOT NULL DEFAULT 0,
    expires_at    DATETIME     NOT NULL,
    verified_at   DATETIME,
    invalidated_at DATETIME,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_email_challenge_session_created (session_id, created_at),
    KEY idx_email_challenge_email_created (email, created_at),
    KEY idx_email_challenge_expires_at (expires_at),
    CONSTRAINT fk_email_challenge_session
        FOREIGN KEY (session_id) REFERENCES social_login_sessions (id) ON DELETE CASCADE
);
