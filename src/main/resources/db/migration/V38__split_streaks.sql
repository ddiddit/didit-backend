-- 기존 streaks 테이블은 페이즈 3(평가기 재작성)에서 DROP. 이 마이그레이션은 신규 두 테이블 추가만.

CREATE TABLE weekly_retro_streaks
(
    user_id            BINARY(16) PRIMARY KEY,
    current_weeks      INT      NOT NULL DEFAULT 0,
    longest_weeks      INT      NOT NULL DEFAULT 0,
    last_achieved_week DATE,
    updated_at         DATETIME NOT NULL
);

CREATE TABLE daily_access_streaks
(
    user_id          BINARY(16) PRIMARY KEY,
    current_streak   INT      NOT NULL DEFAULT 0,
    longest_streak   INT      NOT NULL DEFAULT 0,
    last_access_date DATE,
    updated_at       DATETIME NOT NULL
);
