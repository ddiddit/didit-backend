-- retrospective_summaries 테이블 제거
DROP TABLE IF EXISTS retrospective_summaries;

-- chat_messages FK, Index 제거 후 재구성
ALTER TABLE chat_messages
DROP FOREIGN KEY chat_messages_ibfk_1,
DROP INDEX idx_retrospective_id,
DROP INDEX idx_question_number,
DROP INDEX idx_is_answer,
DROP COLUMN question_number,
    DROP COLUMN is_answer,
    DROP COLUMN is_deep_question,
    DROP COLUMN message_created_at,
    DROP COLUMN updated_at,
    ADD COLUMN sender VARCHAR(10) NOT NULL,
    ADD COLUMN question_type VARCHAR(10) NOT NULL,
    ADD COLUMN is_skipped BOOLEAN NOT NULL DEFAULT FALSE,
    ADD INDEX idx_retrospective_id (retrospective_id),
    ADD INDEX idx_question_type (question_type),
    ADD INDEX idx_sender (sender);

-- retrospectives Index 제거 후 재구성
ALTER TABLE retrospectives
DROP INDEX idx_user_job,
DROP INDEX idx_is_completed,
DROP COLUMN user_job,
    DROP COLUMN current_question_number,
    DROP COLUMN is_completed,
    DROP COLUMN completed_at,
    ADD COLUMN project_id BINARY(16) DEFAULT NULL AFTER user_id,
    ADD COLUMN title VARCHAR(255) DEFAULT NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    ADD COLUMN input_tokens INT NOT NULL DEFAULT 0,
    ADD COLUMN output_tokens INT NOT NULL DEFAULT 0,
    ADD COLUMN feedback TEXT DEFAULT NULL,
    ADD COLUMN insight TEXT DEFAULT NULL,
    ADD COLUMN done_work TEXT DEFAULT NULL,
    ADD COLUMN blocked_point TEXT DEFAULT NULL,
    ADD COLUMN solution_process TEXT DEFAULT NULL,
    ADD COLUMN lesson_learned TEXT DEFAULT NULL,
    ADD COLUMN deleted_at DATETIME DEFAULT NULL,
    ADD INDEX idx_status (status),
    ADD INDEX idx_project_id (project_id);