INSERT INTO missions (id, level, mission_type, target_count, title, description, sub_text, duration_days, is_active, created_at)
VALUES (UUID_TO_BIN(UUID()), 1, 'FIRST_RETRO', 1, '첫 회고 작성하기', '첫 회고를 작성해보세요', NULL, NULL, TRUE, NOW()),
       (UUID_TO_BIN(UUID()), 2, 'TIME_LIMITED', 3, '일주일 내에 회고 3회 작성하기', '7일 이내에 회고를 3회 이상 작성해보세요', NULL, 7, TRUE, NOW()),
       (UUID_TO_BIN(UUID()), 3, 'CONSECUTIVE_WEEK', 2, '2주 연속 주 1회 이상 회고', '매주 한 번씩 회고를 작성하면 달성할 수 있어요', '꾸준한 기록이 습관을 만들어요', NULL, TRUE, NOW()),
       (UUID_TO_BIN(UUID()), 4, 'CUMULATIVE_RETRO', 3, '회고 3회 작성하기', '총 3회의 회고를 작성해보세요', '원하는 속도로 미션을 완료해보세요', NULL, TRUE, NOW()),
       (UUID_TO_BIN(UUID()), 5, 'CONSECUTIVE_WEEK', 3, '3주 연속 주 1회 이상 회고', '매주 한 번씩 회고를 작성하면 달성할 수 있어요', '꾸준한 기록이 습관을 만들어요', NULL, TRUE, NOW()),
       (UUID_TO_BIN(UUID()), 6, 'CUMULATIVE_RETRO', 5, '회고 5회 작성하기', '총 5회의 회고를 작성해보세요', '원하는 속도로 미션을 완료해보세요', NULL, TRUE, NOW()),
       (UUID_TO_BIN(UUID()), 7, 'CONSECUTIVE_WEEK', 4, '4주 연속 주 1회 이상 회고', '매주 한 번씩 회고를 작성하면 달성할 수 있어요', '꾸준한 기록이 습관을 만들어요', NULL, TRUE, NOW()),
       (UUID_TO_BIN(UUID()), 8, 'CUMULATIVE_RETRO', 7, '회고 7회 작성하기', '총 7회의 회고를 작성해보세요', '원하는 속도로 미션을 완료해보세요', NULL, TRUE, NOW()),
       (UUID_TO_BIN(UUID()), 9, 'CONSECUTIVE_WEEK', 5, '5주 연속 주 1회 이상 회고', '매주 한 번씩 회고를 작성하면 달성할 수 있어요', '꾸준한 기록이 습관을 만들어요', NULL, TRUE, NOW()),
       (UUID_TO_BIN(UUID()), 10, 'CUMULATIVE_RETRO', 10, '회고 10회 작성하기', '총 10회의 회고를 작성해보세요', '원하는 속도로 미션을 완료해보세요', NULL, TRUE, NOW());
