ALTER TABLE retrospective_analysis_items
    ADD COLUMN progress_status VARCHAR(20) NULL DEFAULT 'PENDING';

UPDATE retrospective_analysis_items
SET progress_status = CASE
    WHEN question_allowed = FALSE THEN 'SKIPPED'
    WHEN status <> 'EMPTY' THEN 'ANSWERED'
    ELSE 'PENDING'
END;

ALTER TABLE retrospective_analysis_items
    MODIFY COLUMN progress_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE retrospectives
    ADD COLUMN conversation_mode VARCHAR(20) NULL DEFAULT 'COLLECTING',
    ADD COLUMN extension_round_count INT NULL DEFAULT 0,
    ADD COLUMN extension_meaningful_turn_count INT NULL DEFAULT 0,
    ADD COLUMN extension_consecutive_empty_count INT NULL DEFAULT 0;

UPDATE retrospectives
SET conversation_mode = COALESCE(conversation_mode, 'COLLECTING'),
    extension_round_count = COALESCE(extension_round_count, 0),
    extension_meaningful_turn_count = COALESCE(extension_meaningful_turn_count, 0),
    extension_consecutive_empty_count = COALESCE(extension_consecutive_empty_count, 0);

ALTER TABLE retrospectives
    MODIFY COLUMN conversation_mode VARCHAR(20) NOT NULL DEFAULT 'COLLECTING',
    MODIFY COLUMN extension_round_count INT NOT NULL DEFAULT 0,
    MODIFY COLUMN extension_meaningful_turn_count INT NOT NULL DEFAULT 0,
    MODIFY COLUMN extension_consecutive_empty_count INT NOT NULL DEFAULT 0;
