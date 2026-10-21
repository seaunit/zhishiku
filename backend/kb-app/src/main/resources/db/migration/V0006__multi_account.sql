ALTER TABLE owner_account
    DROP CONSTRAINT IF EXISTS owner_account_instance_key_key;

ALTER TABLE owner_account
    DROP CONSTRAINT IF EXISTS owner_account_instance_key_check;

ALTER TABLE owner_account
    DROP COLUMN IF EXISTS instance_key;

ALTER TABLE owner_account
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;

UPDATE owner_account
SET email_verified_at = created_at
WHERE email_verified_at IS NULL;

ALTER TABLE owner_account
    ALTER COLUMN email_verified_at SET NOT NULL;
