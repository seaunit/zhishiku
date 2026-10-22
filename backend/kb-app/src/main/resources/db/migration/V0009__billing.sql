CREATE TABLE billing_catalog_version (
    id UUID PRIMARY KEY,
    version_code TEXT NOT NULL,
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    status TEXT NOT NULL CHECK (status IN ('draft', 'scheduled', 'active', 'retired')),
    effective_at TIMESTAMPTZ NOT NULL,
    retired_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (environment, version_code)
);
CREATE UNIQUE INDEX billing_catalog_one_active_idx
    ON billing_catalog_version (environment)
    WHERE status = 'active';

CREATE TABLE billing_point_pack (
    id UUID PRIMARY KEY,
    catalog_version_id UUID NOT NULL REFERENCES billing_catalog_version(id) ON DELETE RESTRICT,
    sku TEXT NOT NULL CHECK (sku IN ('kb_pack_9', 'kb_pack_29', 'kb_pack_99')),
    name_zh TEXT NOT NULL,
    name_en TEXT NOT NULL,
    price_minor INTEGER NOT NULL CHECK (price_minor > 0),
    currency CHAR(3) NOT NULL CHECK (currency = 'CNY'),
    points INTEGER NOT NULL CHECK (points > 0),
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    status TEXT NOT NULL CHECK (status IN ('enabled', 'disabled')),
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    UNIQUE (catalog_version_id, sku)
);

CREATE TABLE billing_ai_rate (
    id UUID PRIMARY KEY,
    catalog_version_id UUID NOT NULL REFERENCES billing_catalog_version(id) ON DELETE RESTRICT,
    feature TEXT NOT NULL CHECK (feature IN (
        'classify', 'ai_text', 'webpage_vision', 'document_extract', 'screenshot_ocr',
        'internet_search', 'ai_answer'
    )),
    unit TEXT NOT NULL CHECK (unit IN ('entry', 'view', 'per_10k_chars', 'image', 'query', 'answer')),
    points_per_unit INTEGER NOT NULL CHECK (points_per_unit > 0),
    success_rule_code TEXT NOT NULL,
    description_zh TEXT NOT NULL,
    description_en TEXT NOT NULL,
    UNIQUE (catalog_version_id, feature)
);

CREATE TABLE billing_payment_order (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    pack_id UUID NOT NULL REFERENCES billing_point_pack(id) ON DELETE RESTRICT,
    sku_snapshot TEXT NOT NULL,
    catalog_version_id UUID NOT NULL REFERENCES billing_catalog_version(id) ON DELETE RESTRICT,
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    amount_minor INTEGER NOT NULL CHECK (amount_minor > 0),
    currency CHAR(3) NOT NULL CHECK (currency = 'CNY'),
    points_snapshot INTEGER NOT NULL CHECK (points_snapshot > 0),
    provider_product_id TEXT,
    status TEXT NOT NULL CHECK (status IN (
        'PENDING', 'CONFIRMING', 'PAID', 'CLOSED', 'REFUNDING', 'REFUNDED', 'EXCEPTION'
    )),
    checkout_url TEXT,
    provider_order_id TEXT,
    provider_session_id TEXT,
    paid_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    next_check_at TIMESTAMPTZ,
    reconciliation_attempts INTEGER NOT NULL DEFAULT 0 CHECK (reconciliation_attempts >= 0),
    last_reconciliation_result TEXT,
    idempotency_key TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (owner_id, idempotency_key),
    CHECK (status <> 'PAID' OR paid_at IS NOT NULL),
    CHECK (status <> 'CLOSED' OR closed_at IS NOT NULL)
);
CREATE UNIQUE INDEX billing_order_one_pending_sku_idx
    ON billing_payment_order (owner_id, environment, sku_snapshot)
    WHERE status = 'PENDING';
CREATE UNIQUE INDEX billing_order_provider_session_idx
    ON billing_payment_order (environment, provider_session_id)
    WHERE provider_session_id IS NOT NULL;
CREATE INDEX billing_order_owner_idx ON billing_payment_order (owner_id, created_at DESC);
CREATE INDEX billing_order_reconcile_idx ON billing_payment_order (status, next_check_at);

