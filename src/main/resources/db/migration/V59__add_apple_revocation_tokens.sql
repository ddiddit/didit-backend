ALTER TABLE social_identities
    ADD COLUMN apple_refresh_token TEXT NULL,
    ADD COLUMN apple_token_client_id VARCHAR(255) NULL;

ALTER TABLE social_login_sessions
    ADD COLUMN apple_refresh_token TEXT NULL,
    ADD COLUMN apple_token_client_id VARCHAR(255) NULL;
