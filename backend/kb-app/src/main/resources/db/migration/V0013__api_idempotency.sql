CREATE TABLE api_idempotency (
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE CASCADE,
    operation TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('in_progress', 'completed')),
    response_json JSONB,
    status_code INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (owner_id, operation, idempotency_key)
);

CREATE INDEX api_idempotency_expiry_idx ON api_idempotency (expires_at);
