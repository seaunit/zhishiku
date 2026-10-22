import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const packageRoot = fileURLToPath(new URL("..", import.meta.url));

describe("operator scripts", () => {
  it.each([
    ["scripts/smoke-checkout.mjs", "requires --confirm"],
    ["scripts/setup-catalog.mjs", "requires --apply"],
  ])("%s fails closed without an explicit confirmation flag", (script, message) => {
    const result = spawnSync(process.execPath, [script], {
      cwd: packageRoot,
      encoding: "utf8",
      env: {
        PATH: process.env.PATH,
      },
    });

    expect(result.status).toBe(2);
    expect(result.stderr).toContain(message);
  });
});
