ALTER TABLE badges
    ADD COLUMN category         VARCHAR(30)  NULL AFTER condition_type,
    ADD COLUMN threshold        INT          NULL AFTER category,
    ADD COLUMN params           JSON         NULL AFTER threshold,
    ADD COLUMN icon_url         VARCHAR(500) NULL AFTER params,
    ADD COLUMN congrats_title   VARCHAR(100) NULL AFTER icon_url,
    ADD COLUMN congrats_message VARCHAR(255) NULL AFTER congrats_title,
    ADD COLUMN active           TINYINT(1)   NOT NULL DEFAULT 1 AFTER congrats_message;
