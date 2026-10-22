ALTER TABLE knowledge_item
    ADD COLUMN ai_summary TEXT;

CREATE TABLE knowledge_classification_term (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE CASCADE,
    kind TEXT NOT NULL CHECK (kind IN ('category', 'tag')),
    name TEXT NOT NULL CHECK (char_length(name) BETWEEN 1 AND 100),
    normalized_name TEXT NOT NULL CHECK (char_length(normalized_name) BETWEEN 1 AND 100),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'merged')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (owner_id, kind, normalized_name)
);

CREATE INDEX knowledge_classification_term_owner_idx
    ON knowledge_classification_term (owner_id, kind, normalized_name);

CREATE TABLE knowledge_classification_assignment (
    item_id UUID NOT NULL REFERENCES knowledge_item(id) ON DELETE CASCADE,
    term_id UUID NOT NULL REFERENCES knowledge_classification_term(id) ON DELETE RESTRICT,
    kind TEXT NOT NULL CHECK (kind IN ('category', 'tag')),
    assignment_status TEXT NOT NULL CHECK (
        assignment_status IN ('applied', 'provisional', 'locked')
    ),
    origin TEXT NOT NULL CHECK (origin IN ('ai', 'user')),
    confidence DOUBLE PRECISION CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
    locked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (item_id, term_id)
);

CREATE UNIQUE INDEX knowledge_classification_one_category_idx
    ON knowledge_classification_assignment (item_id)
    WHERE kind = 'category';
CREATE INDEX knowledge_classification_item_idx
    ON knowledge_classification_assignment (item_id, kind);
CREATE INDEX knowledge_classification_term_lookup_idx
    ON knowledge_classification_assignment (term_id);
