import { describe, expect, it } from "vitest";
import { loadConfig } from "./config.js";

const PRODUCT_MAP = "kb_pack_9=PROD_9,kb_pack_29=PROD_29,kb_pack_99=PROD_99";
const PEM = `-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7
-----END PRIVATE KEY-----`;

describe("waffo bridge config", () => {
  it.each([
    ["WAFFO_MERCHANT_ID", "WAFFO_MERCHANT_ID is required."],
    ["WAFFO_PRIVATE_KEY_BASE64", "WAFFO_PRIVATE_KEY_BASE64 is required."],
    ["WAFFO_STORE_ID", "WAFFO_STORE_ID is required."],
  ])("fails closed when %s is missing", (name, message) => {
    expect(() =>
      loadConfig({
        ...baseEnv(),
        [name]: "",
      }),
    ).toThrow(message);
  });

  it("loads the store and preserves a valid PEM private key", () => {
    const config = loadConfig({
      ...baseEnv(),
      WAFFO_PRIVATE_KEY_BASE64: PEM,
    });

    expect(config.storeId).toBe("STO_1234567890123456789012");
    expect(config.privateKey).toBe(PEM);
    expect(config.environment).toBe("test");
  });

  it("decodes a base64-wrapped PEM private key", () => {
    const config = loadConfig({
      ...baseEnv(),
      WAFFO_PRIVATE_KEY_BASE64: Buffer.from(PEM, "utf8").toString("base64"),
    });

    expect(config.privateKey).toBe(PEM);
  });

  it("normalizes literal newline private keys", () => {
    const config = loadConfig({
      ...baseEnv(),
      WAFFO_PRIVATE_KEY_BASE64: PEM.replace(/\n/g, "\\n"),
    });

    expect(config.privateKey).toBe(PEM);
  });

  it("allows missing supplier credentials only in dry-run mode", () => {
    const config = loadConfig({
      ...baseEnv(),
      WAFFO_DRY_RUN: "true",
      WAFFO_MERCHANT_ID: "",
      WAFFO_PRIVATE_KEY_BASE64: "",
      WAFFO_STORE_ID: "",
      WAFFO_PRODUCT_MAP: PRODUCT_MAP,
    });

    expect(config.dryRun).toBe(true);
    expect(config.merchantId).toBe("");
  });
});

function baseEnv(): NodeJS.ProcessEnv {
  return {
    WAFFO_ENV: "test",
    WAFFO_STORE_ID: "STO_1234567890123456789012",
    WAFFO_MERCHANT_ID: "MER_1234567890123456789012",
    WAFFO_PRIVATE_KEY_BASE64: PEM,
    WAFFO_PRODUCT_MAP: PRODUCT_MAP,
    WAFFO_DRY_RUN: "false",
    APP_TO_BRIDGE_SECRET: "app-secret",
    BRIDGE_TO_APP_SECRET: "bridge-secret",
    INTERNAL_EVENT_URL: "http://127.0.0.1:8082/kb/api/internal/billing/waffo-events",
  };
}
