ALTER TABLE owner_account
    ADD COLUMN IF NOT EXISTS username TEXT,
    ADD COLUMN IF NOT EXISTS account_role TEXT NOT NULL DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS account_status TEXT NOT NULL DEFAULT 'enabled';

ALTER TABLE owner_account
    ADD CONSTRAINT owner_account_username_format_check
    CHECK (username IS NULL OR username ~ '^[A-Za-z0-9]{1,64}$');

ALTER TABLE owner_account
    ADD CONSTRAINT owner_account_role_check
    CHECK (account_role IN ('USER', 'ADMIN'));

ALTER TABLE owner_account
    ADD CONSTRAINT owner_account_status_check
    CHECK (account_status IN ('enabled', 'disabled'));

CREATE UNIQUE INDEX owner_account_username_unique_idx
    ON owner_account (lower(username))
    WHERE username IS NOT NULL;

CREATE INDEX owner_account_status_created_idx
    ON owner_account (account_status, created_at DESC);

CREATE TABLE admin_audit_event (
    id UUID PRIMARY KEY,
    actor_owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    target_owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    action TEXT NOT NULL CHECK (action IN (
        'account_disabled', 'account_enabled', 'points_granted'
    )),
    reason_safe TEXT NOT NULL CHECK (char_length(reason_safe) BETWEEN 1 AND 300),
    previous_state JSONB NOT NULL DEFAULT '{}'::jsonb,
    next_state JSONB NOT NULL DEFAULT '{}'::jsonb,
    idempotency_key TEXT NOT NULL CHECK (char_length(idempotency_key) BETWEEN 16 AND 128),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (actor_owner_id, action, idempotency_key)
);

CREATE INDEX admin_audit_actor_created_idx
    ON admin_audit_event (actor_owner_id, created_at DESC);

CREATE INDEX admin_audit_target_created_idx
    ON admin_audit_event (target_owner_id, created_at DESC);
