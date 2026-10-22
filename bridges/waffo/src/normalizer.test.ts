import { describe, expect, it } from "vitest";
import type { BridgeConfig } from "./config.js";
import { normalizeWebhook } from "./normalizer.js";

describe("waffo webhook normalizer", () => {
  it("maps official payment metadata and charged amount", () => {
    const event = normalizeWebhook(
      JSON.stringify(paymentEvent()),
      paymentEvent(),
      config("production"),
      {},
    );

    expect(event).toMatchObject({
      environment: "production",
      eventType: "paid",
      orderId: "123e4567-e89b-42d3-a456-426614174900",
      providerRefundId: null,
      providerProductId: "PROD_9",
      productMatch: true,
      amountMinor: 900,
      currency: "CNY",
      signatureStatus: "valid",
    });
  });

  it("outputs the signed event environment even when it differs from bridge config", () => {
    const event = normalizeWebhook(
      JSON.stringify(paymentEvent()),
      paymentEvent(),
      config("test"),
      {},
    );

    expect(event.environment).toBe("production");
    expect(event.eventType).toBe("paid");
    expect(event.productMatch).toBe(true);
  });

  it("reads identifiers from legacy metadata", () => {
    const payload = paymentEvent();
    const data = payload.data as Record<string, unknown>;
    data.metadata = data.orderMetadata;
    delete data.orderMetadata;

    const event = normalizeWebhook(JSON.stringify(payload), payload, config("production"), {});

    expect(event.orderId).toBe("123e4567-e89b-42d3-a456-426614174900");
    expect(event.providerProductId).toBe("PROD_9");
    expect(event.productMatch).toBe(true);
  });

  it("uses listPrice total when chargedAmount is absent", () => {
    const payload = paymentEvent();
    const data = payload.data as Record<string, unknown>;
    delete data.chargedAmount;

    const event = normalizeWebhook(JSON.stringify(payload), payload, config("production"), {});

    expect(event.amountMinor).toBe(800);
  });

  it("maps official refund amounts and ticket identifiers", () => {
    const payload = refundEvent("REF_123");

    const event = normalizeWebhook(JSON.stringify(payload), payload, config("production"), {});

    expect(event).toMatchObject({
      eventType: "refund",
      providerRefundId: "REF_123",
      providerProductId: "PROD_9",
      productMatch: true,
      refundAmountMinor: 900,
      currency: "CNY",
    });
  });

  it("falls back to the stable event id for a refund identifier", () => {
    const payload = refundEvent(undefined);

    const event = normalizeWebhook(JSON.stringify(payload), payload, config("production"), {});

    expect(event.providerRefundId).toBe("PAY_1234567890123456789012");
  });

  it("does not treat the original payment total as the refunded amount", () => {
    const payload = refundEvent("REF_123");
    const data = payload.data as Record<string, unknown>;
    delete data.refundedAmount;
    data.originalPayment = { total: "8.00", taxAmount: "0.00" };

    const event = normalizeWebhook(JSON.stringify(payload), payload, config("production"), {});

    expect(event.refundAmountMinor).toBe(100);
  });

  it("leaves an unknown refund amount unset for manual review", () => {
    const payload = refundEvent("REF_123");
    const data = payload.data as Record<string, unknown>;
    delete data.refundedAmount;
    delete data.amount;
    data.originalPayment = { total: "9.00", taxAmount: "0.00" };

    const event = normalizeWebhook(JSON.stringify(payload), payload, config("production"), {});

    expect(event.refundAmountMinor).toBeNull();
  });

  it("uses the delivery id before the business event id for deduplication", () => {
    const payload = paymentEvent();

    const event = normalizeWebhook(JSON.stringify(payload), payload, config("production"), {});

    expect(event.sourceEventKey).toBe(payload.id);
    expect(event.providerEventId).toBe(payload.eventId);
  });

  it("rejects a signed event from another store", () => {
    const payload = paymentEvent();
    payload.storeId = "STO_OTHER";

    const event = normalizeWebhook(JSON.stringify(payload), payload, config("production"), {});

    expect(event.eventType).toBe("conflict");
    expect(event.productMatch).toBe(false);
  });

  it("rejects metadata whose item code maps to another product", () => {
    const payload = paymentEvent();
    const data = payload.data as Record<string, unknown>;
    (data.orderMetadata as Record<string, string>).itemCode = "kb_pack_29";

    const event = normalizeWebhook(JSON.stringify(payload), payload, config("production"), {});

    expect(event.productMatch).toBe(false);
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

function paymentEvent() {
  return {
    id: "8d89ff1d-6a87-4f2c-a5aa-b1fecc3e31f2",
    timestamp: new Date().toISOString(),
    eventType: "order.completed",
    eventId: "PAY_1234567890123456789012",
    storeId: "STO_1234567890123456789012",
    storeName: "识海",
    mode: "prod",
    data: {
      orderId: "ORD_1234567890123456789012",
      buyerEmail: "owner@example.com",
      orderMerchantExternalId: "123e4567-e89b-42d3-a456-426614174900",
      currency: "CNY",
      chargedAmount: "9.00",
      amount: "1.00",
      listPrice: {
        total: "8.00",
        subtotal: "8.00",
        taxAmount: "0.00",
      },
      originalPayment: {
        total: "7.00",
        subtotal: "7.00",
        taxAmount: "0.00",
      },
      orderMetadata: {
        orderId: "123e4567-e89b-42d3-a456-426614174900",
        itemCode: "kb_pack_9",
        waffoProductId: "PROD_9",
      },
    },
  };
}

function refundEvent(refundTicketMerchantExternalId: string | undefined) {
  return {
    id: "d7af8a45-cf95-464c-a97d-148f3e58eff6",
    timestamp: new Date().toISOString(),
    eventType: "refund.succeeded",
    eventId: "PAY_1234567890123456789012",
    storeId: "STO_1234567890123456789012",
    storeName: "识海",
    mode: "prod",
    data: {
      orderId: "ORD_1234567890123456789012",
      buyerEmail: "owner@example.com",
      orderMerchantExternalId: "123e4567-e89b-42d3-a456-426614174900",
      refundTicketMerchantExternalId,
      currency: "CNY",
      refundedAmount: "9.00",
      amount: "1.00",
      listPrice: {
        total: "8.00",
        subtotal: "8.00",
        taxAmount: "0.00",
      },
      originalPayment: {
        total: "9.00",
        subtotal: "9.00",
        taxAmount: "0.00",
      },
      orderMetadata: {
        orderId: "123e4567-e89b-42d3-a456-426614174000",
        itemCode: "kb_pack_9",
        waffoProductId: "PROD_9",
      },
    },
  };
}
