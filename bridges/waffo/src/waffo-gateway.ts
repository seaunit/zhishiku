import { WaffoPancake } from "@waffo/pancake-ts";
import type { BridgeConfig } from "./config.js";
import type { CheckoutInput, OrderStatusInput, WaffoGateway } from "./types.js";

interface PaymentNode {
  status?: string;
  total?: { amount?: string | number; currency?: string };
  amount?: string | number;
  currency?: string;
  productId?: string;
  orderMerchantExternalId?: string;
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
      });
  }

  configured(): boolean {
    return Boolean(this.config.merchantId && this.config.privateKey);
  }

  async createCheckout(input: CheckoutInput, productId: string) {
    const result = await this.client.checkout.authenticated.create({
      productId,
      currency: this.config.currency,
      buyerIdentity: input.buyerIdentity,
      orderMerchantExternalId: input.orderId,
      successUrl: input.successUrl,
    });
    return {
      sessionId: result.sessionId,
      checkoutUrl: result.checkoutUrl,
      expiresAt: new Date(Date.now() + 5 * 60_000).toISOString(),
    };
  }

  async queryOrder(input: OrderStatusInput) {
    const result = await this.client.graphql.query<{ payments: PaymentNode[] }>({
      query: `query ($ref: String!) {
        payments(filter: { orderMerchantExternalId: { eq: $ref } }) {
          status amount currency productId orderMerchantExternalId
          total { amount currency }
        }
      }`,
      variables: { ref: input.orderId },
    });
    const payment = result.data?.payments?.[0];
    if (!payment) {
      return {
        status: "unavailable" as const,
        amountMinor: null,
        currency: null,
        productId: null,
      };
    }
    const amount = payment.total?.amount ?? payment.amount;
    const currency = payment.total?.currency ?? payment.currency ?? null;
    return {
      status: normalizeStatus(payment.status),
      amountMinor: amount === undefined ? null : toMinor(amount),
      currency,
      productId: payment.productId ?? null,
    };
  }

  verifyWebhook(rawBody: string, signature: string | undefined): unknown {
    return this.client.webhooks.verify(rawBody, signature ?? "", {
      environment: this.config.environment === "production" ? "prod" : "test",
    });
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
    default:
      return "mismatch";
  }
}

function toMinor(value: string | number): number | null {
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount < 0) return null;
  return Math.round(amount * 100);
}
