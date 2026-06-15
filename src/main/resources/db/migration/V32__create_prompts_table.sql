CREATE TABLE prompts (
    id          BINARY(16)   NOT NULL,
    job_type    VARCHAR(20)  NOT NULL,
    prompt_type VARCHAR(20)  NOT NULL,
    content     LONGTEXT     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    updated_by  VARCHAR(100),
    PRIMARY KEY (id),
    UNIQUE KEY uq_prompts_job_type (job_type, prompt_type)
);
