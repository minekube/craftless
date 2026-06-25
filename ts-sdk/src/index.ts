export type FetchLike = (url: string | URL | Request, init?: RequestInit) => Promise<Response>;

export interface CraftwrightOptions {
  baseUrl?: string;
  fetch?: FetchLike;
}

export interface LaunchOptions {
  name: string;
  id?: string;
  version: string;
  loader?: "FABRIC" | "VANILLA" | "NEOFORGE" | "FORGE" | "QUILT";
  offline?: boolean;
}

export interface ChatEvent {
  type: string;
  message?: string;
  client?: string;
  time?: string;
}

export interface WaitForChatOptions {
  timeoutMs?: number;
  intervalMs?: number;
}

export interface Craftwright {
  version(): Promise<unknown>;
  openapi(): Promise<unknown>;
  launch(options: LaunchOptions | string): Promise<CraftwrightPlayer>;
  client(id: string): CraftwrightPlayer;
}

export interface CraftwrightPlayer {
  id: string;
  connect(host: string, port: number): Promise<unknown>;
  chat(message: string): Promise<unknown>;
  command(command: string): Promise<unknown>;
  waitForChat(pattern: RegExp | string, options?: WaitForChatOptions): Promise<ChatEvent>;
  player(): Promise<unknown>;
  stop(): Promise<unknown>;
}

export function createCraftwright(options: CraftwrightOptions = {}): Craftwright {
  const transport = new HttpTransport(
    options.baseUrl ?? "http://127.0.0.1:25565",
    options.fetch ?? globalThis.fetch.bind(globalThis),
  );

  return {
    version: () => transport.get("/version"),
    openapi: () => transport.get("/openapi.json"),
    launch: async (input) => {
      const launch = normalizeLaunchOptions(input);
      await transport.post("/clients", {
        id: launch.id ?? clientIdFromName(launch.name),
        version: launch.version,
        loader: launch.loader ?? "FABRIC",
        profile: launch.offline === false
          ? { kind: "AUTHENTICATED", name: launch.name }
          : { kind: "OFFLINE", name: launch.name },
      });
      return createPlayer(transport, launch.id ?? clientIdFromName(launch.name));
    },
    client: (id) => createPlayer(transport, id),
  };
}

export async function start(options: CraftwrightOptions = {}): Promise<Craftwright> {
  return createCraftwright(options);
}

function createPlayer(transport: HttpTransport, id: string): CraftwrightPlayer {
  const base = `/clients/${encodeURIComponent(id)}`;
  return {
    id,
    connect: (host, port) => transport.post(`${base}/connection/connect`, { host, port }),
    chat: (message) => transport.post(`${base}/player/sendChat`, { message }),
    command: (command) => transport.post(`${base}/player/sendChat`, { message: command }),
    waitForChat: (pattern, options) => waitForChat(transport, `${base}/events`, pattern, options),
    player: () => transport.get(`${base}/player`),
    stop: () => transport.post(`${base}/stop`, {}),
  };
}

async function waitForChat(
  transport: HttpTransport,
  path: string,
  pattern: RegExp | string,
  options: WaitForChatOptions = {},
): Promise<ChatEvent> {
  const timeoutMs = options.timeoutMs ?? 10_000;
  const intervalMs = options.intervalMs ?? 100;
  const deadline = Date.now() + timeoutMs;

  while (Date.now() <= deadline) {
    const events = await transport.get<ChatEvent[]>(path);
    const match = events.find((event) => matchesChat(event, pattern));
    if (match) return match;
    await sleep(intervalMs);
  }

  throw new Error(`timed out waiting for chat matching ${pattern}`);
}

function matchesChat(event: ChatEvent, pattern: RegExp | string): boolean {
  if (!event.message) return false;
  return typeof pattern === "string" ? event.message.includes(pattern) : pattern.test(event.message);
}

function normalizeLaunchOptions(input: LaunchOptions | string): LaunchOptions {
  if (typeof input === "string") {
    return { name: input, version: "latest", offline: true };
  }
  return input;
}

function clientIdFromName(name: string): string {
  return name.trim().toLowerCase();
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

class HttpTransport {
  private readonly baseUrl: string;
  private readonly fetch: FetchLike;

  constructor(baseUrl: string, fetch: FetchLike) {
    this.baseUrl = baseUrl.replace(/\/+$/, "");
    this.fetch = fetch;
  }

  get<T = unknown>(path: string): Promise<T> {
    return this.request<T>(path, { method: "GET" });
  }

  post<T = unknown>(path: string, body: unknown): Promise<T> {
    return this.request<T>(path, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(body),
    });
  }

  private async request<T>(path: string, init: RequestInit): Promise<T> {
    const response = await this.fetch(`${this.baseUrl}${path}`, init);
    const text = await response.text();
    if (!response.ok) {
      throw new Error(text || `Craftwright request failed with HTTP ${response.status}`);
    }
    return text ? JSON.parse(text) as T : undefined as T;
  }
}
