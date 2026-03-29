ALTER TABLE chat_messages
    ADD COLUMN input_type VARCHAR(10) DEFAULT NULL;

CREATE INDEX idx_input_type ON chat_messages (input_type);