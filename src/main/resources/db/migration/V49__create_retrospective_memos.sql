CREATE TABLE retrospective_memos (
    id BINARY(16) NOT NULL,
    retrospective_id BINARY(16) NOT NULL,
    content TEXT NOT NULL,
    memo_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_retrospective_memo_retrospective
        FOREIGN KEY (retrospective_id) REFERENCES retrospectives(id) ON DELETE CASCADE,
    CONSTRAINT uq_retrospective_memo_date UNIQUE (retrospective_id, memo_date),
    INDEX idx_retrospective_memo_retrospective_date (retrospective_id, memo_date DESC)
);
