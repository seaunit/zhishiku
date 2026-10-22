import { createHash } from "node:crypto";
import type { BridgeConfig } from "./config.js";
import type { NormalizedEvent } from "./types.js";

export function normalizeWebhook(
  rawBody: string,
  verifiedEvent: unknown,
  config: BridgeConfig,
  headers: Record<string, string | string[] | undefined>,
): NormalizedEvent {
  const event = verifiedEvent as {
    eventId?: string;
    eventType?: string;
    id?: string;
    deliveryId?: string;
    timestamp?: string;
    createdAt?: string;
    mode?: string;
    data?: Record<string, unknown>;
  };
  const data = event.data ?? {};
  const environmentMismatch =
    event.mode !== undefined &&
    event.mode !== config.environment &&
    !(config.environment === "production" && event.mode === "prod") &&
    !(config.environment === "test" && event.mode === "test");
  const timestamp = event.timestamp ?? event.createdAt;
  const timestampInvalid = timestampOutsideWindow(timestamp);
  const eventType =
    environmentMismatch || timestampInvalid ? "conflict" : mapEventType(event.eventType);
  const orderId = stringValue(data.orderMerchantExternalId);
  const providerEventId = stringValue(event.eventId);
  const deliveryId =
    stringValue(event.id) ??
    stringValue(event.deliveryId) ??
    headerValue(headers["x-waffo-delivery-id"]);
  const sourceEventKey =
    providerEventId ??
    deliveryId ??
    `payload:${sha256(rawBody)}`;
  const providerProductId = stringValue(data.productId);
  const productMatch =
    providerProductId !== null &&
    Object.values(config.productMap).includes(providerProductId);
  const amount = numberValue(data.amount ?? data.total);
  const refundAmount = numberValue(data.refundAmount ?? data.refundedAmount);
  return {
    environment: config.environment,
    eventType,
    sourceEventKey,
    providerEventId,
    deliveryId,
    orderId,
    providerOrderId: stringValue(data.orderId),
    providerSessionId: stringValue(data.checkoutSessionId ?? data.sessionId),
    providerRefundId: stringValue(data.refundId),
    providerProductId,
    productMatch: productMatch && !environmentMismatch,
    amountMinor: amount === null ? null : Math.round(amount * 100),
    currency: stringValue(data.currency),
    refundAmountMinor: refundAmount === null ? null : Math.round(refundAmount * 100),
    reasonCode: timestampInvalid
      ? "TIMESTAMP_OUTSIDE_WINDOW"
      : stringValue(data.refundReason ?? data.reason),
    signatureStatus: "valid",
    payloadHash: sha256(rawBody),
    receivedAt: timestamp ?? new Date().toISOString(),
  };
}

function timestampOutsideWindow(value: string | undefined): boolean {
  if (!value) return false;
  const parsed = Date.parse(value);
  if (!Number.isFinite(parsed)) return true;
  return Math.abs(Date.now() - parsed) > 5 * 60_000;
}

function mapEventType(value: string | undefined): NormalizedEvent["eventType"] {
  const normalized = value?.toLowerCase() ?? "";
  if (normalized.includes("refund") && normalized.includes("succeed")) return "refund";
  if (normalized.includes("complete") || normalized.includes("paid")) return "paid";
  if (normalized.includes("expire") || normalized.includes("cancel")) return "expired";
  return "conflict";
}

function stringValue(value: unknown): string | null {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function numberValue(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  if (value && typeof value === "object" && "amount" in value) {
    return numberValue((value as { amount?: unknown }).amount);
  }
  return null;
}

function headerValue(value: string | string[] | undefined): string | null {
  if (Array.isArray(value)) return value[0] ?? null;
  return value?.trim() || null;
}

export function sha256(value: string): string {
  return createHash("sha256").update(value, "utf8").digest("hex");
}
