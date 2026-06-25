export interface CraftlessAutomationClient {
  launch(input: {
    name: string;
    id?: string;
    version: string;
    loader?: string;
    offline?: boolean;
  }): Promise<unknown>;
}

export interface CraftlessPlayer {
  waitForChat(pattern: RegExp | string): Promise<unknown>;
}

export interface CraftlessFixtureOptions<TClient extends CraftlessAutomationClient> {
  client: TClient;
}

export type FixtureUse<T> = (value: T) => Promise<void>;

export function createCraftlessFixture<TClient extends CraftlessAutomationClient>(
  options: CraftlessFixtureOptions<TClient>,
) {
  const client = options.client;
  return async function craftlessFixture(
    _args: Record<string, unknown>,
    use: FixtureUse<TClient>,
  ): Promise<void> {
    await use(client);
  };
}

export async function toHaveChat(
  player: Pick<CraftlessPlayer, "waitForChat">,
  pattern: RegExp | string,
): Promise<{ pass: boolean; message: string }> {
  await player.waitForChat(pattern);
  return {
    pass: true,
    message: `received chat matching ${pattern}`,
  };
}
