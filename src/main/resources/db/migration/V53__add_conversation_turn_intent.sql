ALTER TABLE retrospective_conversation_turns
    ADD COLUMN conversation_intent VARCHAR(30) NOT NULL DEFAULT 'NORMAL';
