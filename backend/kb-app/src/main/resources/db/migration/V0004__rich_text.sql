CREATE TABLE rich_document (
    item_id UUID PRIMARY KEY REFERENCES knowledge_item(id) ON DELETE CASCADE,
    schema_version INT NOT NULL CHECK (schema_version >= 1),
    document JSONB NOT NULL,
    plain_text TEXT NOT NULL CHECK (char_length(plain_text) <= 200000),
    document_hash CHAR(64) NOT NULL,
    image_count INT NOT NULL DEFAULT 0 CHECK (image_count BETWEEN 0 AND 20),
    total_image_bytes BIGINT NOT NULL DEFAULT 0 CHECK (total_image_bytes BETWEEN 0 AND 52428800),
    sanitized_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE rich_document_revision (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES knowledge_item(id) ON DELETE CASCADE,
    revision_no INT NOT NULL,
    schema_version INT NOT NULL,
    document JSONB NOT NULL,
    plain_text TEXT NOT NULL,
    document_hash CHAR(64) NOT NULL,
    reason TEXT NOT NULL CHECK (reason IN ('create', 'edit', 'migration', 'conflict_overwrite')),
    source_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (item_id, revision_no)
);
CREATE INDEX rich_document_revision_item_idx
    ON rich_document_revision (item_id, revision_no DESC);

CREATE TABLE rich_document_source (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES knowledge_item(id) ON DELETE CASCADE,
    source_kind TEXT NOT NULL,
    source_url TEXT,
    source_host TEXT,
    captured_at TIMESTAMPTZ,
    converted_at TIMESTAMPTZ NOT NULL,
    converter_version TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX rich_document_source_item_idx ON rich_document_source (item_id, created_at DESC);

CREATE TABLE rich_document_conversion_report (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL UNIQUE REFERENCES rich_document_source(id) ON DELETE CASCADE,
    preserved_features TEXT[] NOT NULL DEFAULT '{}',
    downgraded_features TEXT[] NOT NULL DEFAULT '{}',
    removed_features TEXT[] NOT NULL DEFAULT '{}',
    rejected_features TEXT[] NOT NULL DEFAULT '{}',
    converter_version TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE rich_image_upload (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    item_id UUID REFERENCES knowledge_item(id) ON DELETE SET NULL,
    storage_key TEXT NOT NULL UNIQUE,
    filename_display TEXT NOT NULL,
    media_type TEXT NOT NULL,
    source_size_bytes BIGINT NOT NULL CHECK (source_size_bytes BETWEEN 0 AND 26214400),
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    content_hash CHAR(64) NOT NULL,
    normalized_width INT NOT NULL,
    normalized_height INT NOT NULL,
    aspect_ratio DOUBLE PRECISION NOT NULL CHECK (aspect_ratio BETWEEN 0.01 AND 100),
    scan_state TEXT NOT NULL CHECK (scan_state IN ('pending', 'clean', 'rejected')),
    redaction_state TEXT NOT NULL CHECK (redaction_state IN ('pending', 'none', 'redacted', 'rejected')),
    status TEXT NOT NULL CHECK (status IN ('staged', 'scanning', 'ready', 'rejected', 'attached', 'expired')),
    error_code TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX rich_image_upload_owner_idx ON rich_image_upload (owner_id, status, expires_at);
CREATE INDEX rich_image_upload_hash_idx ON rich_image_upload (owner_id, content_hash, status);

CREATE TABLE rich_image_reference (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES knowledge_item(id) ON DELETE CASCADE,
    revision_id UUID NOT NULL REFERENCES rich_document_revision(id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL REFERENCES attachment(id) ON DELETE RESTRICT,
    node_id TEXT NOT NULL,
    alt_text TEXT,
    caption TEXT,
    display_width_px INT NOT NULL CHECK (display_width_px BETWEEN 80 AND 1600),
    alignment TEXT NOT NULL CHECK (alignment IN ('left', 'center', 'right')),
    decorative BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (revision_id, node_id)
);
CREATE INDEX rich_image_reference_item_idx ON rich_image_reference (item_id);
CREATE INDEX rich_image_reference_attachment_idx ON rich_image_reference (attachment_id);
