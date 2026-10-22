import { describe, expect, it } from "vitest";
import { loadConfig } from "./config.js";

const PRODUCT_MAP = "kb_pack_9=PROD_9,kb_pack_29=PROD_29,kb_pack_99=PROD_99";
const PEM = `-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7
-----END PRIVATE KEY-----`;

describe("waffo bridge config", () => {
  it.each([
    ["SHIHAI_WAFFO_MERCHANT_ID", "SHIHAI_WAFFO_MERCHANT_ID is required."],
    [
      "SHIHAI_WAFFO_PRIVATE_KEY_BASE64",
      "SHIHAI_WAFFO_PRIVATE_KEY_BASE64 is required.",
    ],
    ["SHIHAI_WAFFO_STORE_ID", "SHIHAI_WAFFO_STORE_ID is required."],
  ])("fails closed when %s is missing", (name, message) => {
    expect(() =>
      loadConfig({
        ...prefixedBaseEnv(),
        [name]: "",
      }),
    ).toThrow(message);
  });

  it("falls back to legacy names during migration", () => {
    const config = loadConfig(legacyBaseEnv());

    expect(config).toMatchObject({
      environment: "test",
      storeId: "STO_LEGACY",
      merchantId: "MER_LEGACY",
      privateKey: PEM,
      productMap: {
        kb_pack_9: "LEGACY_9",
        kb_pack_29: "LEGACY_29",
        kb_pack_99: "LEGACY_99",
      },
      dryRun: false,
      port: 8092,
      successUrlAllowlist: ["https://legacy.example/"],
      appToBridgeSecret: "legacy-app-secret",
      bridgeToAppSecret: "legacy-bridge-secret",
      internalEventUrl: "http://127.0.0.1:8082/legacy-events",
    });
  });

  it("fails closed when canonical and legacy values conflict", () => {
    expect(() =>
      loadConfig({
        ...prefixedBaseEnv(),
        SHIHAI_WAFFO_STORE_ID: "STO_NEW",
        WAFFO_STORE_ID: "STO_OLD",
      }),
    ).toThrow(/SHIHAI_WAFFO_STORE_ID.*WAFFO_STORE_ID/);
  });

  it("rejects currencies other than CNY", () => {
    expect(() =>
      loadConfig({
        ...prefixedBaseEnv(),
        SHIHAI_WAFFO_CURRENCY: "USD",
      }),
    ).toThrow("SHIHAI_WAFFO_CURRENCY must be CNY.");
  });

  it("loads the store and preserves a valid PEM private key", () => {
    const config = loadConfig({
      ...prefixedBaseEnv(),
      SHIHAI_WAFFO_PRIVATE_KEY_BASE64: PEM,
    });

    expect(config.storeId).toBe("STO_1234567890123456789012");
    expect(config.privateKey).toBe(PEM);
    expect(config.environment).toBe("test");
  });

  it("decodes a base64-wrapped PEM private key", () => {
    const config = loadConfig({
      ...prefixedBaseEnv(),
      SHIHAI_WAFFO_PRIVATE_KEY_BASE64: Buffer.from(PEM, "utf8").toString("base64"),
    });

    expect(config.privateKey).toBe(PEM);
  });

  it("normalizes literal newline private keys", () => {
    const config = loadConfig({
      ...prefixedBaseEnv(),
      SHIHAI_WAFFO_PRIVATE_KEY_BASE64: PEM.replace(/\n/g, "\\n"),
    });

    expect(config.privateKey).toBe(PEM);
  });

  it("allows missing supplier credentials only in dry-run mode", () => {
    const config = loadConfig({
      ...prefixedBaseEnv(),
      SHIHAI_WAFFO_DRY_RUN: "true",
      SHIHAI_WAFFO_MERCHANT_ID: "",
      SHIHAI_WAFFO_PRIVATE_KEY_BASE64: "",
      SHIHAI_WAFFO_STORE_ID: "",
      SHIHAI_WAFFO_PRODUCT_MAP: PRODUCT_MAP,
    });

    expect(config.dryRun).toBe(true);
    expect(config.merchantId).toBe("");
  });
});

function prefixedBaseEnv(): NodeJS.ProcessEnv {
  return {
    SHIHAI_WAFFO_ENV: "test",
    SHIHAI_WAFFO_STORE_ID: "STO_1234567890123456789012",
    SHIHAI_WAFFO_MERCHANT_ID: "MER_1234567890123456789012",
    SHIHAI_WAFFO_PRIVATE_KEY_BASE64: PEM,
    SHIHAI_WAFFO_PRODUCT_MAP: PRODUCT_MAP,
    SHIHAI_WAFFO_DRY_RUN: "false",
    SHIHAI_APP_TO_BRIDGE_SECRET: "app-secret",
    SHIHAI_BRIDGE_TO_APP_SECRET: "bridge-secret",
    SHIHAI_INTERNAL_EVENT_URL:
      "http://127.0.0.1:8082/kb/api/internal/billing/waffo-events",
  };
}

function legacyBaseEnv(): NodeJS.ProcessEnv {
  return {
    WAFFO_ENV: "test",
    WAFFO_STORE_ID: "STO_LEGACY",
    WAFFO_MERCHANT_ID: "MER_LEGACY",
    WAFFO_PRIVATE_KEY_BASE64: PEM,
    WAFFO_PRODUCT_MAP: "kb_pack_9=LEGACY_9,kb_pack_29=LEGACY_29,kb_pack_99=LEGACY_99",
    WAFFO_DRY_RUN: "false",
    WAFFO_BRIDGE_PORT: "8092",
    WAFFO_SUCCESS_URL_ALLOWLIST: "https://legacy.example/",
    APP_TO_BRIDGE_SECRET: "legacy-app-secret",
    BRIDGE_TO_APP_SECRET: "legacy-bridge-secret",
    INTERNAL_EVENT_URL: "http://127.0.0.1:8082/legacy-events",
  };
}
