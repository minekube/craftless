# Representative Older Product Lane Design

## Problem

CL-03 proves the latest/current packaged product lane for Minecraft `26.2`.
CL-04 requires the representative older lane to pass the same public product
gate set. Existing older evidence proves packaging, manifest selection, and
some older diagnostic smoke, but it is not equivalent to CL-03 because the
packaged older lane has not run a single product probe that captures connect,
generated OpenAPI, projections, SSE, JSON-RPC query/subscription, JSON-RPC
invocation, and adaptive CLI invocation artifacts from a real local server.

## Representative Version

Use Minecraft `1.20.6` as the representative older lane because it is already
the packaged older Fabric lane in the distribution manifest, uses a different
Java major requirement (`21`) from current `26.2`, uses the remapped Yarn-based
driver path rather than the official 26.x path, and has prior diagnostic smoke
evidence. Passing this lane proves the shared packaging/cache/launch/API path
is not current-version-only.

## Goal

Add a packaged representative older probe that:

- builds the packaged CLI/Docker staging directory;
- creates a Fabric client for Minecraft `1.20.6` through the packaged CLI and
  supervisor API;
- attaches the packaged `mods/fabric-1.20.6/craftless-driver-fabric.jar`;
- connects to a real local Minecraft `1.20.6` server;
- captures the same generated OpenAPI/projection/SSE/JSON-RPC artifacts as
  CL-03;
- selects an available generated no-argument operation from
  `x-craftless-actions`;
- invokes it through public JSON-RPC `method=invoke`;
- invokes it through adaptive packaged CLI `clients <id> run <action>`.

## Non-Goals

- Do not add new static gameplay operations, static CLI gameplay commands, or
  scenario shortcuts.
- Do not mark CL-05, CL-06, CL-07, or CL-08 complete.
- Do not use server-provisioned inventory or final survival gameplay shortcuts.
- Do not bypass packaged distribution artifacts by launching Gradle
  `:driver-fabric:runClient` directly.

## Design

Create `scripts/packaged-representative-older-probe.sh` as a product-surface
counterpart to the latest/current probe. The script starts
`build/docker/craftless/bin/craftless server start`, creates client
`representative-older` with `--version 1.20.6 --loader fabric --loader-version
0.19.3`, waits for `client.attached`, connects to the local server provided by
`LocalMinecraftServerSmoke`, waits for generated connected OpenAPI resources,
captures the public projection and JSON-RPC artifacts, selects an available
generated action from `x-craftless-actions`, and invokes it through both
JSON-RPC and CLI.

Add a mise task `packaged-representative-older-probe` that runs
`mise run package-cli`, then runs `:driver-fabric:fabricClientSmoke` with
`CRAFTLESS_SMOKE_MINECRAFT_VERSION=1.20.6`,
`CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT=build/craftless-packaged-representative-older-probe`,
and the packaged older probe as the smoke action command.

Add a distribution guard so the task and script cannot regress into static
gameplay shortcuts or direct Gradle driver launches.

## Acceptance

- The distribution guard fails before the script/task exist.
- `mise exec -- bun test playwright/src/distribution.test.ts` passes after the
  script/task exist.
- `mise run packaged-representative-older-probe` exits `0`.
- The probe writes:
  - `clients-create-representative-older.log`;
  - `client-openapi-connected.json`;
  - `client-actions.json`;
  - `client-resources.json`;
  - `client-events-stream.sse`;
  - `client-rpc-openapi.json`;
  - `client-rpc-actions.json`;
  - `client-rpc-resources.json`;
  - `client-rpc-subscribe.json`;
  - `client-events-subscription-stream.sse`;
  - `client-generated-action-selected.json`;
  - `client-rpc-invoke-generated.json`;
  - `client-cli-invoke-generated.log`;
  - `packaged-probe-summary.json`.
- JSON-RPC and CLI invocation transcripts show the selected generated action
  returning `ACCEPTED`, or the evidence records an explicit generated
  unsupported result as the next blocker.
