import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import { randomUUID, timingSafeEqual } from "node:crypto";
import type { BridgeConfig } from "./config.js";
import { loadConfig } from "./config.js";
import { normalizeWebhook, sha256 } from "./normalizer.js";
import type {
  CheckoutInput,
  NormalizedEvent,
  NormalizedStatus,
  OrderStatusInput,
  WaffoGateway,
} from "./types.js";
import { SdkWaffoGateway } from "./waffo-gateway.js";

const MAX_BODY_BYTES = 1_048_576;
const PACK_AMOUNTS: Record<string, number> = {
  kb_pack_9: 900,
  kb_pack_29: 2900,
  kb_pack_99: 9900,
};

export interface BridgeDependencies {
  config: BridgeConfig;
  gateway: WaffoGateway;
  fetchImpl?: typeof fetch;
}

export function createBridgeServer(dependencies: BridgeDependencies) {
  const { config, gateway } = dependencies;
  const fetchImpl = dependencies.fetchImpl ?? fetch;
  return createServer(async (request, response) => {
    try {
      const url = new URL(request.url ?? "/", "http://127.0.0.1");
      if (request.method === "GET" && url.pathname === "/health") {
        return json(response, 200, {
          ok: true,
          configured: gateway.configured() || config.dryRun,
          environment: config.environment,
          dryRun: config.dryRun,
        });
      }
      if (request.method === "POST" && url.pathname === "/checkout") {
        requireInternalSecret(request, config.appToBridgeSecret);
        const body = await readJson<CheckoutInput>(request);
        validateCheckout(body, config, request.headers["idempotency-key"]);
        const productId = config.productMap[body.sku]!;
        const checkout = config.dryRun
          ? {
              sessionId: `dry_${randomUUID()}`,
              checkoutUrl: `${body.successUrl}?dryRun=1&orderId=${encodeURIComponent(body.orderId)}`,
              expiresAt: new Date(Date.now() + 5 * 60_000).toISOString(),
            }
          : await gateway.createCheckout(body, productId);
        return json(response, 200, {
          ...checkout,
          environment: config.environment,
          productId,
        });
      }
      if (request.method === "POST" && url.pathname === "/orders/status") {
        requireInternalSecret(request, config.appToBridgeSecret);
        const body = await readJson<OrderStatusInput>(request);
        if (!body.orderId || !body.providerSessionId) {
          return error(response, 400, "INVALID_ORDER_QUERY", "Order and session are required.");
        }
        const result = config.dryRun
          ? {
              status: "pending" as const,
              amountMinor: null,
              currency: null,
              productId: null,
            }
          : await gateway.queryOrder(body);
        const normalized: NormalizedStatus = {
          environment: config.environment,
          ...result,
          productMatch:
            result.productId !== null &&
            Object.values(config.productMap).includes(result.productId),
          checkedAt: new Date().toISOString(),
          payloadHash: sha256(JSON.stringify(result)),
        };
        return json(response, 200, normalized);
      }
      if (request.method === "POST" && url.pathname === "/webhooks/waffo") {
        const rawBody = await readRawBody(request);
        const signature = String(request.headers["x-waffo-signature"] ?? "");
        let verified: unknown;
        try {
          verified = config.dryRun
            ? JSON.parse(rawBody)
            : gateway.verifyWebhook(rawBody, signature);
        } catch {
          return error(response, 401, "INVALID_SIGNATURE", "Webhook signature verification failed.");
        }
        const event = normalizeWebhook(rawBody, verified, config, request.headers);
        const forwarded = await forwardEvent(fetchImpl, config, event);
        if (!forwarded) {
          return error(response, 500, "EVENT_FORWARD_FAILED", "Event forwarding failed.");
        }
        return json(response, 200, { received: true });
      }
      return error(response, 404, "NOT_FOUND", "Bridge route was not found.");
    } catch (caught) {
      const status = caught instanceof BridgeHttpError ? caught.status : 500;
      const code = caught instanceof BridgeHttpError ? caught.code : "BRIDGE_INTERNAL_ERROR";
      const message =
        caught instanceof BridgeHttpError
          ? caught.message
          : "Waffo bridge request could not be completed.";
      return error(response, status, code, message);
    }
  });
}

