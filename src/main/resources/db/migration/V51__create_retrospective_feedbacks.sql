-- 회고 삭제 후에도 평가를 보존하므로 회고/사용자 FK 및 삭제 cascade를 두지 않는다.
CREATE TABLE retrospective_feedbacks (
    id BINARY(16) NOT NULL,
    retrospective_id BINARY(16) NOT NULL,
    rating VARCHAR(32) NOT NULL,
    comment VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_retrospective_feedback UNIQUE (retrospective_id),
    INDEX idx_retrospective_feedback_created_at (created_at)
);

CREATE TABLE retrospective_feedback_reasons (
    feedback_id BINARY(16) NOT NULL,
    reason_order INT NOT NULL,
    reason VARCHAR(32) NOT NULL,
    PRIMARY KEY (feedback_id, reason_order),
    CONSTRAINT fk_retrospective_feedback_reason
        FOREIGN KEY (feedback_id) REFERENCES retrospective_feedbacks(id) ON DELETE CASCADE
);
