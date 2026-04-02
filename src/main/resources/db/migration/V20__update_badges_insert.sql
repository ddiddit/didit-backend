DELETE FROM user_badges;
DELETE FROM badges;

INSERT INTO badges (id, name, description, condition_type, created_at)
VALUES
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567801'), '첫 기록', '첫 회고 저장 완료', 'FIRST_RETRO', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567802'), '3일 기록', '3일 연속 회고 저장 완료', 'STREAK_3_DAYS', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567803'), '30회 기록', '누적 회고 저장 완료 30회', 'TOTAL_30', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567804'), '몰입의 시작', '심화 질문 1회 답변 완료', 'DEEP_QUESTION_1', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567805'), '몰입의 지속', '심화 질문 5회 답변 완료', 'DEEP_QUESTION_5', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567806'), '몰입의 힘', '심화 질문 10회 답변 완료', 'DEEP_QUESTION_10', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567807'), '월요일 루틴', '매주 월요일에 회고 3회 작성', 'WEEKLY_MON', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567808'), '화요일 루틴', '매주 화요일에 회고 3회 작성', 'WEEKLY_TUE', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567809'), '수요일 루틴', '매주 수요일에 회고 3회 작성', 'WEEKLY_WED', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567810'), '목요일 루틴', '매주 목요일에 회고 3회 작성', 'WEEKLY_THU', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567811'), '금요일 루틴', '매주 금요일에 회고 3회 작성', 'WEEKLY_FRI', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567812'), '토요일 루틴', '매주 토요일에 회고 3회 작성', 'WEEKLY_SAT', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567813'), '일요일 루틴', '매주 일요일에 회고 3회 작성', 'WEEKLY_SUN', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567814'), '루틴 첫 걸음', '주 3회 이상 회고 완료 첫 달성', 'WEEKLY_3_FIRST', NOW()),
    (UUID_TO_BIN('b1a2c3d4-e5f6-7890-abcd-ef1234567815'), '루틴의 힘', '3주 이상 주 3회 이상 회고 완료', 'WEEKLY_3_THREE_WEEKS', NOW());
