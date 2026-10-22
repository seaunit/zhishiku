import { describe, expect, it } from "vitest";
import {
  REQUIRED_EVENTS,
  normalizeWebhookUrl,
  selectDedicatedWebhook,
} from "./configure-test-webhook.mjs";

describe("configure test webhook", () => {
  it("requires an HTTPS callback ending in the bridge webhook path", () => {
    expect(() =>
      normalizeWebhookUrl("http://127.0.0.1:8091/webhooks/waffo"),
    ).toThrow(/HTTPS/);
    expect(() =>
      normalizeWebhookUrl("https://example.com/not-the-webhook"),
    ).toThrow(/webhooks\/waffo/);
    expect(
      normalizeWebhookUrl(
        "https://example.trycloudflare.com/webhooks/waffo",
      ),
    ).toBe("https://example.trycloudflare.com/webhooks/waffo");
  });

  it("selects only the dedicated test environment event set", () => {
    const dedicated = {
      id: "dedicated",
      channel: "http",
      testMode: true,
      events: REQUIRED_EVENTS,
    };
    const unrelated = {
      id: "shuati",
      channel: "http",
      testMode: true,
      events: ["order.completed", "subscription.activated"],
    };
    expect(selectDedicatedWebhook([unrelated, dedicated])).toBe(dedicated);
  });

  it("requires an explicit id when more than one webhook matches", () => {
    const webhook = {
      id: "first",
      channel: "http",
      testMode: true,
      events: REQUIRED_EVENTS,
    };
    expect(() =>
      selectDedicatedWebhook([webhook, { ...webhook, id: "second" }]),
    ).toThrow(/SHIHAI_WAFFO_TEST_WEBHOOK_ID/);
  });
});
