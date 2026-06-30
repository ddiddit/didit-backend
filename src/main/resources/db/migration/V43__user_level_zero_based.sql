-- 레벨을 0부터 시작하도록 변경: currentLevel = 완료한 미션 수(0~10).
-- 기존 1-base(시작=1) 데이터를 0-base로 한 칸 내리고, 기본값을 0으로 변경한다.
ALTER TABLE user_levels MODIFY current_level INT NOT NULL DEFAULT 0;

UPDATE user_levels SET current_level = current_level - 1 WHERE current_level > 0;
