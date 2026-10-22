import type { BridgeEnvironment } from "./config.js";

export interface CheckoutInput {
  orderId: string;
  sku: string;
  amountMinor: number;
  currency: "CNY";
  buyerIdentity: string;
  successUrl: string;
}

export interface CheckoutResult {
  sessionId: string;
  checkoutUrl: string;
  expiresAt: string;
  environment: BridgeEnvironment;
  productId: string;
}

export interface OrderStatusInput {
  orderId: string;
  providerSessionId: string;
}

export interface NormalizedStatus {
  environment: BridgeEnvironment;
  status: "pending" | "paid" | "closed" | "refunded" | "mismatch" | "unavailable";
  amountMinor: number | null;
  currency: string | null;
  productId: string | null;
  productMatch: boolean;
  checkedAt: string;
  payloadHash: string;
}

export interface NormalizedEvent {
  environment: BridgeEnvironment;
  eventType: "paid" | "refund" | "expired" | "conflict";
  sourceEventKey: string;
  providerEventId: string | null;
  deliveryId: string | null;
  orderId: string | null;
  providerOrderId: string | null;
  providerSessionId: string | null;
  providerRefundId: string | null;
  providerProductId: string | null;
  productMatch: boolean;
  amountMinor: number | null;
  currency: string | null;
  refundAmountMinor: number | null;
  reasonCode: string | null;
  signatureStatus: "valid" | "invalid" | "not_applicable";
  payloadHash: string;
  receivedAt: string;
}

export interface WaffoGateway {
  configured(): boolean;
  createCheckout(input: CheckoutInput, productId: string): Promise<{
    sessionId: string;
    checkoutUrl: string;
    expiresAt: string;
  }>;
  queryOrder(input: OrderStatusInput): Promise<{
    status: NormalizedStatus["status"];
    amountMinor: number | null;
    currency: string | null;
    productId: string | null;
  }>;
  verifyWebhook(rawBody: string, signature: string | undefined): unknown;
}
