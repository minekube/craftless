import { describe, expect, test } from "bun:test";
import { createCraftwright } from "./index";

function jsonResponse(value: unknown, init: ResponseInit = {}): Response {
  return new Response(JSON.stringify(value), {
    status: init.status ?? 200,
    headers: { "content-type": "application/json", ...(init.headers ?? {}) },
  });
}

describe("Craftwright TypeScript SDK", () => {
  test("launch maps to the daemon client creation route", async () => {
    const requests: Array<{ url: string; init?: RequestInit }> = [];
    const fetch = async (url: string | URL | Request, init?: RequestInit) => {
      requests.push({ url: url.toString(), init });
      return jsonResponse({ id: "alice", state: "RUNNING" }, { status: 201 });
    };

    const mc = createCraftwright({ baseUrl: "http://127.0.0.1:25565", fetch });
    const alice = await mc.launch({ name: "Alice", version: "1.21.4", offline: true });

    expect(alice.id).toBe("alice");
    expect(requests).toHaveLength(1);
    expect(requests[0].url).toBe("http://127.0.0.1:25565/clients");
    expect(requests[0].init?.method).toBe("POST");
    expect(JSON.parse(String(requests[0].init?.body))).toEqual({
      id: "alice",
      version: "1.21.4",
      loader: "FABRIC",
      profile: { kind: "OFFLINE", name: "Alice" },
    });
  });

  test("player methods map to session API routes", async () => {
    const requests: Array<{ url: string; init?: RequestInit }> = [];
    const fetch = async (url: string | URL | Request, init?: RequestInit) => {
      requests.push({ url: url.toString(), init });
      return jsonResponse({ ok: true });
    };

    const mc = createCraftwright({ baseUrl: "http://127.0.0.1:25565/", fetch });
    const alice = mc.client("alice");

    await alice.connect("localhost", 25565);
    await alice.chat("hello");
    await alice.player();
    await alice.stop();

    expect(requests.map((request) => `${request.init?.method ?? "GET"} ${request.url}`)).toEqual([
      "POST http://127.0.0.1:25565/clients/alice/connection/connect",
      "POST http://127.0.0.1:25565/clients/alice/player/sendChat",
      "GET http://127.0.0.1:25565/clients/alice/player",
      "POST http://127.0.0.1:25565/clients/alice/stop",
    ]);
    expect(JSON.parse(String(requests[0].init?.body))).toEqual({ host: "localhost", port: 25565 });
    expect(JSON.parse(String(requests[1].init?.body))).toEqual({ message: "hello" });
  });

  test("authenticated launch maps to the daemon profile kind", async () => {
    let body: unknown;
    const fetch = async (_url: string | URL | Request, init?: RequestInit) => {
      body = JSON.parse(String(init?.body));
      return jsonResponse({ id: "alice", state: "RUNNING" }, { status: 201 });
    };

    const mc = createCraftwright({ baseUrl: "http://127.0.0.1:25565", fetch });
    await mc.launch({ name: "Alice", id: "alice-auth", version: "1.21.4", offline: false });

    expect(body).toEqual({
      id: "alice-auth",
      version: "1.21.4",
      loader: "FABRIC",
      profile: { kind: "AUTHENTICATED", name: "Alice" },
    });
  });

  test("waitForChat polls client events until a message matches", async () => {
    let calls = 0;
    const fetch = async () => {
      calls += 1;
      return jsonResponse(
        calls === 1
          ? [{ type: "chat", message: "not yet" }]
          : [{ type: "chat", message: "Welcome Alice" }],
      );
    };

    const mc = createCraftwright({ baseUrl: "http://127.0.0.1:25565", fetch });
    await expect(mc.client("alice").waitForChat(/Welcome/, { timeoutMs: 250, intervalMs: 1 })).resolves.toEqual({
      type: "chat",
      message: "Welcome Alice",
    });
  });
});