async function forwardEvent(
  fetchImpl: typeof fetch,
  config: BridgeConfig,
  event: NormalizedEvent,
): Promise<boolean> {
  for (let attempt = 0; attempt < 3; attempt += 1) {
    try {
      const response = await fetchImpl(config.internalEventUrl, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Internal-Secret": config.bridgeToAppSecret,
        },
        body: JSON.stringify(event),
        signal: AbortSignal.timeout(8_000),
      });
      if (response.ok || response.status === 409) return true;
      if (response.status < 500) return false;
    } catch {
      // Retry only temporary transport and server failures.
    }
    await new Promise((resolve) => setTimeout(resolve, 250 * 2 ** attempt));
  }
  return false;
}

function validateCheckout(
  input: CheckoutInput,
  config: BridgeConfig,
  idempotencyKey: string | string[] | undefined,
) {
  if (!input.orderId || !input.sku || !input.buyerIdentity) {
    throw new BridgeHttpError(400, "CHECKOUT_INVALID", "Checkout fields are required.");
  }
  if (Array.isArray(idempotencyKey) || idempotencyKey !== input.orderId) {
    throw new BridgeHttpError(
      400,
      "CHECKOUT_IDEMPOTENCY_INVALID",
      "Idempotency-Key must equal orderId.",
    );
  }
  if (input.currency !== config.currency || PACK_AMOUNTS[input.sku] !== input.amountMinor) {
    throw new BridgeHttpError(
      409,
      "CHECKOUT_PRODUCT_MISMATCH",
      "Checkout product or amount does not match the zhishiku catalog.",
    );
  }
  if (!config.productMap[input.sku]) {
    throw new BridgeHttpError(409, "CHECKOUT_PRODUCT_MISSING", "Waffo product is not mapped.");
  }
  if (!allowedSuccessUrl(input.successUrl, config.successUrlAllowlist)) {
    throw new BridgeHttpError(400, "CHECKOUT_SUCCESS_URL_INVALID", "Success URL is not allowed.");
  }
}

function allowedSuccessUrl(value: string, allowlist: string[]): boolean {
  try {
    const url = new URL(value);
    if (!["http:", "https:"].includes(url.protocol)) return false;
    if (allowlist.length === 0) {
      return url.hostname === "127.0.0.1" || url.hostname === "seaunit.site";
    }
    return allowlist.some((prefix) => value.startsWith(prefix));
  } catch {
    return false;
  }
}

function requireInternalSecret(request: IncomingMessage, expected: string) {
  const value = request.headers["x-internal-secret"];
  const actual = Array.isArray(value) ? value[0] : value;
  if (!actual || !safeEqual(actual, expected)) {
    throw new BridgeHttpError(401, "INTERNAL_AUTH_REQUIRED", "Internal authentication failed.");
  }
}

function safeEqual(left: string, right: string): boolean {
  const a = Buffer.from(left);
  const b = Buffer.from(right);
  return a.length === b.length && timingSafeEqual(a, b);
}

async function readJson<T>(request: IncomingMessage): Promise<T> {
  const raw = await readRawBody(request);
  try {
    return JSON.parse(raw) as T;
  } catch {
    throw new BridgeHttpError(400, "JSON_INVALID", "Request body is not valid JSON.");
  }
}

async function readRawBody(request: IncomingMessage): Promise<string> {
  const chunks: Buffer[] = [];
  let total = 0;
  for await (const chunk of request) {
    const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
    total += buffer.length;
    if (total > MAX_BODY_BYTES) {
      throw new BridgeHttpError(413, "BODY_TOO_LARGE", "Request body is too large.");
    }
    chunks.push(buffer);
  }
  return Buffer.concat(chunks).toString("utf8");
}

function json(response: ServerResponse, status: number, body: unknown) {
  const payload = JSON.stringify(body);
  response.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(payload),
    "Cache-Control": "no-store",
  });
  response.end(payload);
}

function error(response: ServerResponse, status: number, code: string, message: string) {
  return json(response, status, { code, message });
}

class BridgeHttpError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

if (process.argv[1]?.endsWith("server.ts") || process.argv[1]?.endsWith("server.js")) {
  const config = loadConfig(process.env);
  const server = createBridgeServer({
    config,
    gateway: new SdkWaffoGateway(config),
  });
  server.listen(config.port, "127.0.0.1", () => {
    console.log(
      `[waffo-bridge] listening on 127.0.0.1:${config.port} environment=${config.environment} dryRun=${config.dryRun}`,
    );
  });
}
