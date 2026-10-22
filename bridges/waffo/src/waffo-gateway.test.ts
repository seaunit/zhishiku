import { generateKeyPairSync, sign } from "node:crypto";
import { afterEach, describe, expect, it, vi } from "vitest";
import { WaffoPancake } from "@waffo/pancake-ts";
import type { BridgeConfig } from "./config.js";
import { SdkWaffoGateway } from "./waffo-gateway.js";

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("SdkWaffoGateway", () => {
  it("passes idempotency, metadata, price snapshot and expiry to checkout", async () => {
    const create = vi.fn().mockResolvedValue({
      sessionId: "SESSION_1",
      checkoutUrl: "https://pancake.example/checkout#token=secret",
      expiresAt: "2026-10-22T01:10:00.000Z",
      token: "secret",
      tokenExpiresAt: "2026-10-22T01:05:00.000Z",
    });
    const client = {
      checkout: { authenticated: { create } },
      graphql: { query: vi.fn() },
      webhooks: { verify: vi.fn() },
    } as unknown as WaffoPancake;
    const gateway = new SdkWaffoGateway(config("test"), client);
    const input = checkoutInput();

    const result = await gateway.createCheckout(input, "PROD_9");

    expect(create).toHaveBeenCalledWith(
      {
        productId: "PROD_9",
        currency: "CNY",
        buyerIdentity: "owner@example.com",
        orderMerchantExternalId: input.orderId,
        successUrl: input.successUrl,
        metadata: {
          orderId: input.orderId,
          itemCode: "kb_pack_9",
          waffoProductId: "PROD_9",
        },
        priceSnapshot: {
          amount: "9.00",
          taxCategory: "digital_goods",
        },
        expiresInSeconds: 900,
      },
      { idempotencyKey: input.orderId },
    );
    expect(result.expiresAt).toBe("2026-10-22T01:10:00.000Z");
  });

  it("queries payment snapshots and onetime products using official GraphQL fields", async () => {
    const query = vi.fn().mockResolvedValue({
      data: {
        payments: [
          {
            status: "succeeded",
            orderMerchantExternalId: checkoutInput().orderId,
            snapshotAmountDetails: { total: "9.00", currency: "CNY" },
            onetimeOrder: { onetimeProduct: { id: "PROD_9" } },
          },
        ],
      },
    });
    const client = {
      checkout: { authenticated: { create: vi.fn() } },
      graphql: { query },
      webhooks: { verify: vi.fn() },
    } as unknown as WaffoPancake;
    const gateway = new SdkWaffoGateway(config("test"), client);

    const result = await gateway.queryOrder({
      orderId: checkoutInput().orderId,
      providerSessionId: "SESSION_1",
    });

    expect(result).toEqual({
      status: "paid",
      amountMinor: 900,
      currency: "CNY",
      productId: "PROD_9",
    });
    const request = query.mock.calls[0]?.[0] as { query: string; variables: unknown };
    expect(request.variables).toEqual({ ref: checkoutInput().orderId });
    expect(request.query).toContain("snapshotAmountDetails");
    expect(request.query).toContain("onetimeOrder");
    expect(request.query).toContain("onetimeProduct");
    expect(request.query).toContain("orderMerchantExternalId");
  });

  it("selects the successful payment when earlier attempts failed", async () => {
    const query = vi.fn().mockResolvedValue({
      data: {
        payments: [
          {
            status: "failed",
            orderMerchantExternalId: checkoutInput().orderId,
            snapshotAmountDetails: { total: "9.00", currency: "CNY" },
            onetimeOrder: { onetimeProduct: { id: "PROD_9" } },
          },
          {
            status: "succeeded",
            orderMerchantExternalId: checkoutInput().orderId,
            snapshotAmountDetails: { total: "9.00", currency: "CNY" },
            onetimeOrder: { onetimeProduct: { id: "PROD_9" } },
          },
        ],
      },
    });
    const client = {
      checkout: { authenticated: { create: vi.fn() } },
      graphql: { query },
      webhooks: { verify: vi.fn() },
    } as unknown as WaffoPancake;
    const gateway = new SdkWaffoGateway(config("test"), client);

    await expect(
      gateway.queryOrder({
        orderId: checkoutInput().orderId,
        providerSessionId: "SESSION_1",
      }),
    ).resolves.toMatchObject({
      status: "paid",
      amountMinor: 900,
      productId: "PROD_9",
    });
  });

  it.each([
    ["pending", "pending"],
    ["succeeded", "paid"],
    ["failed", "closed"],
    ["canceled", "closed"],
    ["unknown", "mismatch"],
  ])("maps PaymentStatus %s to %s", async (providerStatus, expected) => {
    const query = vi.fn().mockResolvedValue({
      data: {
        payments: [
          {
            status: providerStatus,
            orderMerchantExternalId: checkoutInput().orderId,
            snapshotAmountDetails: { total: "9.00", currency: "CNY" },
            onetimeOrder: { onetimeProduct: { id: "PROD_9" } },
          },
        ],
      },
    });
    const client = {
      checkout: { authenticated: { create: vi.fn() } },
      graphql: { query },
      webhooks: { verify: vi.fn() },
    } as unknown as WaffoPancake;
    const gateway = new SdkWaffoGateway(config("test"), client);

    const result = await gateway.queryOrder({
      orderId: checkoutInput().orderId,
      providerSessionId: "SESSION_1",
    });

    expect(result.status).toBe(expected);
  });

  it("does not lock webhook verification to the bridge environment", async () => {
    const verify = vi.fn().mockReturnValue({ mode: "test" });
    const client = {
      checkout: { authenticated: { create: vi.fn() } },
      graphql: { query: vi.fn() },
      webhooks: { verify },
    } as unknown as WaffoPancake;
    const gateway = new SdkWaffoGateway(config("production"), client);

    expect(gateway.verifyWebhook("{}", "signature")).toEqual({ mode: "test" });

    expect(verify.mock.calls[0]).toEqual(["{}", "signature"]);
  });

  it("verifies a real test signature through SDK environment auto-detection", () => {
    const { privateKey, publicKey } = generateKeyPairSync("rsa", {
      modulusLength: 2048,
      publicKeyEncoding: { type: "spki", format: "pem" },
      privateKeyEncoding: { type: "pkcs8", format: "pem" },
    });
    vi.stubEnv("WAFFO_WEBHOOK_TEST_PUBLIC_KEY", publicKey);
    const rawBody = JSON.stringify(paymentEvent());
    const timestamp = Date.now().toString();
    const signature = sign("RSA-SHA256", Buffer.from(`${timestamp}.${rawBody}`), privateKey).toString(
      "base64",
    );
    const gateway = new SdkWaffoGateway({
      ...config("production"),
      merchantId: "MER_1234567890123456789012",
      privateKey,
    });

    const event = gateway.verifyWebhook(rawBody, `t=${timestamp},v1=${signature}`);

    expect(event).toMatchObject({ mode: "test", eventType: "order.completed" });
  });

  it("rejects a tampered real signature", () => {
    const { privateKey, publicKey } = generateKeyPairSync("rsa", {
      modulusLength: 2048,
      publicKeyEncoding: { type: "spki", format: "pem" },
      privateKeyEncoding: { type: "pkcs8", format: "pem" },
    });
    vi.stubEnv("WAFFO_WEBHOOK_TEST_PUBLIC_KEY", publicKey);
    const rawBody = JSON.stringify(paymentEvent());
    const timestamp = Date.now().toString();
    const signature = sign("RSA-SHA256", Buffer.from(`${timestamp}.${rawBody}`), privateKey).toString(
      "base64",
    );
    const tampered = `${signature.slice(0, -4)}AAAA`;
    const gateway = new SdkWaffoGateway({
      ...config("test"),
      privateKey,
    });

    expect(() => gateway.verifyWebhook(rawBody, `t=${timestamp},v1=${tampered}`)).toThrow(
      /signature/i,
    );
  });

  it("accepts a bare PKCS#8 base64 private key", () => {
    const { privateKey } = generateKeyPairSync("rsa", {
      modulusLength: 2048,
      publicKeyEncoding: { type: "spki", format: "pem" },
      privateKeyEncoding: { type: "pkcs8", format: "der" },
    });
    const gateway = new SdkWaffoGateway({
      ...config("test"),
      privateKey: privateKey.toString("base64"),
    });

    expect(gateway.configured()).toBe(true);
  });
});

function config(environment: "test" | "production"): BridgeConfig {
  return {
    port: 8091,
    environment,
    storeId: "STO_1234567890123456789012",
    merchantId: "MER_1234567890123456789012",
    privateKey: "test-key",
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

function checkoutInput() {
  return {
    orderId: "123e4567-e89b-42d3-a456-426614174900",
    sku: "kb_pack_9",
    amountMinor: 900,
    currency: "CNY" as const,
    buyerIdentity: "owner@example.com",
    successUrl: "https://seaunit.site/kb/billing/return",
  };
}

function paymentEvent() {
  return {
    id: "8d89ff1d-6a87-4f2c-a5aa-b1fecc3e31f2",
    timestamp: new Date().toISOString(),
    eventType: "order.completed",
    eventId: "PAY_1234567890123456789012",
    storeId: "STO_1234567890123456789012",
    storeName: "识海",
    mode: "test",
    data: {
      orderId: "ORD_1234567890123456789012",
      buyerEmail: "owner@example.com",
      currency: "CNY",
      chargedAmount: "9.00",
      orderMetadata: {
        orderId: "123e4567-e89b-42d3-a456-426614174900",
        itemCode: "kb_pack_9",
        waffoProductId: "PROD_9",
      },
    },
  };
}
