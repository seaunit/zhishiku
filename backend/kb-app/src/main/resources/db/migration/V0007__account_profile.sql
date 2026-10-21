ALTER TABLE owner_account
    ADD COLUMN display_name TEXT;

UPDATE owner_account
SET display_name = CASE
    WHEN btrim(split_part(email_normalized, '@', 1)) = '' THEN 'user'
    ELSE left(btrim(split_part(email_normalized, '@', 1)), 40)
END
WHERE display_name IS NULL;

ALTER TABLE owner_account
    ALTER COLUMN display_name SET NOT NULL;

ALTER TABLE owner_account
    ADD CONSTRAINT owner_account_display_name_length_check
    CHECK (char_length(btrim(display_name)) BETWEEN 1 AND 40);
