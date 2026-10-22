export type BridgeEnvironment = "test" | "production";

export interface BridgeConfig {
  port: number;
  environment: BridgeEnvironment;
  storeId: string;
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
  const environment = parseEnvironment(
    envValue(env, "SHIHAI_WAFFO_ENV", "WAFFO_ENV"),
  );
  const productMap = parseProductMap(
    envValue(env, "SHIHAI_WAFFO_PRODUCT_MAP", "WAFFO_PRODUCT_MAP") ?? "",
  );
  const dryRun =
    envValue(env, "SHIHAI_WAFFO_DRY_RUN", "WAFFO_DRY_RUN") === "true";
  const merchantId =
    envValue(env, "SHIHAI_WAFFO_MERCHANT_ID", "WAFFO_MERCHANT_ID")?.trim() ?? "";
  const privateKey = normalizePrivateKeyInput(
    envValue(
      env,
      "SHIHAI_WAFFO_PRIVATE_KEY_BASE64",
      "WAFFO_PRIVATE_KEY_BASE64",
    ) ?? "",
  );
  const storeId =
    envValue(env, "SHIHAI_WAFFO_STORE_ID", "WAFFO_STORE_ID")?.trim() ?? "";
  const currency =
    envValue(env, "SHIHAI_WAFFO_CURRENCY", "WAFFO_CURRENCY")?.trim() ??
    "CNY";
  if (currency !== "CNY") {
    throw new Error("SHIHAI_WAFFO_CURRENCY must be CNY.");
  }
  if (!dryRun) {
    required(merchantId, "SHIHAI_WAFFO_MERCHANT_ID");
    required(privateKey, "SHIHAI_WAFFO_PRIVATE_KEY_BASE64");
    required(storeId, "SHIHAI_WAFFO_STORE_ID");
  }
  const appToBridgeSecret = required(
    envValue(env, "SHIHAI_APP_TO_BRIDGE_SECRET", "APP_TO_BRIDGE_SECRET"),
    "SHIHAI_APP_TO_BRIDGE_SECRET",
  );
  const bridgeToAppSecret = required(
    envValue(env, "SHIHAI_BRIDGE_TO_APP_SECRET", "BRIDGE_TO_APP_SECRET"),
    "SHIHAI_BRIDGE_TO_APP_SECRET",
  );
  if (appToBridgeSecret === bridgeToAppSecret) {
    throw new Error(
      "SHIHAI_APP_TO_BRIDGE_SECRET and SHIHAI_BRIDGE_TO_APP_SECRET must be different.",
    );
  }
  const internalEventUrl = required(
    envValue(env, "SHIHAI_INTERNAL_EVENT_URL", "INTERNAL_EVENT_URL"),
    "SHIHAI_INTERNAL_EVENT_URL",
  );
  return {
    port: Number(envValue(env, "SHIHAI_WAFFO_BRIDGE_PORT", "WAFFO_BRIDGE_PORT") ?? 8091),
    environment,
    storeId,
    merchantId,
    privateKey,
    productMap,
    currency,
    dryRun,
    appToBridgeSecret,
    bridgeToAppSecret,
    internalEventUrl,
    successUrlAllowlist: (
      envValue(
        env,
        "SHIHAI_WAFFO_SUCCESS_URL_ALLOWLIST",
        "WAFFO_SUCCESS_URL_ALLOWLIST",
      ) ?? ""
    )
      .split(",")
      .map((value) => value.trim())
      .filter(Boolean),
  };
}

export function normalizePrivateKeyInput(value: string): string {
  const normalized = value.replace(/\\n/g, "\n").replace(/\r\n/g, "\n").trim();
  if (!normalized || normalized.includes("-----BEGIN ")) {
    return normalized;
  }
  const compact = normalized.replace(/\s+/g, "");
  if (!/^[A-Za-z0-9+/]+=*$/.test(compact)) {
    return normalized;
  }
  const decoded = Buffer.from(compact, "base64").toString("utf8").trim();
  if (decoded.includes("-----BEGIN ") && decoded.includes("PRIVATE KEY-----")) {
    return decoded.replace(/\r\n/g, "\n");
  }
  return normalized;
}

function parseEnvironment(value: string | undefined): BridgeEnvironment {
  if (value === "test") return "test";
  if (value === "production" || value === "prod") return "production";
  throw new Error("SHIHAI_WAFFO_ENV must be test or production.");
}

function parseProductMap(value: string): Record<string, string> {
  const entries = value
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean)
    .map((entry) => {
      const [sku, productId, extra] = entry.split("=");
      if (!sku || !productId || extra !== undefined) {
        throw new Error(
          "SHIHAI_WAFFO_PRODUCT_MAP must use sku=productId entries.",
        );
      }
      if (!["kb_pack_9", "kb_pack_29", "kb_pack_99"].includes(sku)) {
        throw new Error(`Unsupported Waffo product SKU: ${sku}`);
      }
      return [sku, productId] as const;
    });
  const map = Object.fromEntries(entries);
  if (Object.keys(map).length !== 3) {
    throw new Error(
      "SHIHAI_WAFFO_PRODUCT_MAP must contain all three zhishiku SKUs.",
    );
  }
  return map;
}

function envValue(
  env: NodeJS.ProcessEnv,
  primary: string,
  legacy: string,
): string | undefined {
  const primaryValue = env[primary];
  const legacyValue = env[legacy];
  const normalizedPrimary = primaryValue?.trim();
  const normalizedLegacy = legacyValue?.trim();
  if (
    normalizedPrimary &&
    normalizedLegacy &&
    normalizedPrimary !== normalizedLegacy
  ) {
    throw new Error(
      `Conflicting Shihai environment variables: ${primary} and legacy ${legacy} must have the same value during migration.`,
    );
  }
  return normalizedPrimary ? primaryValue : legacyValue;
}

function required(value: string | undefined, name: string): string {
  const result = value?.trim() ?? "";
  if (!result) {
    throw new Error(`${name} is required.`);
  }
  return result;
}
