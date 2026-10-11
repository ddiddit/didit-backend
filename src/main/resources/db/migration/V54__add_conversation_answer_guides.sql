ALTER TABLE retrospective_conversation_turns
    ADD COLUMN answer_guide_mode VARCHAR(20) NULL;

ALTER TABLE retrospective_conversation_turns
    ADD COLUMN answer_examples TEXT NULL;
