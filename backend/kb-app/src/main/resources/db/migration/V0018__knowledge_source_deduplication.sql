ALTER TABLE rich_document_source
    ADD COLUMN canonical_url TEXT;

UPDATE rich_document_source
SET canonical_url = source_url
WHERE source_url IS NOT NULL;

CREATE INDEX rich_document_source_canonical_url_idx
    ON rich_document_source (canonical_url)
    WHERE canonical_url IS NOT NULL;

CREATE INDEX rich_document_source_url_idx
    ON rich_document_source (source_url)
    WHERE source_url IS NOT NULL;
