CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE rich_document_migration (
    item_id UUID PRIMARY KEY REFERENCES knowledge_item(id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('pending', 'running', 'migrated', 'failed')),
    attempt_count INT NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    source_hash CHAR(64) NOT NULL,
    error_code TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX rich_document_migration_status_idx
    ON rich_document_migration (status, updated_at);

ALTER TABLE rich_image_reference
    ADD COLUMN status TEXT NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'removed', 'cleanup_pending'));
CREATE INDEX rich_image_reference_cleanup_idx
    ON rich_image_reference (item_id, status, attachment_id);
CREATE INDEX rich_image_reference_attachment_status_idx
    ON rich_image_reference (attachment_id, status);

CREATE TABLE attachment_cleanup (
    attachment_id UUID PRIMARY KEY REFERENCES attachment(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    storage_key TEXT NOT NULL,
    reason TEXT NOT NULL
        CHECK (reason IN ('reference_removed', 'item_purged', 'upload_expired')),
    not_before TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX attachment_cleanup_due_idx ON attachment_cleanup (not_before);

ALTER TABLE rich_document_source
    ADD CONSTRAINT rich_document_source_kind_check
        CHECK (source_kind IN (
            'native', 'clipboard', 'word', 'google_docs', 'webpage',
            'document', 'screenshot', 'legacy_text', 'internet_result'
        )) NOT VALID;

UPDATE rich_document_source
SET captured_at = converted_at
WHERE source_kind <> 'native' AND captured_at IS NULL;

ALTER TABLE rich_document_source
    ADD CONSTRAINT rich_document_source_provenance_check
        CHECK (
            (source_kind NOT IN ('webpage', 'internet_result') OR source_url IS NOT NULL)
            AND (source_kind = 'native' OR captured_at IS NOT NULL)
            AND (
                source_url IS NULL
                OR source_url ~ '^https?://[^[:space:]]+$'
            )
        ) NOT VALID;

INSERT INTO rich_document_migration
    (item_id, status, attempt_count, source_hash, error_code,
     started_at, finished_at, updated_at)
SELECT
    k.id,
    'pending',
    0,
    encode(digest(COALESCE(k.body, ''), 'sha256'), 'hex'),
    NULL,
    NULL,
    NULL,
    now()
FROM knowledge_item k
LEFT JOIN rich_document d ON d.item_id = k.id
WHERE d.item_id IS NULL
ON CONFLICT (item_id) DO NOTHING;

UPDATE knowledge_item
SET body_migration_state = 'pending',
    updated_at = now()
WHERE body_migration_state <> 'pending'
  AND NOT EXISTS (
      SELECT 1 FROM rich_document d WHERE d.item_id = knowledge_item.id
  );
