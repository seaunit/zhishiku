#!/usr/bin/env node

import { WaffoPancake } from "@waffo/pancake-ts";
import { normalizePrivateKeyInput, synchronizeCatalog } from "./catalog-sync.mjs";

const apply = process.argv.includes("--apply");
const publish = process.argv.includes("--publish");
if (!apply) {
  fail(
    2,
    "setup-catalog requires --apply to create missing live Waffo products; add --publish to publish them.",
  );
}

const storeId = requiredEnv("SHIHAI_WAFFO_STORE_ID", "WAFFO_STORE_ID");
const merchantId = requiredEnv("SHIHAI_WAFFO_MERCHANT_ID", "WAFFO_MERCHANT_ID");
const privateKey = normalizePrivateKeyInput(
  requiredEnv("SHIHAI_WAFFO_PRIVATE_KEY_BASE64", "WAFFO_PRIVATE_KEY_BASE64"),
);
const environment = parseEnvironment(
  envValue("SHIHAI_WAFFO_ENV", "WAFFO_ENV"),
);
if (environment === "test" && publish) {
  fail(2, "--publish is only allowed for production; test products are updated and activated in place.");
}
const client = new WaffoPancake({ merchantId, privateKey, environment });
await synchronizeCatalog({ client, storeId, publish, activate: environment === "test" });

function parseEnvironment(value) {
  if (value === "production" || value === "prod") return "prod";
  if (value === "test" || value === undefined || value === "") return "test";
  fail(2, "SHIHAI_WAFFO_ENV must be test or production.");
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
  if (normalizedPrimary && normalizedLegacy && normalizedPrimary !== normalizedLegacy) {
    fail(
      2,
      `Conflicting Shihai environment variables: ${primary} and ${legacy} must match during migration.`,
    );
  }
  return normalizedPrimary ? primaryValue : legacyValue;
}

function fail(exitCode, message) {
  console.error(`[setup-catalog] ${message}`);
  process.exit(exitCode);
}
