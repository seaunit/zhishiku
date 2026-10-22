#!/usr/bin/env node

import { TaxCategory, WaffoPancake } from "@waffo/pancake-ts";

const CATALOG = [
  { sku: "kb_pack_9", name: "Shihai 400 AI credits", amount: "9.00" },
  { sku: "kb_pack_29", name: "Shihai 1,400 AI credits", amount: "29.00" },
  { sku: "kb_pack_99", name: "Shihai 5,000 AI credits", amount: "99.00" },
];

const apply = process.argv.includes("--apply");
const publish = process.argv.includes("--publish");
if (!apply) {
  fail(
    2,
    "setup-catalog requires --apply to create missing live Waffo products; add --publish to publish them.",
  );
}

const storeId = requiredEnv("WAFFO_STORE_ID");
const merchantId = requiredEnv("WAFFO_MERCHANT_ID");
const privateKey = resolvePrivateKey(requiredEnv("WAFFO_PRIVATE_KEY_BASE64"));
const environment = parseEnvironment(process.env.WAFFO_ENV);
const client = new WaffoPancake({ merchantId, privateKey, environment });
const products = await listProducts(client, storeId);

for (const item of CATALOG) {
  let product = products.find((candidate) => metadataSku(candidate.metadata) === item.sku);
  let action = "existing";
  if (!product) {
    const created = await client.onetimeProducts.create(
      {
        storeId,
        name: item.name,
        prices: {
          CNY: {
            amount: item.amount,
            taxCategory: TaxCategory.DigitalGoods,
          },
        },
        metadata: { sku: item.sku },
      },
      { idempotencyKey: `zhishiku-catalog-${item.sku}` },
    );
    product = created.product;
    action = "created";
  }
  if (publish) {
    await client.onetimeProducts.publish(
      { id: product.id },
      { idempotencyKey: `zhishiku-publish-${item.sku}` },
    );
    action = action === "created" ? "created-published" : "published";
  }
  console.log(`${item.sku}=${product.id} (${action})`);
}

async function listProducts(client, storeId) {
  const result = await client.graphql.query({
    query: `query ($storeId: String!) {
      onetimeProducts(storeId: $storeId) {
        id
        metadata
      }
    }`,
    variables: { storeId },
  });
  return result.data?.onetimeProducts ?? [];
}

function metadataSku(metadata) {
  if (!metadata) return null;
  if (typeof metadata === "string") {
    try {
      return JSON.parse(metadata).sku ?? null;
    } catch {
      return null;
    }
  }
  return typeof metadata.sku === "string" ? metadata.sku : null;
}

function parseEnvironment(value) {
  if (value === "production" || value === "prod") return "prod";
  if (value === "test" || value === undefined || value === "") return "test";
  fail(2, "WAFFO_ENV must be test or production.");
}

function requiredEnv(name) {
  const value = process.env[name]?.trim();
  if (!value) fail(2, `${name} is required.`);
  return value;
}

function resolvePrivateKey(value) {
  const normalized = value.replace(/\\n/g, "\n").replace(/\r\n/g, "\n").trim();
  const compact = normalized.replace(/\s+/g, "");
  const decoded = /^[A-Za-z0-9+/]+=*$/.test(compact)
    ? Buffer.from(compact, "base64").toString("utf8").trim()
    : "";
  const candidates = [...new Set([normalized, decoded].filter(Boolean))];
  for (const candidate of candidates) {
    try {
      new WaffoPancake({
        merchantId,
        privateKey: candidate,
        environment: parseEnvironment(process.env.WAFFO_ENV),
      });
      return candidate;
    } catch {
      // Try the next supported encoding without logging private material.
    }
  }
  fail(
    2,
    "WAFFO_PRIVATE_KEY_BASE64 must contain a PEM key, escaped PEM key, or Base64-wrapped PEM key.",
  );
}

function fail(exitCode, message) {
  console.error(`[setup-catalog] ${message}`);
  process.exit(exitCode);
}
