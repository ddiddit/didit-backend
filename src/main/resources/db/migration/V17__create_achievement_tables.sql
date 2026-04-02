CREATE TABLE badges
(
    id             BINARY(16)   PRIMARY KEY,
    name           VARCHAR(50)  NOT NULL,
    description    VARCHAR(255) NOT NULL,
    condition_type VARCHAR(50)  NOT NULL,
    created_at     DATETIME     NOT NULL,
);

CREATE TABLE user_badges
(
    id          BINARY(16) PRIMARY KEY,
    user_id     BINARY(16) NOT NULL,
    badge_id    BINARY(16) NOT NULL,
    acquired_at DATETIME   NOT NULL,

    INDEX idx_user_id (user_id),
    INDEX idx_badge_id (badge_id),
    INDEX idx_acquired_at (acquired_at)
);

CREATE TABLE streaks
(
    user_id        BINARY(16) PRIMARY KEY,
    current_streak INT        NOT NULL DEFAULT 0,
    longest_streak INT        NOT NULL DEFAULT 0,
    last_retro_date DATE,
    updated_at     DATETIME   NOT NULL,
);
