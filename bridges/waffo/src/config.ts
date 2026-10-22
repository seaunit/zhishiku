export type BridgeEnvironment = "test" | "production";

export interface BridgeConfig {
  port: number;
  environment: BridgeEnvironment;
  merchantId: string;
  privateKey: string;
  productMap: Record<string, string>;
  currency: "CNY";
  dryRun: boolean;
  appToBridgeSecret: string;
  bridgeToAppSecret: string;
  internalEventUrl: string;
  successUrlAllowlist: string[];
}

export function loadConfig(env: NodeJS.ProcessEnv): BridgeConfig {
  const environment = parseEnvironment(env.WAFFO_ENV);
  const productMap = parseProductMap(env.WAFFO_PRODUCT_MAP ?? "");
  const appToBridgeSecret = required(env.APP_TO_BRIDGE_SECRET, "APP_TO_BRIDGE_SECRET");
  const bridgeToAppSecret = required(env.BRIDGE_TO_APP_SECRET, "BRIDGE_TO_APP_SECRET");
  if (appToBridgeSecret === bridgeToAppSecret) {
    throw new Error("APP_TO_BRIDGE_SECRET and BRIDGE_TO_APP_SECRET must be different.");
  }
  const internalEventUrl = required(env.INTERNAL_EVENT_URL, "INTERNAL_EVENT_URL");
  return {
    port: Number(env.WAFFO_BRIDGE_PORT ?? 8091),
    environment,
    merchantId: env.WAFFO_MERCHANT_ID?.trim() ?? "",
    privateKey: env.WAFFO_PRIVATE_KEY_BASE64?.trim() ?? "",
    productMap,
    currency: "CNY",
    dryRun: env.WAFFO_DRY_RUN === "true",
    appToBridgeSecret,
    bridgeToAppSecret,
    internalEventUrl,
    successUrlAllowlist: (env.WAFFO_SUCCESS_URL_ALLOWLIST ?? "")
      .split(",")
      .map((value) => value.trim())
      .filter(Boolean),
  };
}

function parseEnvironment(value: string | undefined): BridgeEnvironment {
  if (value === "test") return "test";
  if (value === "production" || value === "prod") return "production";
  throw new Error("WAFFO_ENV must be test or production.");
}

function parseProductMap(value: string): Record<string, string> {
  const entries = value
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean)
    .map((entry) => {
      const [sku, productId, extra] = entry.split("=");
      if (!sku || !productId || extra !== undefined) {
        throw new Error("WAFFO_PRODUCT_MAP must use sku=productId entries.");
      }
      if (!["kb_pack_9", "kb_pack_29", "kb_pack_99"].includes(sku)) {
        throw new Error(`Unsupported Waffo product SKU: ${sku}`);
      }
      return [sku, productId] as const;
    });
  const map = Object.fromEntries(entries);
  if (Object.keys(map).length !== 3) {
    throw new Error("WAFFO_PRODUCT_MAP must contain all three zhishiku SKUs.");
  }
  return map;
}

function required(value: string | undefined, name: string): string {
  const result = value?.trim() ?? "";
  if (!result) {
    throw new Error(`${name} is required.`);
  }
  return result;
}
