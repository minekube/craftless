# Packaged Live Attach And Cold-Cache Usability Design

## Problem

After Phase 101, the packaged Fabric driver mod carried its nested runtime
jars, but the end-to-end packaged path still needed live proof. The first
packaged smoke exposed a separate usability blocker: `clients create` performs
real first-run cache preparation and launch work inside one supervisor request,
and the CLI's default Ktor request timeout expired while Minecraft assets were
still downloading.

The same smoke also showed that asset objects were fetched sequentially even
though they are independent immutable objects.

## Goals

- Keep CLI API calls on Ktor and give real client creation enough request
  budget for cold-cache launch work.
- Allow users and CI to override that timeout through
  `CRAFTLESS_HTTP_REQUEST_TIMEOUT_MS`.
- Fetch independent Minecraft asset objects with bounded parallelism while
  preserving checksum validation, corrupt-cache refetch, and resume behavior.
- Prove the packaged CLI/server/client path with a real Fabric client:
  launch, self-attach, generated OpenAPI/action/resource projections, SSE
  events, and clean stop.

## Non-Goals

- Do not add public gameplay actions, static descriptors, route families, CLI
  gameplay catalogs, Fabric bindings, or scenario shortcuts.
- Do not use OkHttp, Java `HttpClient`, `com.sun.net.httpserver`, npm, node, or
  non-mise dependency flows.
- Do not claim final survival gameplay or expanded version support from this
  smoke.

## Acceptance Criteria

- Focused CLI tests prove API request timeout configuration is honored.
- Focused daemon tests prove independent asset object downloads overlap.
- `mise run package-cli` succeeds after the changes.
- Packaged `craftless server start` plus packaged `craftless clients create`
  launches a real Fabric `1.21.6` client with the staged driver mod.
- The real in-client Fabric driver posts `client.attached` to the packaged
  supervisor.
- Packaged CLI can read generated actions/resources and SSE lifecycle events
  from the attached client.
- Evidence records the timeout failure, the fix, local tests, package smoke,
  live attach smoke, and cleanup.
