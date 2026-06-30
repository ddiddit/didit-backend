CREATE TABLE missions
(
    id            BINARY(16)   PRIMARY KEY,
    level         INT          NOT NULL,
    mission_type  VARCHAR(30)  NOT NULL,
    target_count  INT          NOT NULL,
    title         VARCHAR(100) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    sub_text      VARCHAR(255),
    duration_days INT,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL,

    UNIQUE KEY uk_level (level),
    INDEX idx_mission_type (mission_type),
    INDEX idx_is_active (is_active)
);


CREATE TABLE user_levels
(
    user_id       BINARY(16) PRIMARY KEY,
    current_level INT        NOT NULL DEFAULT 1,
    updated_at    DATETIME   NOT NULL,

    INDEX idx_current_level (current_level)
);


CREATE TABLE user_missions
(
    id                BINARY(16)   PRIMARY KEY,
    user_id           BINARY(16)   NOT NULL,
    mission_id        BINARY(16)   NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    progress          INT          NOT NULL DEFAULT 0,
    last_retro_date   DATE,
    failure_count     INT          NOT NULL DEFAULT 0,
    level_up_popup_shown    BOOLEAN      NOT NULL DEFAULT FALSE,
    failure_popup_shown     BOOLEAN      NOT NULL DEFAULT FALSE,
    started_at        DATETIME     NOT NULL,
    completed_at      DATETIME,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NOT NULL,

    INDEX idx_user_id (user_id),
    INDEX idx_mission_id (mission_id),
    INDEX idx_status (status),
    INDEX idx_user_mission (user_id, status)
);
