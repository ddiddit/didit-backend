ALTER TABLE users MODIFY provider_id VARCHAR(255) NULL;

-- 새로운 UNIQUE 제약 추가 (deleted_at 포함)
ALTER TABLE users ADD CONSTRAINT uq_provider_provider_id_deleted UNIQUE (provider, provider_id, deleted_at);
