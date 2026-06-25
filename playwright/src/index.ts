import { createCraftwright, type Craftwright, type CraftwrightPlayer } from "../../ts-sdk/src/index";

export interface CraftwrightFixtureOptions {
  sdk?: Craftwright;
}

export type FixtureUse<T> = (value: T) => Promise<void>;

export function createCraftwrightFixture(options: CraftwrightFixtureOptions = {}) {
  const sdk = options.sdk ?? createCraftwright();
  return async function craftwrightFixture(
    _args: Record<string, unknown>,
    use: FixtureUse<Craftwright>,
  ): Promise<void> {
    await use(sdk);
  };
}

export async function toHaveChat(
  player: Pick<CraftwrightPlayer, "waitForChat">,
  pattern: RegExp | string,
): Promise<{ pass: boolean; message: string }> {
  await player.waitForChat(pattern);
  return {
    pass: true,
    message: `received chat matching ${pattern}`,
  };
}

export { createCraftwright };
export type { Craftwright, CraftwrightPlayer };
