ALTER TABLE rich_document_source
    DROP CONSTRAINT IF EXISTS rich_document_source_kind_check;

ALTER TABLE rich_document_source
    ADD CONSTRAINT rich_document_source_kind_check
        CHECK (source_kind IN (
            'native', 'clipboard', 'text', 'word', 'google_docs', 'webpage',
            'document', 'screenshot', 'legacy_text', 'internet_result'
        )) NOT VALID;
