-- 1.2.0 배지 재정의(V39) 이후 남은 구 배지 획득 기록 정리.
-- 현재 badges 테이블에 존재하지 않는 badge_id를 가진 user_badges(=구 스트릭 시절 획득분)를 삭제한다.
-- 배포 시점부터 신규 배지는 소급 없이 다음 트리거 이벤트부터 평가/부여된다.
DELETE FROM user_badges
WHERE badge_id NOT IN (SELECT id FROM badges);
