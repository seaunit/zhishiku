CREATE TABLE chat_conversation (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chat_conversation_title_check
        CHECK (length(btrim(title)) BETWEEN 1 AND 100),
    CONSTRAINT chat_conversation_owner_id_key UNIQUE (id, owner_id)
);

CREATE INDEX chat_conversation_owner_updated_idx
    ON chat_conversation (owner_id, updated_at DESC, id DESC);

CREATE TABLE chat_message (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    sources JSONB NOT NULL DEFAULT '[]'::jsonb,
    needs_internet BOOLEAN NOT NULL DEFAULT FALSE,
    reason VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chat_message_role_check CHECK (role IN ('user', 'assistant')),
    CONSTRAINT chat_message_content_check
        CHECK (length(btrim(content)) BETWEEN 1 AND 8000),
    CONSTRAINT chat_message_sources_array_check CHECK (jsonb_typeof(sources) = 'array'),
    CONSTRAINT chat_message_conversation_owner_fk
        FOREIGN KEY (conversation_id, owner_id)
        REFERENCES chat_conversation (id, owner_id)
        ON DELETE CASCADE
);

CREATE INDEX chat_message_conversation_created_idx
    ON chat_message (conversation_id, created_at, id);

CREATE INDEX chat_message_owner_idx
    ON chat_message (owner_id);
