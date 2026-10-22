export const CATALOG = Object.freeze([
  { sku: "kb_pack_9", name: "识海 400 点", amount: "9.00" },
  { sku: "kb_pack_29", name: "识海 1,400 点", amount: "29.00" },
  { sku: "kb_pack_99", name: "识海 5,000 点", amount: "99.00" },
]);

export function normalizePrivateKeyInput(value) {
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

export async function synchronizeCatalog({ client, storeId, publish = false, log = console.log }) {
  const products = await listProducts(client, storeId);

  for (const item of CATALOG) {
    let product = products.find((candidate) => metadataSku(candidate.metadata) === item.sku);
    let action = "existing";
    if (!product) {
      const created = await client.onetimeProducts.create(
        {
          storeId,
          name: item.name,
          prices: {
            CNY: {
              amount: item.amount,
              taxCategory: "digital_goods",
            },
          },
          metadata: { sku: item.sku },
        },
        { idempotencyKey: `zhishiku-catalog-${item.sku}` },
      );
      product = created.product;
      action = "created";
    } else if (product.name !== item.name) {
      const updated = await client.onetimeProducts.update(
        { id: product.id, name: item.name },
        { idempotencyKey: `zhishiku-rename-${item.sku}` },
      );
      product = updated.product;
      action = "renamed";
    }
    if (publish) {
      await client.onetimeProducts.publish(
        { id: product.id },
        { idempotencyKey: `zhishiku-publish-${item.sku}` },
      );
      action = action === "existing" ? "published" : `${action}-published`;
    }
    log(`${item.sku}=${product.id} (${action})`);
  }
}

async function listProducts(client, storeId) {
  const result = await client.graphql.query({
    query: `query ($storeId: String!) {
      onetimeProducts(storeId: $storeId) {
        id
        name
        metadata
      }
    }`,
    variables: { storeId },
  });
  return result.data?.onetimeProducts ?? [];
}

function metadataSku(metadata) {
  if (!metadata) return null;
  if (typeof metadata === "string") {
    try {
      return JSON.parse(metadata).sku ?? null;
    } catch {
      return null;
    }
  }
  return typeof metadata.sku === "string" ? metadata.sku : null;
}
