ALTER TABLE retrospectives
    ADD COLUMN summary_generation_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED';

UPDATE retrospectives
SET summary_generation_status = 'GENERATED'
WHERE summary IS NOT NULL;

CREATE INDEX idx_retrospectives_user_status_deleted_created
    ON retrospectives (user_id, status, deleted_at, created_at DESC);
