import { afterEach, describe, expect, it, vi } from "vitest";
import type { AddressInfo } from "node:net";
import type { Server } from "node:http";
import { createBridgeServer } from "./server.js";
import type { BridgeConfig } from "./config.js";
import type { WaffoGateway } from "./types.js";

const servers: Server[] = [];

afterEach(async () => {
  await Promise.all(
    servers.splice(0).map(
      (server) =>
        new Promise<void>((resolve) => {
          server.close(() => resolve());
        }),
    ),
  );
});

describe("waffo bridge", () => {
  it("validates internal auth and product facts", async () => {
    const { baseUrl } = await start();
    const unauthorized = await fetch(`${baseUrl}/checkout`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(checkout()),
    });
    expect(unauthorized.status).toBe(401);

    const mismatch = await fetch(`${baseUrl}/checkout`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Internal-Secret": "app-secret",
        "Idempotency-Key": checkout().orderId,
      },
      body: JSON.stringify({ ...checkout(), amountMinor: 1 }),
    });
    expect(mismatch.status).toBe(409);
  });

  it("creates a dry-run checkout and forwards normalized webhooks", async () => {
    const fetchImpl = vi.fn().mockResolvedValue(new Response("{}", { status: 200 }));
    const { baseUrl } = await start(fetchImpl);
    const checkoutResponse = await fetch(`${baseUrl}/checkout`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Internal-Secret": "app-secret",
        "Idempotency-Key": checkout().orderId,
      },
      body: JSON.stringify(checkout()),
    });
    expect(checkoutResponse.status).toBe(200);
    await expect(checkoutResponse.json()).resolves.toMatchObject({
      environment: "test",
      productId: "PROD_9",
    });

    const webhook = await fetch(`${baseUrl}/webhooks/waffo`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Waffo-Signature": "dry-signature",
      },
      body: JSON.stringify({
        eventId: "EVT_1",
        eventType: "order.completed",
        mode: "test",
        data: {
          orderMerchantExternalId: checkout().orderId,
          orderMetadata: {
            orderId: checkout().orderId,
            itemCode: "kb_pack_9",
            waffoProductId: "PROD_9",
          },
          chargedAmount: "9.00",
          currency: "CNY",
        },
      }),
    });
    expect(webhook.status).toBe(200);
    expect(fetchImpl).toHaveBeenCalledWith(
      "http://127.0.0.1:8082/kb/api/internal/billing/waffo-events",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ "X-Internal-Secret": "bridge-secret" }),
      }),
    );
  });

  it("uses verified webhook events outside dry-run mode", async () => {
    const fetchImpl = vi.fn().mockResolvedValue(new Response("{}", { status: 200 }));
    const verifiedEvent = {
      id: "DELIVERY_1",
      timestamp: new Date().toISOString(),
      eventId: "EVT_1",
      eventType: "order.completed",
      storeId: "STO_1234567890123456789012",
      mode: "test",
      data: {
        orderMerchantExternalId: checkout().orderId,
        orderMetadata: {
          orderId: checkout().orderId,
          itemCode: "kb_pack_9",
          waffoProductId: "PROD_9",
        },
        chargedAmount: "9.00",
        currency: "CNY",
      },
    };
    const verifyWebhook = vi.fn().mockReturnValue(verifiedEvent);
    const { baseUrl } = await start(fetchImpl, {
      dryRun: false,
      gateway: {
        configured: () => true,
        createCheckout: vi.fn(),
        queryOrder: vi.fn(),
        verifyWebhook,
      },
    });

    const response = await fetch(`${baseUrl}/webhooks/waffo`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Waffo-Signature": "signed-header",
      },
      body: JSON.stringify(verifiedEvent),
    });

    expect(response.status).toBe(200);
    expect(verifyWebhook).toHaveBeenCalledWith(JSON.stringify(verifiedEvent), "signed-header");
    const forwarded = JSON.parse(String((fetchImpl.mock.calls[0]?.[1] as RequestInit).body));
    expect(forwarded).toMatchObject({
      environment: "test",
      eventType: "paid",
      productMatch: true,
      amountMinor: 900,
    });
  });

  it("rejects invalid signatures without forwarding them", async () => {
    const fetchImpl = vi.fn();
    const { baseUrl } = await start(fetchImpl, {
      dryRun: false,
      gateway: {
        configured: () => true,
        createCheckout: vi.fn(),
        queryOrder: vi.fn(),
        verifyWebhook: vi.fn(() => {
          throw new Error("invalid signature");
        }),
      },
    });

    const response = await fetch(`${baseUrl}/webhooks/waffo`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Waffo-Signature": "tampered",
      },
      body: "{}",
    });

    expect(response.status).toBe(401);
    expect(fetchImpl).not.toHaveBeenCalled();
  });

  it("never treats a missing query product id as a product match", async () => {
    const { baseUrl } = await start();
    const response = await fetch(`${baseUrl}/orders/status`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Internal-Secret": "app-secret",
      },
      body: JSON.stringify({
        orderId: checkout().orderId,
        providerSessionId: "SESSION_1",
      }),
    });

    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toMatchObject({
      status: "pending",
      productId: null,
      productMatch: false,
    });
  });
});

async function start(
  fetchImpl?: typeof fetch,
  overrides: {
    dryRun?: boolean;
    gateway?: WaffoGateway;
  } = {},
) {
  const config: BridgeConfig = {
    port: 0,
    environment: "test",
    storeId: "STO_1234567890123456789012",
    merchantId: "",
    privateKey: "",
    productMap: {
      kb_pack_9: "PROD_9",
      kb_pack_29: "PROD_29",
      kb_pack_99: "PROD_99",
    },
    currency: "CNY",
    dryRun: overrides.dryRun ?? true,
    appToBridgeSecret: "app-secret",
    bridgeToAppSecret: "bridge-secret",
    internalEventUrl: "http://127.0.0.1:8082/kb/api/internal/billing/waffo-events",
    successUrlAllowlist: ["http://127.0.0.1:4173/kb/billing/return"],
  };
  const gateway: WaffoGateway = overrides.gateway ?? {
    configured: () => true,
    createCheckout: vi.fn(),
    queryOrder: vi.fn(),
    verifyWebhook: () => ({}),
  };
  const server = createBridgeServer({ config, gateway, fetchImpl });
  servers.push(server);
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  const address = server.address() as AddressInfo;
  return { baseUrl: `http://127.0.0.1:${address.port}` };
}

function checkout() {
  return {
    orderId: "123e4567-e89b-42d3-a456-426614174900",
    sku: "kb_pack_9",
    amountMinor: 900,
    currency: "CNY" as const,
    buyerIdentity: "owner@example.com",
    successUrl: "http://127.0.0.1:4173/kb/billing/return",
  };
}
