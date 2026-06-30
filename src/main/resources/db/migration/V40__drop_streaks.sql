-- 회고 일별 연속 스트릭(streaks)은 1.2.0에서 폐기.
-- WeeklyRetroStreak(주간 회고 연속) + DailyAccessStreak(일일 접속 연속) 두 도메인으로 대체됨 (V35).
-- 소급 없음 정책에 따라 기존 데이터 보존 불필요.

DROP TABLE IF EXISTS streaks;
