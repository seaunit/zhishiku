CREATE TABLE billing_environment_guard (
    singleton_id SMALLINT PRIMARY KEY CHECK (singleton_id = 1),
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    initialized_at TIMESTAMPTZ NOT NULL
);
