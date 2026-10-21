CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE owner_account (
    id UUID PRIMARY KEY,
    instance_key SMALLINT NOT NULL UNIQUE CHECK (instance_key = 1),
    email_normalized TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    locale TEXT NOT NULL DEFAULT 'zh-CN' CHECK (locale IN ('zh-CN', 'en')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);
CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK
        FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);

CREATE TABLE knowledge_item (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    title TEXT NOT NULL CHECK (char_length(title) BETWEEN 1 AND 300),
    body TEXT NOT NULL DEFAULT '',
    note TEXT NOT NULL DEFAULT '',
    processing_status TEXT NOT NULL DEFAULT 'not_requested',
    lifecycle_state TEXT NOT NULL DEFAULT 'active'
        CHECK (lifecycle_state IN ('active', 'recycled', 'purging')),
    body_migration_state TEXT NOT NULL DEFAULT 'completed'
        CHECK (body_migration_state IN ('not_required', 'pending', 'completed', 'failed')),
    search_revision BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);
CREATE INDEX knowledge_item_owner_updated_idx
    ON knowledge_item (owner_id, updated_at DESC)
    WHERE lifecycle_state = 'active';

CREATE TABLE search_document (
    item_id UUID PRIMARY KEY REFERENCES knowledge_item(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    title_text TEXT NOT NULL DEFAULT '',
    body_text TEXT NOT NULL DEFAULT '',
    note_text TEXT NOT NULL DEFAULT '',
    image_text TEXT NOT NULL DEFAULT '',
    search_text TEXT NOT NULL DEFAULT '',
    search_vector TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', search_text)) STORED,
    search_revision BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX search_document_vector_idx ON search_document USING GIN (search_vector);
CREATE INDEX search_document_trgm_idx ON search_document USING GIN (search_text gin_trgm_ops);
CREATE INDEX search_document_title_trgm_idx ON search_document USING GIN (title_text gin_trgm_ops);

CREATE TABLE attachment (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    item_id UUID REFERENCES knowledge_item(id) ON DELETE RESTRICT,
    kind TEXT NOT NULL CHECK (kind IN ('image', 'document', 'screenshot', 'derived_redacted_image')),
    storage_key TEXT NOT NULL UNIQUE,
    filename_display TEXT NOT NULL,
    media_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    sha256 CHAR(64) NOT NULL,
    scan_state TEXT NOT NULL CHECK (scan_state IN ('pending', 'clean', 'rejected')),
    redaction_state TEXT NOT NULL CHECK (redaction_state IN ('pending', 'none', 'redacted', 'not_required', 'rejected')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX attachment_owner_hash_idx ON attachment (owner_id, sha256);
CREATE INDEX attachment_item_idx ON attachment (item_id);

CREATE TABLE processing_job (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    item_id UUID REFERENCES knowledge_item(id) ON DELETE CASCADE,
    kind TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'queued'
        CHECK (status IN ('queued', 'running', 'succeeded', 'failed', 'cancelled')),
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    scheduled_at TIMESTAMPTZ NOT NULL,
    lease_owner TEXT,
    lease_expires_at TIMESTAMPTZ,
    fencing_token BIGINT NOT NULL DEFAULT 0,
    idempotency_key TEXT NOT NULL,
    error_code TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (owner_id, idempotency_key)
);
CREATE INDEX processing_job_claim_idx ON processing_job (status, scheduled_at);
