ALTER TABLE retrospective_analysis_items
    ADD COLUMN question_allowed BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE retrospective_conversation_turns
    ADD COLUMN action VARCHAR(30) DEFAULT NULL;

UPDATE retrospective_conversation_turns
SET action = CASE
    WHEN question_target IS NOT NULL THEN 'ASK'
    ELSE 'REFLECT'
END
WHERE status = 'COMPLETED';
