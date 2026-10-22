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
    storeId?: string;
    data?: Record<string, unknown>;
  };
  const data = event.data ?? {};
  const environment = eventEnvironment(event.mode, config.environment);
  const storeMatches =
    config.dryRun || !config.storeId || event.storeId === config.storeId;
  const timestamp = event.timestamp ?? event.createdAt;
  const timestampInvalid = timestampOutsideWindow(timestamp);
  const eventType =
    timestampInvalid || !storeMatches ? "conflict" : mapEventType(event.eventType);
  const metadata =
    objectValue(data.orderMetadata) ??
    objectValue(data.metadata) ??
    {};
  const orderId =
    stringValue(data.orderMerchantExternalId) ??
    stringValue(metadata.orderId);
  const providerEventId = stringValue(event.eventId);
  const deliveryId =
    stringValue(event.id) ??
    stringValue(event.deliveryId) ??
    headerValue(headers["x-waffo-delivery-id"]);
  const sourceEventKey =
    deliveryId ??
    providerEventId ??
    `payload:${sha256(rawBody)}`;
  const itemCode = stringValue(metadata.itemCode) ?? stringValue(metadata.sku);
  const providerProductId =
    stringValue(metadata.waffoProductId) ??
    stringValue(metadata.waffo_product_id) ??
    stringValue(metadata.productId);
  const productMatch =
    providerProductId !== null &&
    Object.values(config.productMap).includes(providerProductId) &&
    (itemCode === null || config.productMap[itemCode] === providerProductId);
  const amount =
    numberValue(data.chargedAmount) ??
    breakdownTotal(data.listPrice) ??
    numberValue(data.amount) ??
    numberValue(data.total);
  const refundAmount =
    numberValue(data.refundedAmount) ??
    numberValue(data.amount);
  const providerRefundId =
    eventType === "refund"
      ? stringValue(data.refundTicketMerchantExternalId) ??
        providerEventId ??
        deliveryId
      : null;
  return {
    environment,
    eventType,
    sourceEventKey,
    providerEventId,
    deliveryId,
    orderId,
    providerOrderId: stringValue(data.orderId),
    providerSessionId: stringValue(data.checkoutSessionId ?? data.sessionId),
    providerRefundId,
    providerProductId,
    productMatch: productMatch && storeMatches,
    amountMinor:
      eventType === "refund" || amount === null
        ? null
        : Math.round(amount * 100),
    currency: stringValue(data.currency),
    refundAmountMinor:
      eventType === "refund" && refundAmount !== null
        ? Math.round(refundAmount * 100)
        : null,
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
  return Math.abs(Date.now() - parsed) > 45 * 60_000;
}

function eventEnvironment(
  mode: string | undefined,
  fallback: BridgeConfig["environment"],
): BridgeConfig["environment"] {
  if (mode === "test") return "test";
  if (mode === "prod" || mode === "production") return "production";
  return fallback;
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

function breakdownTotal(value: unknown): number | null {
  if (!value || typeof value !== "object") return null;
  return numberValue((value as { total?: unknown }).total);
}

function objectValue(value: unknown): Record<string, unknown> | null {
  return value && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function headerValue(value: string | string[] | undefined): string | null {
  if (Array.isArray(value)) return value[0] ?? null;
  return value?.trim() || null;
}

export function sha256(value: string): string {
  return createHash("sha256").update(value, "utf8").digest("hex");
}
