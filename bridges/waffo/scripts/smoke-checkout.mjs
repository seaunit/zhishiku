#!/usr/bin/env node

const PACK_AMOUNTS = {
  kb_pack_9: 900,
  kb_pack_29: 2900,
  kb_pack_99: 9900,
};

if (!process.argv.includes("--confirm")) {
  fail(
    2,
    "smoke-checkout requires --confirm because it creates a real Waffo checkout session.",
  );
}

const orderId = envValue("SHIHAI_SMOKE_ORDER_ID", "KB_SMOKE_ORDER_ID")?.trim();
const internalSecret = envValue(
  "SHIHAI_APP_TO_BRIDGE_SECRET",
  "APP_TO_BRIDGE_SECRET",
)?.trim();
const bridgeUrl = (
  envValue("SHIHAI_WAFFO_BRIDGE_URL", "WAFFO_BRIDGE_URL") ??
  "http://127.0.0.1:8091"
).replace(/\/+$/, "");
const sku = envValue("SHIHAI_SMOKE_SKU", "KB_SMOKE_SKU")?.trim() ?? "kb_pack_9";
const amountMinor = PACK_AMOUNTS[sku];
if (!orderId) fail(2, "SHIHAI_SMOKE_ORDER_ID is required.");
if (!internalSecret) fail(2, "SHIHAI_APP_TO_BRIDGE_SECRET is required.");
if (amountMinor === undefined) fail(2, `Unsupported smoke-checkout SKU: ${sku}`);

const response = await fetch(`${bridgeUrl}/checkout`, {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Idempotency-Key": orderId,
    "X-Internal-Secret": internalSecret,
  },
  body: JSON.stringify({
    orderId,
    sku,
    amountMinor,
    currency: "CNY",
    buyerIdentity:
      envValue("SHIHAI_SMOKE_BUYER_IDENTITY", "KB_SMOKE_BUYER_IDENTITY")?.trim() ??
      "operator-smoke",
    buyerEmail:
      envValue("SHIHAI_SMOKE_BUYER_EMAIL", "KB_SMOKE_BUYER_EMAIL")?.trim() ??
      "operator-smoke@seaunit.site",
    successUrl:
      envValue("SHIHAI_SMOKE_SUCCESS_URL", "KB_SMOKE_SUCCESS_URL")?.trim() ??
      "https://seaunit.site/kb/billing/return",
  }),
  signal: AbortSignal.timeout(20_000),
}).catch((error) => {
  fail(1, `Bridge request failed: ${safeErrorMessage(error)}`);
});

const body = await response.json().catch(() => ({}));
if (!response.ok) {
  fail(
    1,
    `Bridge rejected checkout: ${String(body.code ?? response.status)} ${String(
      body.message ?? "",
    )}`.trim(),
  );
}

console.log(
  JSON.stringify(
    {
      created: true,
      orderId,
      sessionId: body.sessionId ?? null,
      expiresAt: body.expiresAt ?? null,
      environment: body.environment ?? null,
      productId: body.productId ?? null,
      checkoutUrlPresent: typeof body.checkoutUrl === "string" && body.checkoutUrl.length > 0,
    },
    null,
    2,
  ),
);

function fail(exitCode, message) {
  console.error(`[smoke-checkout] ${message}`);
  process.exit(exitCode);
}

function safeErrorMessage(error) {
  return error instanceof Error ? error.message : "unknown transport error";
}

function envValue(primary, legacy) {
  const primaryValue = process.env[primary];
  const legacyValue = process.env[legacy];
  const normalizedPrimary = primaryValue?.trim();
  const normalizedLegacy = legacyValue?.trim();
  if (normalizedPrimary && normalizedLegacy && normalizedPrimary !== normalizedLegacy) {
    fail(
      2,
      `Conflicting Shihai environment variables: ${primary} and ${legacy} must match during migration.`,
    );
  }
  return normalizedPrimary ? primaryValue : legacyValue;
}
