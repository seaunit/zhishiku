import { describe, expect, it, vi } from "vitest";
import {
  CATALOG,
  normalizePrivateKeyInput,
  synchronizeCatalog,
} from "./catalog-sync.mjs";

describe("Waffo catalog synchronization", () => {
  it("uses Chinese product names for all Zhishiku packs", () => {
    expect(CATALOG.map((item) => item.name)).toEqual([
      "识海 400 点",
      "识海 1,400 点",
      "识海 5,000 点",
    ]);
  });

  it("renames existing products and publishes the updated versions", async () => {
    const update = vi.fn(async ({ id, name }) => ({ product: { id, name } }));
    const publish = vi.fn(async ({ id }) => ({ product: { id } }));
    const client = {
      graphql: {
        query: vi.fn().mockResolvedValue({
          data: {
            onetimeProducts: [
              { id: "PROD_9", name: "Shihai 400 AI credits", metadata: { sku: "kb_pack_9" } },
              {
                id: "PROD_29",
                name: "Shihai 1,400 AI credits",
                metadata: { sku: "kb_pack_29" },
              },
              {
                id: "PROD_99",
                name: "Shihai 5,000 AI credits",
                metadata: { sku: "kb_pack_99" },
              },
            ],
          },
        }),
      },
      onetimeProducts: {
        create: vi.fn(),
        update,
        publish,
      },
    };

    await synchronizeCatalog({
      client,
      storeId: "STO_1234567890123456789012",
      publish: true,
      log: () => {},
    });

    expect(update).toHaveBeenCalledTimes(3);
    expect(update).toHaveBeenCalledWith(
      { id: "PROD_9", name: "识海 400 点" },
      { idempotencyKey: "zhishiku-rename-kb_pack_9" },
    );
    expect(publish).toHaveBeenCalledTimes(3);
  });

  it("accepts SDK-compatible bare PKCS#8 base64 without exposing the key", () => {
    const bareDer = Buffer.from("pkcs8-der-placeholder").toString("base64");

    expect(normalizePrivateKeyInput(bareDer)).toBe(bareDer);
  });

  it("decodes base64-wrapped PEM input", () => {
    const pem = "-----BEGIN PRIVATE KEY-----\nkey\n-----END PRIVATE KEY-----";
    const wrapped = Buffer.from(pem).toString("base64");

    expect(normalizePrivateKeyInput(wrapped)).toBe(pem);
  });
});
