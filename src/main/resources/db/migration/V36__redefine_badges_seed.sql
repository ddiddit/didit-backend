-- 1.2.0 배지 10종 재정의. 기존 시드(STREAK_3_DAYS, DEEP_QUESTION_x, WEEKLY_3_FIRST 등)는 신규 conditionType과 의미가 달라 DELETE 후 재시드.
-- UserBadge 데이터는 보존(badge_id orphan은 BadgeQueryService에서 mapNotNull로 안전 처리).

DELETE FROM badges;

INSERT INTO badges (id, name, description, condition_type, category, threshold, params, icon_url, congrats_title, congrats_message, active, created_at)
VALUES
    -- 꾸준함
    (UUID_TO_BIN('ba001111-1111-1111-1111-000000000001'), '첫 기록',     '첫 회고 저장 완료',         'CUMULATIVE_RETRO',     'CONSISTENCY',  1, NULL, NULL, '첫 기록 달성!',     '첫 회고를 남겼어요',          1, NOW()),
    (UUID_TO_BIN('ba001111-1111-1111-1111-000000000002'), '10회 기록',   '누적 회고 10회 달성',       'CUMULATIVE_RETRO',     'CONSISTENCY', 10, NULL, NULL, '10회 기록 달성!',   '회고 10회를 채웠어요',        1, NOW()),
    (UUID_TO_BIN('ba001111-1111-1111-1111-000000000003'), '30회 기록',   '누적 회고 30회 달성',       'CUMULATIVE_RETRO',     'CONSISTENCY', 30, NULL, NULL, '30회 기록 달성!',   '회고 30회를 채웠어요',        1, NOW()),

    -- 프로젝트
    (UUID_TO_BIN('ba001111-2222-2222-2222-000000000001'), '컬렉터',      '프로젝트 3개 생성',         'PROJECT_COUNT',        'PROJECT',      3, NULL, NULL, '컬렉터 달성!',      '프로젝트 3개를 만들었어요',    1, NOW()),
    (UUID_TO_BIN('ba001111-2222-2222-2222-000000000002'), '피커',        '지정된 회고 5회 작성',      'PROJECT_TAGGED_RETRO', 'PROJECT',      5, NULL, NULL, '피커 달성!',        '지정 회고를 5회 작성했어요',  1, NOW()),
    (UUID_TO_BIN('ba001111-2222-2222-2222-000000000003'), '디깅',        '한 프로젝트에 회고 3회 작성','PROJECT_RETRO_IN_ONE', 'PROJECT',      3, NULL, NULL, '디깅 달성!',        '한 프로젝트에 회고 3회 완료', 1, NOW()),

    -- 패턴
    (UUID_TO_BIN('ba001111-3333-3333-3333-000000000001'), '루틴 첫걸음', '한 주에 회고 3회 작성',     'WEEKLY_RETRO_COUNT',   'PATTERN',      3, NULL, NULL, '루틴 첫걸음 달성!', '한 주에 회고 3회를 채웠어요', 1, NOW()),
    (UUID_TO_BIN('ba001111-3333-3333-3333-000000000002'), '루틴의 힘',   '3주간 매주 3회 회고',       'WEEKLY_STREAK',        'PATTERN',      3, JSON_OBJECT('weeklyMinCount', 3), NULL, '루틴의 힘 달성!',   '3주 연속 주 3회 완료',      1, NOW()),
    (UUID_TO_BIN('ba001111-3333-3333-3333-000000000003'), '루틴의 지속', '4주 연속 주 1회 이상 회고', 'WEEKLY_STREAK',        'PATTERN',      4, JSON_OBJECT('weeklyMinCount', 1), NULL, '루틴의 지속 달성!', '4주 연속 회고를 이어갔어요',  1, NOW()),

    -- 접속
    (UUID_TO_BIN('ba001111-4444-4444-4444-000000000001'), '디딧 러버',   '7일 연속 접속',             'DAILY_ACCESS_STREAK',  'ACCESS',       7, NULL, NULL, '디딧 러버 달성!',   '7일 연속 접속했어요',          1, NOW());
