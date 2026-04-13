CREATE TABLE retrospective_tags (
    id BINARY(16) NOT NULL,
    retrospective_id BINARY(16) NOT NULL,
    tag_id BINARY(16) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX uq_retrospective_tags_retrospective_tag (retrospective_id, tag_id),
    INDEX idx_retrospective_tags_tag_active (tag_id, deleted_at),
);