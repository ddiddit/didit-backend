-- 회고 테이블
CREATE TABLE retrospectives (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    user_job VARCHAR(20) NOT NULL,
    current_question_number INT NOT NULL DEFAULT 1,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_user_job (user_job),
    INDEX idx_is_completed (is_completed)
);

-- 채팅 메시지 테이블
CREATE TABLE chat_messages (
    id BINARY(16) PRIMARY KEY,
    retrospective_id BINARY(16) NOT NULL,
    question_number INT NOT NULL,
    content TEXT NOT NULL,
    is_answer BOOLEAN NOT NULL,
    is_deep_question BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (retrospective_id) REFERENCES retrospectives(id) ON DELETE CASCADE,
    INDEX idx_retrospective_id (retrospective_id),
    INDEX idx_question_number (question_number),
    INDEX idx_is_answer (is_answer)
);

-- 회고 요약 테이블
CREATE TABLE retrospective_summaries (
    id BINARY(16) PRIMARY KEY,
    retrospective_id BINARY(16) NOT NULL,
    summary_content TEXT NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (retrospective_id) REFERENCES retrospectives(id) ON DELETE CASCADE,
    INDEX idx_retrospective_id (retrospective_id)
);
