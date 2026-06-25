import { expect, test } from "bun:test";
import { createCraftwrightFixture, toHaveChat } from "./index";

test("fixture provides the SDK client without shelling out to CLI output", async () => {
  let usedMc: unknown;
  const sdk = {
    launch: async () => ({ id: "alice" }),
  };
  const fixture = createCraftwrightFixture({ sdk });

  await fixture({}, async (mc) => {
    usedMc = mc;
    await mc.launch({ name: "Alice", version: "1.21.4", offline: true });
  });

  expect(usedMc).toBe(sdk);
});

test("chat matcher delegates to the SDK waitForChat method", async () => {
  const calls: RegExp[] = [];
  const player = {
    waitForChat: async (pattern: RegExp) => {
      calls.push(pattern);
      return { type: "chat", message: "Welcome Alice" };
    },
  };

  await expect(toHaveChat(player, /Welcome/)).resolves.toEqual({
    pass: true,
    message: "received chat matching /Welcome/",
  });
  expect(calls).toEqual([/Welcome/]);
});
