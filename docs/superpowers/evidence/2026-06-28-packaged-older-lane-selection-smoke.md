# Packaged Older Lane Selection Smoke Evidence

## Scope

Phase 139 proves only that the supervisor create-client path can select the
packaged representative older Fabric lane from a multi-entry driver-mod
manifest and stage that lane's jar into the prepared launch plan.

It does not prove older Minecraft runtime support is complete. Runtime launch,
driver attach, generated OpenAPI, SSE/JSON-RPC behavior, and public API/CLI
gameplay smoke for the older lane remain required completion evidence.

## Red

Command:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.prepared runtime selects packaged older fabric lane from manifest'
```

Result: failed before implementation at `:daemon:compileTestKotlin`.

Relevant failure:

```text
Unresolved reference 'preparedRuntimeMetadataFetcherWithOlderLane'.
```

## Green

Command:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.prepared runtime selects packaged older fabric lane from manifest'
```

Result: passed.

Evidence covered by the test:

- request: `version=1.20.6`, `loader=FABRIC`, `loaderVersion=0.19.3`;
- manifest entries: current `1.21.6` lane and older `1.20.6` lane;
- older runtime metadata: Fabric API `0.100.8+1.20.6`, Java major version 21;
- prepared manifest: `cache/prepared/1.20.6-fabric-0.19.3.json`;
- staged launch mod content includes `older-driver-mod`;
- staged launch mod content excludes `current-driver-mod`.

## Focused Regression

Command:

```sh
mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest*' --tests '*LocalSessionApiServerTest.*driver mod*' --tests '*LocalSessionApiServerTest.prepared runtime selects packaged older fabric lane from manifest'
```

Result: passed.

## Local CI

Command:

```sh
mise run ci
```

Result: passed.

Covered gates:

- `mise exec -- gradle lint`;
- `mise run unused-check`;
- `mise exec -- gradle test`;
- `mise exec -- bun test playwright`.

Additional hygiene:

```sh
git diff --check
```

Result: passed.
