#!/usr/bin/env node

import { pathToFileURL } from "node:url";
import { WaffoPancake, WebhookEventType } from "@waffo/pancake-ts";

export const REQUIRED_EVENTS = [
  WebhookEventType.OrderCompleted,
  WebhookEventType.RefundSucceeded,
];

export function normalizeWebhookUrl(value) {
  const url = new URL(value);
  if (url.protocol !== "https:") {
    throw new Error("SHIHAI_WAFFO_TEST_WEBHOOK_URL must use HTTPS.");
  }
  if (!url.pathname.endsWith("/webhooks/waffo")) {
    throw new Error(
      "SHIHAI_WAFFO_TEST_WEBHOOK_URL must end with /webhooks/waffo.",
    );
  }
  if (
    url.hostname === "seaunit.site" &&
    url.pathname.startsWith("/pay/webhooks/")
  ) {
    throw new Error(
      "The Shuati production callback must not be used for the Shihai local test environment.",
    );
  }
  return url.toString();
}

export function hasRequiredEvents(webhook) {
  return sameEvents(webhook.events, REQUIRED_EVENTS);
}

export function selectDedicatedWebhook(webhooks, configuredId) {
  if (configuredId) {
    const configured = webhooks.find((webhook) => webhook.id === configuredId);
    if (!configured) {
      throw new Error(
        "SHIHAI_WAFFO_TEST_WEBHOOK_ID does not identify a webhook in the test store.",
      );
    }
    return configured;
  }
  const candidates = webhooks.filter(
    (webhook) =>
      webhook.channel === "http" &&
      webhook.testMode === true &&
      hasRequiredEvents(webhook),
  );
  if (candidates.length > 1) {
    throw new Error(
      "Multiple Waffo test webhooks use the Shihai event set; set SHIHAI_WAFFO_TEST_WEBHOOK_ID explicitly.",
    );
  }
  return candidates[0] ?? null;
}

export function sameEvents(left, right) {
  if (!Array.isArray(left) || !Array.isArray(right)) return false;
  if (left.length !== right.length) return false;
  const leftSet = new Set(left);
  return leftSet.size === right.length && right.every((event) => leftSet.has(event));
}

async function main() {
  const environment = envValue("SHIHAI_WAFFO_ENV", "WAFFO_ENV");
  if (environment !== "test") {
    fail(2, "Webhook automation is only allowed for the Waffo test environment.");
  }
  const storeId = requiredEnv("SHIHAI_WAFFO_STORE_ID", "WAFFO_STORE_ID");
  const merchantId = requiredEnv(
    "SHIHAI_WAFFO_MERCHANT_ID",
    "WAFFO_MERCHANT_ID",
  );
  const privateKey = normalizePrivateKeyInput(
    requiredEnv(
      "SHIHAI_WAFFO_PRIVATE_KEY_BASE64",
      "WAFFO_PRIVATE_KEY_BASE64",
    ),
  );
  const webhookUrl = normalizeWebhookUrl(
    requiredEnv(
      "SHIHAI_WAFFO_TEST_WEBHOOK_URL",
      "WAFFO_TEST_WEBHOOK_URL",
    ),
  );
  const configuredId = envValue(
    "SHIHAI_WAFFO_TEST_WEBHOOK_ID",
    "WAFFO_TEST_WEBHOOK_ID",
  )?.trim();
  const client = new WaffoPancake({
    merchantId,
    privateKey,
    environment: "test",
  });
  const result = await client.graphql.query({
    query: `query ($id: String!) {
      store(id: $id) {
        storeWebhooks {
          id channel url events testMode
        }
      }
    }`,
    variables: { id: storeId },
  });
  if (result.errors?.length) {
    throw new Error(
      `Waffo webhook query failed: ${result.errors
        .map((error) => error.message)
        .join("; ")}`,
    );
  }
  const webhooks = result.data?.store?.storeWebhooks ?? [];
  const existing = selectDedicatedWebhook(webhooks, configuredId);
  let action = "unchanged";
  let webhook = existing;
  if (!existing) {
    const created = await client.webhooks.add({
      storeId,
      channel: "http",
      url: webhookUrl,
      events: REQUIRED_EVENTS,
      testMode: true,
    });
    webhook = created.webhook;
    action = "created";
  } else if (
    existing.url !== webhookUrl ||
    !sameEvents(existing.events, REQUIRED_EVENTS)
  ) {
    const updated = await client.webhooks.update({
      id: existing.id,
      url: webhookUrl,
      events: REQUIRED_EVENTS,
    });
    webhook = updated.webhook;
    action = "updated";
  }
  console.log(
    JSON.stringify(
      {
        configured: true,
        action,
        webhook: {
          id: webhook.id,
          channel: webhook.channel,
          url: webhook.url,
          events: webhook.events,
          testMode: webhook.testMode,
        },
      },
      null,
      2,
    ),
  );
}

function normalizePrivateKeyInput(value) {
  const normalized = value.replace(/\\n/g, "\n").replace(/\r\n/g, "\n").trim();
  if (!normalized || normalized.includes("-----BEGIN ")) {
    return normalized;
  }
  const compact = normalized.replace(/\s+/g, "");
  if (!/^[A-Za-z0-9+/]+=*$/.test(compact)) {
    return normalized;
  }
  const decoded = Buffer.from(compact, "base64").toString("utf8").trim();
  if (decoded.includes("-----BEGIN ") && decoded.includes("PRIVATE KEY-----")) {
    return decoded.replace(/\r\n/g, "\n");
  }
  return normalized;
}

function requiredEnv(primary, legacy) {
  const value = envValue(primary, legacy)?.trim();
  if (!value) fail(2, `${primary} is required.`);
  return value;
}

function envValue(primary, legacy) {
  const primaryValue = process.env[primary];
  const legacyValue = process.env[legacy];
  const normalizedPrimary = primaryValue?.trim();
  const normalizedLegacy = legacyValue?.trim();
  if (
    normalizedPrimary &&
    normalizedLegacy &&
    normalizedPrimary !== normalizedLegacy
  ) {
    fail(
      2,
      `Conflicting Shihai environment variables: ${primary} and legacy ${legacy} must match during migration.`,
    );
  }
  return normalizedPrimary ? primaryValue : legacyValue;
}

function fail(exitCode, message) {
  console.error(`[configure-test-webhook] ${message}`);
  process.exit(exitCode);
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  main().catch((error) => {
    console.error(
      `[configure-test-webhook] ${
        error instanceof Error ? error.message : "unexpected failure"
      }`,
    );
    process.exitCode = 1;
  });
}
