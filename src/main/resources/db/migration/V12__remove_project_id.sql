ALTER TABLE retrospectives
    DROP COLUMN project_id,
    DROP INDEX idx_project_id;