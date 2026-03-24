CREATE TABLE retrospective (
                               id                    BINARY(16)   NOT NULL,
                               user_id               BINARY(16)   NOT NULL,
                               project_id            BINARY(16)   NULL,
                               title                 VARCHAR(255) NULL,
                               status                VARCHAR(50)  NOT NULL,
                               input_tokens          INT          NOT NULL DEFAULT 0,
                               output_tokens         INT          NOT NULL DEFAULT 0,
                               done_work             TEXT         NULL,
                               blocked_point         TEXT         NULL,
                               solution_process      TEXT         NULL,
                               lesson_learned        TEXT         NULL,
                               insight               TEXT         NULL,
                               improvement_direction TEXT         NULL,
                               created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (id)
);

CREATE TABLE chat_message (
                              id                BIGINT       NOT NULL AUTO_INCREMENT,
                              retrospective_id  BINARY(16)   NOT NULL,
                              sender            VARCHAR(50)  NOT NULL,
                              content           TEXT         NOT NULL,
                              question_type     VARCHAR(50)  NOT NULL,
                              is_skipped        BOOLEAN      NOT NULL DEFAULT FALSE,
                              created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (id),
                              CONSTRAINT fk_chat_message_retrospective
                                  FOREIGN KEY (retrospective_id) REFERENCES retrospective(id)
);