# Packaged Latest Current Attach Artifacts Design

## Problem

Phase 181 packages the official Minecraft `26.2` lane into the CLI/Docker
distribution. CL-03 still remains open because no packaged product run has
created a `latest-release` client, attached the packaged official driver,
connected to a real Minecraft server, and captured the live generated API and
streaming artifacts.

The existing `driver-fabric-official:officialFabricAttachProbe` proves the
official module in a diagnostic Gradle harness. It does not prove the packaged
CLI/Docker distribution path because it starts an in-memory daemon and launches
`:driver-fabric-official:runClient` directly.

## Goal

Add a rerunnable packaged latest-current probe that:

- starts from the packaged `build/docker/craftless/bin/craftless` CLI;
- uses the packaged `driver-mods.json` and official `26.2` jar;
- requests `version=latest-release` through the supervisor API;
- connects the attached client to a real local Minecraft server;
- captures generated per-client OpenAPI, actions, resources, SSE, JSON-RPC
  query, and JSON-RPC subscription artifacts.

## Non-Goals

- Do not add public static gameplay actions, static CLI gameplay commands,
  route families, or scenario shortcuts.
- Do not mark CL-03 complete unless the packaged run actually attaches and
  captures the connected artifacts.
- Do not replace the official-lane diagnostic probe; this is product-surface
  evidence, not module-only evidence.
- Do not add server-provisioned inventory or final survival gameplay logic.

## Design

Create `scripts/packaged-latest-current-probe.sh` as a small Bash orchestrator
that is run by `LocalMinecraftServerSmoke` as its action command. The smoke
fixture owns the real Minecraft server and injects:

- `CRAFTLESS_SMOKE_SERVER_PORT`;
- `CRAFTLESS_SMOKE_ARTIFACTS_DIR`;
- `CRAFTLESS_SMOKE_JAVA_EXECUTABLE`.

The script should:

1. Start the packaged daemon from `build/docker/craftless/bin/craftless server
   start` with an isolated workspace.
2. Create a Fabric client through the packaged CLI with `--version
   latest-release`.
3. Poll the supervisor until `client.attached` is visible.
4. Connect the client to `127.0.0.1:$CRAFTLESS_SMOKE_SERVER_PORT`.
5. Poll generated OpenAPI until connected client-state resources are visible.
6. Capture:
   - `supervisor-openapi.json`;
   - `client-openapi-connected.json`;
   - `client-actions.json`;
   - `client-resources.json`;
   - `client-events-stream.sse`;
   - `client-rpc-openapi.json`;
   - `client-rpc-actions.json`;
   - `client-rpc-resources.json`;
   - `client-rpc-subscribe.json`;
   - `client-events-subscription-stream.sse`;
   - `client-rpc-subscriptions.json`;
   - `client-rpc-unsubscribe.json`;
   - `client-rpc-subscriptions-after-unsubscribe.json`;
   - `clients-create-latest-release.log`;
   - `clients-connect-latest-release.log`;
   - `client-stop.log`;
   - `packaged-driver-mods.json`;
   - `packaged-probe-summary.json`.
7. Stop the client and packaged daemon in cleanup.

Use `mise exec -- bun` for HTTP/JSON helpers inside the script so the probe
does not depend on local Node, npm, jq, Python, or global JavaScript tooling.

Add a mise task `packaged-latest-current-probe` that:

- runs `mise run package-cli`;
- runs `LocalMinecraftServerSmoke` through Gradle with
  `CRAFTLESS_LOCAL_SERVER_SMOKE=1`;
- passes `scripts/packaged-latest-current-probe.sh` as the smoke action
  command;
- records artifacts under
  `build/craftless-packaged-latest-current-probe/artifacts`.

## Acceptance

- A failing Bun distribution guard proves the new script/task are required.
- The script uses `build/docker/craftless/bin/craftless` and
  `version=latest-release`.
- The script uses `mise exec -- bun` for HTTP/JSON capture helpers.
- The mise task exists and runs `mise run package-cli` before the live probe.
- `mise run packaged-latest-current-probe` either:
  - exits `0` and writes connected OpenAPI/actions/resources/SSE/JSON-RPC
    artifacts; or
  - exits non-zero with artifacts that identify the exact packaged blocker.
- If the run succeeds, CL-03c.2, CL-03d, and CL-03e.3 can be marked complete
  with evidence. CL-03f still remains open until public API/CLI gameplay smoke
  executes generated primitives.
