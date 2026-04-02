CREATE TABLE audit_logs
(
    id          BINARY(16)  PRIMARY KEY,
    actor_id    BINARY(16)  NOT NULL,
    actor_type  VARCHAR(20) NOT NULL,
    action      VARCHAR(50) NOT NULL,
    target_id   BINARY(16),
    target_type VARCHAR(50),
    payload     JSON,
    created_at  DATETIME    NOT NULL,

    INDEX idx_actor_id (actor_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
);