CREATE TABLE billing_checkout_session (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES billing_payment_order(id) ON DELETE RESTRICT,
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    provider_session_id TEXT NOT NULL,
    checkout_url TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('open', 'expired', 'used', 'cancelled')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (environment, provider_session_id)
);
CREATE UNIQUE INDEX billing_checkout_one_open_idx
    ON billing_checkout_session (order_id)
    WHERE status = 'open';

CREATE TABLE billing_payment_event (
    id UUID PRIMARY KEY,
    order_id UUID REFERENCES billing_payment_order(id) ON DELETE RESTRICT,
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    source TEXT NOT NULL CHECK (source IN ('webhook', 'query')),
    source_event_key TEXT NOT NULL,
    provider_event_id TEXT,
    delivery_id TEXT,
    event_type TEXT NOT NULL,
    signature_status TEXT NOT NULL CHECK (signature_status IN ('valid', 'invalid', 'not_applicable')),
    normalized_status TEXT NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    processing_status TEXT NOT NULL CHECK (processing_status IN (
        'received', 'applied', 'ignored_duplicate', 'manual_review'
    )),
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    UNIQUE (environment, source_event_key)
);
CREATE INDEX billing_event_order_idx ON billing_payment_event (order_id, received_at DESC);

CREATE TABLE billing_credit_account (
    owner_id UUID PRIMARY KEY REFERENCES owner_account(id) ON DELETE RESTRICT,
    available_points INTEGER NOT NULL DEFAULT 0 CHECK (available_points >= 0),
    reserved_points INTEGER NOT NULL DEFAULT 0 CHECK (reserved_points >= 0),
    trial_points INTEGER NOT NULL DEFAULT 0 CHECK (trial_points >= 0),
    purchased_points INTEGER NOT NULL DEFAULT 0 CHECK (purchased_points >= 0),
    lifetime_granted INTEGER NOT NULL DEFAULT 0 CHECK (lifetime_granted >= 0),
    lifetime_settled INTEGER NOT NULL DEFAULT 0 CHECK (lifetime_settled >= 0),
    version INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE billing_credit_batch (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    source_type TEXT NOT NULL CHECK (source_type IN ('trial', 'purchase', 'manual_adjustment')),
    order_id UUID REFERENCES billing_payment_order(id) ON DELETE RESTRICT,
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    initial_points INTEGER NOT NULL CHECK (initial_points > 0),
    remaining_points INTEGER NOT NULL CHECK (remaining_points >= 0),
    reserved_points INTEGER NOT NULL DEFAULT 0 CHECK (reserved_points >= 0),
    status TEXT NOT NULL CHECK (status IN ('active', 'frozen', 'depleted', 'closed')),
    priority_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (remaining_points >= reserved_points),
    CHECK (source_type <> 'purchase' OR order_id IS NOT NULL)
);
CREATE UNIQUE INDEX billing_batch_purchase_order_idx
    ON billing_credit_batch (order_id)
    WHERE source_type = 'purchase';
CREATE INDEX billing_batch_consume_idx
    ON billing_credit_batch (owner_id, status, priority_at ASC);

CREATE TABLE billing_ledger_entry (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    batch_id UUID REFERENCES billing_credit_batch(id) ON DELETE RESTRICT,
    entry_scope TEXT NOT NULL CHECK (entry_scope IN ('account', 'batch')),
    operation_type TEXT NOT NULL CHECK (operation_type IN (
        'trial_grant', 'purchase_grant', 'reserve', 'reserve_release',
        'settle', 'refund_recover', 'manual_adjustment'
    )),
    available_delta INTEGER NOT NULL,
    reserved_delta INTEGER NOT NULL,
    consumed_delta INTEGER NOT NULL,
    available_after INTEGER NOT NULL CHECK (available_after >= 0),
    reserved_after INTEGER NOT NULL CHECK (reserved_after >= 0),
    reference_type TEXT NOT NULL,
    reference_id TEXT NOT NULL,
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    metadata_safe JSONB NOT NULL DEFAULT '{}'::jsonb,
    description_safe TEXT,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX billing_ledger_batch_reference_idx
    ON billing_ledger_entry (operation_type, reference_type, reference_id, batch_id)
    WHERE batch_id IS NOT NULL;
CREATE UNIQUE INDEX billing_ledger_account_reference_idx
    ON billing_ledger_entry (operation_type, reference_type, reference_id)
    WHERE batch_id IS NULL;
CREATE INDEX billing_ledger_owner_idx ON billing_ledger_entry (owner_id, created_at DESC);

CREATE TABLE billing_preflight (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    kind TEXT NOT NULL CHECK (kind IN ('document', 'screenshot', 'webpage')),
    media_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    measured_characters_upper_bound INTEGER NOT NULL CHECK (measured_characters_upper_bound >= 0),
    view_count INTEGER NOT NULL CHECK (view_count BETWEEN 1 AND 10),
    pricing_basis TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE billing_task (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    job_id UUID,
    item_id UUID REFERENCES knowledge_item(id) ON DELETE SET NULL,
    catalog_version_id UUID NOT NULL REFERENCES billing_catalog_version(id) ON DELETE RESTRICT,
    operation TEXT NOT NULL,
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    status TEXT NOT NULL CHECK (status IN (
        'QUOTED', 'CONFIRMED', 'RESERVED', 'RUNNING', 'SETTLED', 'RELEASED',
        'EXPIRED', 'INSUFFICIENT', 'CANCELLED', 'MANUAL_REVIEW'
    )),
    max_points INTEGER NOT NULL CHECK (max_points > 0),
    settled_points INTEGER NOT NULL DEFAULT 0 CHECK (settled_points >= 0),
    auto_authorization_id UUID,
    auto_charge_applied BOOLEAN NOT NULL DEFAULT FALSE,
    idempotency_key TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (owner_id, idempotency_key),
    CHECK (settled_points <= max_points)
);
CREATE INDEX billing_task_owner_idx ON billing_task (owner_id, created_at DESC);
CREATE INDEX billing_task_expiry_idx ON billing_task (status, expires_at);

CREATE TABLE billing_quote_line (
    id UUID PRIMARY KEY,
    billing_task_id UUID NOT NULL REFERENCES billing_task(id) ON DELETE CASCADE,
    feature TEXT NOT NULL,
    unit TEXT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_points INTEGER NOT NULL CHECK (unit_points > 0),
    max_line_points INTEGER NOT NULL CHECK (max_line_points > 0),
    success_rule_code TEXT NOT NULL,
    success_state TEXT NOT NULL DEFAULT 'pending'
        CHECK (success_state IN ('pending', 'succeeded', 'failed', 'skipped')),
    settled_points INTEGER NOT NULL DEFAULT 0 CHECK (settled_points >= 0),
    UNIQUE (billing_task_id, feature),
    CHECK (settled_points <= max_line_points)
);

CREATE TABLE billing_quote_image_resource (
    id UUID PRIMARY KEY,
    billing_task_id UUID NOT NULL REFERENCES billing_task(id) ON DELETE CASCADE,
    asset_id UUID NOT NULL,
    content_hash CHAR(64) NOT NULL,
    media_type TEXT NOT NULL,
    normalized_size_bytes BIGINT NOT NULL CHECK (normalized_size_bytes >= 0),
    counted_quantity INTEGER NOT NULL DEFAULT 1 CHECK (counted_quantity = 1),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (billing_task_id, content_hash)
);

CREATE TABLE billing_reservation (
    id UUID PRIMARY KEY,
    billing_task_id UUID NOT NULL UNIQUE REFERENCES billing_task(id) ON DELETE RESTRICT,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    status TEXT NOT NULL CHECK (status IN (
        'reserved', 'settling', 'settled', 'releasing', 'released', 'expired', 'manual_review'
    )),
    reserved_points INTEGER NOT NULL CHECK (reserved_points > 0),
    settled_points INTEGER NOT NULL DEFAULT 0 CHECK (settled_points >= 0),
    released_points INTEGER NOT NULL DEFAULT 0 CHECK (released_points >= 0),
    deadline_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (settled_points + released_points <= reserved_points)
);

CREATE TABLE billing_reservation_allocation (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES billing_reservation(id) ON DELETE RESTRICT,
    batch_id UUID NOT NULL REFERENCES billing_credit_batch(id) ON DELETE RESTRICT,
    status TEXT NOT NULL CHECK (status IN ('active', 'refund_hold', 'settled', 'released')),
    reserved_points INTEGER NOT NULL CHECK (reserved_points > 0),
    settled_points INTEGER NOT NULL DEFAULT 0 CHECK (settled_points >= 0),
    released_points INTEGER NOT NULL DEFAULT 0 CHECK (released_points >= 0),
    UNIQUE (reservation_id, batch_id),
    CHECK (settled_points + released_points <= reserved_points)
);

CREATE TABLE billing_auto_charge_policy (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    feature_scope TEXT NOT NULL DEFAULT 'classify' CHECK (feature_scope = 'classify'),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    max_points_per_task INTEGER NOT NULL DEFAULT 15 CHECK (max_points_per_task BETWEEN 1 AND 100),
    max_points_per_day INTEGER NOT NULL DEFAULT 100 CHECK (max_points_per_day BETWEEN 1 AND 500),
    timezone TEXT NOT NULL DEFAULT 'Asia/Shanghai',
    consent_text_version TEXT NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX billing_auto_policy_active_idx
    ON billing_auto_charge_policy (owner_id, feature_scope)
    WHERE revoked_at IS NULL;

CREATE TABLE billing_daily_auto_usage (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    usage_date DATE NOT NULL,
    auto_reserved_points INTEGER NOT NULL DEFAULT 0 CHECK (auto_reserved_points >= 0),
    auto_settled_points INTEGER NOT NULL DEFAULT 0 CHECK (auto_settled_points >= 0),
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (owner_id, usage_date)
);

CREATE TABLE billing_refund_case (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES billing_payment_order(id) ON DELETE RESTRICT,
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    status TEXT NOT NULL CHECK (status IN (
        'noticed', 'verifying', 'waiting_task_settlement', 'recovering',
        'recovered', 'partially_recovered', 'rejected', 'manual_review'
    )),
    provider_refund_id TEXT NOT NULL,
    refund_amount_minor INTEGER NOT NULL CHECK (refund_amount_minor >= 0),
    expected_recovery_points INTEGER NOT NULL CHECK (expected_recovery_points >= 0),
    recovered_points INTEGER NOT NULL DEFAULT 0 CHECK (recovered_points >= 0),
    unrecovered_points INTEGER NOT NULL DEFAULT 0 CHECK (unrecovered_points >= 0),
    reason_code TEXT,
    review_owner TEXT,
    review_due_at TIMESTAMPTZ NOT NULL,
    resolution_note_safe TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    UNIQUE (environment, provider_refund_id),
    CHECK (recovered_points <= expected_recovery_points)
);

CREATE TABLE billing_refund_recovery (
    id UUID PRIMARY KEY,
    refund_case_id UUID NOT NULL REFERENCES billing_refund_case(id) ON DELETE RESTRICT,
    batch_id UUID NOT NULL REFERENCES billing_credit_batch(id) ON DELETE RESTRICT,
    points INTEGER NOT NULL CHECK (points > 0),
    status TEXT NOT NULL CHECK (status IN ('reserved', 'recovered', 'failed')),
    created_at TIMESTAMPTZ NOT NULL,
    recovered_at TIMESTAMPTZ,
    UNIQUE (refund_case_id, batch_id)
);

CREATE TABLE billing_reconciliation_attempt (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES billing_payment_order(id) ON DELETE RESTRICT,
    environment TEXT NOT NULL CHECK (environment IN ('test', 'production')),
    attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
    result TEXT NOT NULL CHECK (result IN ('pending', 'paid', 'closed', 'refunded', 'mismatch', 'unavailable')),
    error_code TEXT,
    next_check_at TIMESTAMPTZ,
    checked_at TIMESTAMPTZ NOT NULL,
    UNIQUE (order_id, attempt_number)
);

CREATE TABLE billing_exception (
    id UUID PRIMARY KEY,
    owner_id UUID REFERENCES owner_account(id) ON DELETE RESTRICT,
    order_id UUID REFERENCES billing_payment_order(id) ON DELETE RESTRICT,
    billing_task_id UUID REFERENCES billing_task(id) ON DELETE RESTRICT,
    refund_case_id UUID REFERENCES billing_refund_case(id) ON DELETE RESTRICT,
    category TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('open', 'in_review', 'resolved', 'rejected')),
    safe_details JSONB NOT NULL DEFAULT '{}'::jsonb,
    assigned_to TEXT,
    due_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ
);

CREATE TABLE billing_idempotency (
    owner_id UUID NOT NULL REFERENCES owner_account(id) ON DELETE RESTRICT,
    idempotency_key TEXT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_json JSONB NOT NULL,
    status_code INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (owner_id, idempotency_key)
);

INSERT INTO billing_catalog_version
    (id, version_code, environment, status, effective_at, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000201', '2026-10-mvp', 'test', 'active', '2026-10-01T00:00:00Z', '2026-10-01T00:00:00Z'),
    ('00000000-0000-0000-0000-000000000202', '2026-10-mvp', 'production', 'active', '2026-10-01T00:00:00Z', '2026-10-01T00:00:00Z');

INSERT INTO billing_point_pack
    (id, catalog_version_id, sku, name_zh, name_en, price_minor, currency, points, environment, status, sort_order)
VALUES
    ('00000000-0000-0000-0000-000000000211', '00000000-0000-0000-0000-000000000201', 'kb_pack_9', '轻量加量包', 'Starter', 900, 'CNY', 400, 'test', 'enabled', 1),
    ('00000000-0000-0000-0000-000000000212', '00000000-0000-0000-0000-000000000201', 'kb_pack_29', '常用加量包', 'Standard', 2900, 'CNY', 1400, 'test', 'enabled', 2),
    ('00000000-0000-0000-0000-000000000213', '00000000-0000-0000-0000-000000000201', 'kb_pack_99', '超值加量包', 'Pro', 9900, 'CNY', 5000, 'test', 'enabled', 3),
    ('00000000-0000-0000-0000-000000000221', '00000000-0000-0000-0000-000000000202', 'kb_pack_9', '轻量加量包', 'Starter', 900, 'CNY', 400, 'production', 'enabled', 1),
    ('00000000-0000-0000-0000-000000000222', '00000000-0000-0000-0000-000000000202', 'kb_pack_29', '常用加量包', 'Standard', 2900, 'CNY', 1400, 'production', 'enabled', 2),
    ('00000000-0000-0000-0000-000000000223', '00000000-0000-0000-0000-000000000202', 'kb_pack_99', '超值加量包', 'Pro', 9900, 'CNY', 5000, 'production', 'enabled', 3);

INSERT INTO billing_ai_rate
    (id, catalog_version_id, feature, unit, points_per_unit, success_rule_code, description_zh, description_en)
SELECT
    gen_random_uuid(),
    catalog.id,
    rate.feature,
    rate.unit,
    rate.points,
    rate.rule_code,
    rate.description_zh,
    rate.description_en
FROM billing_catalog_version catalog
CROSS JOIN (
    VALUES
        ('classify', 'entry', 5, 'non_empty_classification', '自动归类、摘要和标签', 'Classification, summary, and tags'),
        ('ai_text', 'entry', 5, 'non_empty_text_knowledge', 'AI 文字拆分、摘要、分类和标签', 'AI text organization'),
        ('webpage_vision', 'view', 10, 'visible_text_extracted', '单页截图内容分析', 'Single-page visual analysis'),
        ('document_extract', 'per_10k_chars', 10, 'document_text_extracted', '文档文字提取与结构整理', 'Document extraction and organization'),
        ('screenshot_ocr', 'image', 10, 'screenshot_text_extracted', '截图 OCR 与知识提取', 'Screenshot OCR and knowledge extraction'),
        ('internet_search', 'query', 3, 'search_completed', '互联网搜索', 'Internet search'),
        ('ai_answer', 'answer', 2, 'answer_displayed', 'AI 检索回答', 'AI answer')
) AS rate(feature, unit, points, rule_code, description_zh, description_en);

CREATE OR REPLACE FUNCTION prevent_billing_ledger_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'billing_ledger_entry is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER billing_ledger_no_update
    BEFORE UPDATE OR DELETE ON billing_ledger_entry
    FOR EACH ROW EXECUTE FUNCTION prevent_billing_ledger_mutation();
