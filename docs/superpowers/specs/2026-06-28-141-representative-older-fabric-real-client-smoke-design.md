# Representative Older Fabric Real-Client Smoke Design

## Problem

The representative older Fabric lane now compiles, is packaged, can be selected
from the manifest, and the smoke harness preserves its Gradle lane properties.
Craftless still needs current Codex-verifiable evidence that the older lane can
actually launch a real client, attach the driver, expose generated APIs, stream
events, and invoke generated actions against a real local server.

## Decision

Run the existing opt-in `fabricClientSmoke` against Minecraft `1.20.6` with the
parameterized Fabric lane properties:

- Yarn mappings `1.20.6+build.3`;
- Fabric Loader `0.19.3`;
- Fabric API `0.100.8+1.20.6`;
- Java major version `21`;
- lane/provider id `fabric-1-20-6-lane`;
- mappings fingerprint `craftless-fabric-bindings-1-20-6`.

Record command output and artifact summaries in repository evidence. The smoke
must show server join/chat/disconnect evidence, generated OpenAPI/actions and
resources, SSE events, runtime metadata for the older mappings fingerprint,
and generated action results.

## Non-Goals

- Do not claim final honest survival gameplay completion.
- Do not claim installed packaged CLI older-lane operation from this Gradle
  smoke alone.
- Do not add public gameplay actions, static route families, or scenario
  shortcuts.
- Do not require a human Minecraft chat confirmation.

## Verification

- `fabricClientSmoke` must exit successfully for Minecraft `1.20.6`.
- Artifacts must include `runtime-lane.json`, `runtime-metadata.json`,
  `client-openapi-connected.json`, `client-actions-connected.json`,
  `client-resources-connected.json`, `client-events-stream.sse`,
  `client-events.jsonl`, `gameplay-results.jsonl`, and `server-evidence.jsonl`.
- Evidence must show Minecraft `1.20.6`, Fabric Loader `0.19.3`, Java `21`,
  mappings `craftless-fabric-bindings-1-20-6`, generated API resources/actions,
  SSE events, chat, and disconnect.
- The evidence must explicitly say the smoke provisioned an iron sword and is
  diagnostic only.
