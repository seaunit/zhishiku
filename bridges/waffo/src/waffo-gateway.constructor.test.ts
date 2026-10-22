import { beforeEach, describe, expect, it, vi } from "vitest";

const constructorSpy = vi.hoisted(() => vi.fn());

vi.mock("@waffo/pancake-ts", () => ({
  WaffoPancake: vi.fn(function WaffoPancake(config: unknown) {
    constructorSpy(config);
    return {
      checkout: { authenticated: { create: vi.fn() } },
      graphql: { query: vi.fn() },
      webhooks: { verify: vi.fn() },
    };
  }),
}));

import type { BridgeConfig } from "./config.js";
import { SdkWaffoGateway } from "./waffo-gateway.js";

describe("SdkWaffoGateway construction", () => {
  beforeEach(() => {
    constructorSpy.mockClear();
  });

  it("passes the SDK environment and does not invent unsupported storeId config", () => {
    const config = bridgeConfig("production");

    new SdkWaffoGateway(config);

    expect(constructorSpy).toHaveBeenCalledWith({
      merchantId: config.merchantId,
      privateKey: config.privateKey,
      environment: "prod",
    });
  });
});

function bridgeConfig(environment: "test" | "production"): BridgeConfig {
  return {
    port: 8091,
    environment,
    storeId: "STO_1234567890123456789012",
    merchantId: "MER_1234567890123456789012",
    privateKey: "private-key",
    productMap: {
      kb_pack_9: "PROD_9",
      kb_pack_29: "PROD_29",
      kb_pack_99: "PROD_99",
    },
    currency: "CNY",
    dryRun: false,
    appToBridgeSecret: "app-secret",
    bridgeToAppSecret: "bridge-secret",
    internalEventUrl: "http://127.0.0.1:8082/kb/api/internal/billing/waffo-events",
    successUrlAllowlist: [],
  };
}
