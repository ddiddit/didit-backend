CREATE TABLE device_tokens (
    id          BINARY(16)   NOT NULL,
    user_id     BINARY(16)   NOT NULL,
    token       VARCHAR(255) NOT NULL,
    device_type VARCHAR(50)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_device_tokens_user_device UNIQUE (user_id, device_type)
);

CREATE TABLE notification_settings (
    user_id            BINARY(16)  NOT NULL,
    marketing_consent  BOOLEAN NOT NULL DEFAULT FALSE,
    night_push_consent BOOLEAN NOT NULL DEFAULT FALSE,
    enabled            BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_time      TIME        NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id)
);