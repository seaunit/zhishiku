import { WaffoPancake, type TaxCategory } from "@waffo/pancake-ts";
import type { BridgeConfig } from "./config.js";
import type { CheckoutInput, OrderStatusInput, WaffoGateway } from "./types.js";

interface PaymentNode {
  status?: string;
  orderMerchantExternalId?: string;
  snapshotAmountDetails?: {
    total?: string | number;
    currency?: string;
  };
  onetimeOrder?: {
    onetimeProduct?: {
      id?: string;
    };
  };
}

export class SdkWaffoGateway implements WaffoGateway {
  private readonly client: WaffoPancake;

  constructor(
    private readonly config: BridgeConfig,
    client?: WaffoPancake,
  ) {
    this.client =
      client ??
      new WaffoPancake({
        merchantId: config.merchantId,
        privateKey: config.privateKey,
        environment: config.environment === "production" ? "prod" : "test",
      });
  }

  configured(): boolean {
    return Boolean(this.config.merchantId && this.config.privateKey);
  }

  async createCheckout(input: CheckoutInput, productId: string) {
    const result = await this.client.checkout.authenticated.create(
      {
        productId,
        currency: this.config.currency,
        buyerIdentity: input.buyerIdentity,
        orderMerchantExternalId: input.orderId,
        successUrl: input.successUrl,
        metadata: {
          orderId: input.orderId,
          itemCode: input.sku,
          waffoProductId: productId,
        },
        priceSnapshot: {
          amount: toDisplayAmount(input.amountMinor),
          taxCategory: "digital_goods" as TaxCategory,
        },
        expiresInSeconds: 900,
      },
      {
        idempotencyKey: input.orderId,
      },
    );
    return {
      sessionId: result.sessionId,
      checkoutUrl: result.checkoutUrl,
      expiresAt: result.expiresAt,
    };
  }

  async queryOrder(input: OrderStatusInput) {
    const result = await this.client.graphql.query<{ payments: PaymentNode[] }>({
      query: `query ($ref: String!) {
        payments(filter: { orderMerchantExternalId: { eq: $ref } }) {
          status orderMerchantExternalId
          snapshotAmountDetails { total currency }
          onetimeOrder { onetimeProduct { id } }
        }
      }`,
      variables: { ref: input.orderId },
    });
    const candidates = (result.data?.payments ?? []).filter(
      (candidate) => candidate.orderMerchantExternalId === input.orderId,
    );
    const payment =
      candidates.find((candidate) => normalizeStatus(candidate.status) === "paid") ??
      candidates.find((candidate) => normalizeStatus(candidate.status) === "pending") ??
      candidates[0];
    if (!payment) {
      return {
        status: "unavailable" as const,
        amountMinor: null,
        currency: null,
        productId: null,
      };
    }
    const amount = payment.snapshotAmountDetails?.total;
    const currency = payment.snapshotAmountDetails?.currency ?? null;
    return {
      status: normalizeStatus(payment.status),
      amountMinor: amount === undefined ? null : toMinor(amount),
      currency,
      productId: payment.onetimeOrder?.onetimeProduct?.id ?? null,
    };
  }

  verifyWebhook(rawBody: string, signature: string | undefined): unknown {
    return this.client.webhooks.verify(rawBody, signature ?? "");
  }
}

function normalizeStatus(status: string | undefined): "pending" | "paid" | "closed" | "refunded" | "mismatch" {
  switch (status?.toLowerCase()) {
    case "completed":
    case "paid":
    case "succeeded":
      return "paid";
    case "refunded":
      return "refunded";
    case "canceled":
    case "closed":
    case "expired":
      return "closed";
    case "pending":
    case "confirming":
      return "pending";
    case "failed":
      return "closed";
    default:
      return "mismatch";
  }
}

function toMinor(value: string | number): number | null {
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount < 0) return null;
  return Math.round(amount * 100);
}

function toDisplayAmount(amountMinor: number): string {
  if (!Number.isInteger(amountMinor) || amountMinor < 0) {
    throw new Error("Checkout amount must be a non-negative integer.");
  }
  return (amountMinor / 100).toFixed(2);
}
