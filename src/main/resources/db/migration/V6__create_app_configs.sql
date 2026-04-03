CREATE TABLE app_configs (
    id                  BINARY(16)  NOT NULL,
    maintenance_mode    TINYINT(1)  NOT NULL DEFAULT 0,
    maintenance_message TEXT,
    minimum_version     VARCHAR(20) NOT NULL DEFAULT '0.0.0',
    PRIMARY KEY (id)
);

INSERT INTO app_configs (id, maintenance_mode, maintenance_message, minimum_version)
VALUES (UNHEX(REPLACE(UUID(), '-', '')), 0, NULL, '0.0.0');