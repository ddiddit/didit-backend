ALTER TABLE retrospectives
    ADD COLUMN insight_title TEXT NULL,
    ADD COLUMN insight_description TEXT NULL,
    ADD COLUMN next_action_title TEXT NULL,
    ADD COLUMN next_action_description TEXT NULL;

UPDATE retrospectives
SET insight_title = ''
WHERE insight_title IS NULL;

UPDATE retrospectives
SET insight_description = insight
WHERE insight IS NOT NULL
  AND insight_description IS NULL;

UPDATE retrospectives
SET insight_description = ''
WHERE insight_description IS NULL;

UPDATE retrospectives
SET next_action_title = ''
WHERE next_action_title IS NULL;

UPDATE retrospectives
SET next_action_description = next_action
WHERE next_action IS NOT NULL
  AND next_action_description IS NULL;

UPDATE retrospectives
SET next_action_description = ''
WHERE next_action_description IS NULL;