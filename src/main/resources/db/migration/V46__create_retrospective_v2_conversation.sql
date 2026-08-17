ALTER TABLE retrospectives
    ADD COLUMN flow_version VARCHAR(20) NOT NULL DEFAULT 'V1',
    ADD COLUMN conversation_status VARCHAR(20) DEFAULT NULL,
    ADD COLUMN conversation_finished_at DATETIME DEFAULT NULL;

ALTER TABLE chat_messages
    ADD COLUMN message_type VARCHAR(20) NOT NULL DEFAULT 'CONVERSATION',
    ADD COLUMN supporting_content TEXT DEFAULT NULL,
    ADD COLUMN relevance VARCHAR(20) DEFAULT NULL,
    ADD COLUMN included_in_result BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE retrospective_analysis_items (
    id BINARY(16) NOT NULL,
    retrospective_id BINARY(16) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'EMPTY',
    summary TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_analysis_item_retrospective
        FOREIGN KEY (retrospective_id) REFERENCES retrospectives(id) ON DELETE CASCADE,
    CONSTRAINT uq_analysis_item_retrospective_type UNIQUE (retrospective_id, item_type),
    INDEX idx_analysis_item_retrospective (retrospective_id)
);

CREATE TABLE retrospective_analysis_evidences (
    id BINARY(16) NOT NULL,
    analysis_item_id BINARY(16) NOT NULL,
    message_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_analysis_evidence_item
        FOREIGN KEY (analysis_item_id) REFERENCES retrospective_analysis_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_analysis_evidence_message
        FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT uq_analysis_evidence_item_message UNIQUE (analysis_item_id, message_id),
    INDEX idx_analysis_evidence_item (analysis_item_id),
    INDEX idx_analysis_evidence_message (message_id)
);

CREATE TABLE retrospective_conversation_turns (
    id BINARY(16) NOT NULL,
    retrospective_id BINARY(16) NOT NULL,
    client_message_id BINARY(16) NOT NULL,
    user_message_id BINARY(16) NOT NULL,
    assistant_message_id BINARY(16) DEFAULT NULL,
    question_target VARCHAR(20) DEFAULT NULL,
    turn_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 1,
    error_code VARCHAR(50) DEFAULT NULL,
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    completed_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_conversation_turn_retrospective
        FOREIGN KEY (retrospective_id) REFERENCES retrospectives(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_turn_user_message
        FOREIGN KEY (user_message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_turn_assistant_message
        FOREIGN KEY (assistant_message_id) REFERENCES chat_messages(id) ON DELETE SET NULL,
    CONSTRAINT uq_conversation_turn_client_message UNIQUE (retrospective_id, client_message_id),
    CONSTRAINT uq_conversation_turn_number UNIQUE (retrospective_id, turn_number),
    INDEX idx_conversation_turn_status (retrospective_id, status)
);
