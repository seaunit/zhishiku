CREATE TABLE ai_processing_consent (
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE CASCADE,
    scope TEXT NOT NULL CHECK (
        scope IN ('text', 'webpage', 'document', 'screenshot', 'search')
    ),
    service TEXT NOT NULL,
    content_type TEXT NOT NULL,
    granted BOOLEAN NOT NULL,
    consent_version TEXT NOT NULL,
    granted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (owner_id, scope),
    CHECK (granted OR revoked_at IS NOT NULL)
);

CREATE INDEX ai_processing_consent_owner_idx
    ON ai_processing_consent (owner_id, updated_at DESC);
