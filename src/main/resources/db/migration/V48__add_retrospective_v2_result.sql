ALTER TABLE retrospectives
    ADD COLUMN result_schema_version INT DEFAULT NULL,
    ADD COLUMN result_generation_started_at DATETIME(6) DEFAULT NULL,
    ADD COLUMN result_generation_attempt_id BINARY(16) DEFAULT NULL,
    ADD COLUMN result_summary TEXT DEFAULT NULL,
    ADD COLUMN result_strength TEXT DEFAULT NULL,
    ADD COLUMN result_improvement TEXT DEFAULT NULL,
    ADD COLUMN result_process TEXT DEFAULT NULL,
    ADD COLUMN result_learning TEXT DEFAULT NULL,
    ADD COLUMN result_insight TEXT DEFAULT NULL,
    ADD COLUMN result_next_actions TEXT DEFAULT NULL;
