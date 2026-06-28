# Installed Latest Release Alias Compatibility Probe Design

## Problem

The active goal requires runnable latest/current-version support, but current
installed packages only contain provider-backed Fabric driver lanes for
Minecraft `1.21.6` and representative older Minecraft `1.20.6`. Earlier
diagnostics recorded latest release `26.2` as unsupported. That evidence is
historical and must be refreshed through the installed packaged product surface
after the older packaged lane was proven.

The latest Minecraft alias is moving data. As of this phase, the live Mojang
manifest reports latest release `26.2` and latest snapshot
`26.3-snapshot-1`. The current Fabric driver build remains a Yarn-mapped
`1.21.6` lane plus a parameterized older `1.20.6` lane. Minecraft 26.x support
must not be claimed until a real provider-backed lane compiles, packages,
launches, attaches, exposes generated OpenAPI/actions/resources, streams SSE,
and passes public gameplay evidence.

## Decision

Run an installed packaged `latest-release` compatibility probe with the
packaged CLI from `build/docker/craftless/bin/craftless`:

1. Capture the live Mojang latest release and latest snapshot values.
2. Build/refresh the packaged CLI distribution.
3. Start the packaged supervisor with an isolated temporary workspace.
4. Request a Fabric client with `--version latest-release`.
5. Capture the cache/runtime/driver-lane result exactly as returned by the
   packaged product.
6. Record whether the result is runnable or unsupported, without converting an
   unsupported diagnostic into completion evidence.

The expected current result is an installed latest-release blocker: the alias
resolves to Minecraft `26.2`, Java/runtime selection should understand Java
25, Fabric Loader/API resolution should be data-driven, but no packaged
Craftless driver lane exists for Minecraft `26.2`.

The next implementation phase after this probe should target the real 26.x
lane boundary. Based on current Fabric ecosystem direction, that work should
investigate an official/Mojang-mappings lane instead of trying to force the
existing Yarn-mapped `v1_21_6` package to cover Minecraft 26.x.

## Non-Goals

- Do not mark latest/current support complete from an unsupported diagnostic.
- Do not add a static unsupported lane id or maintained product matrix entry.
- Do not add public gameplay actions, static CLI gameplay commands, scenario
  shortcuts, or version-specific public API routes.
- Do not duplicate the full Fabric backend for 26.x unless a specific
  Minecraft/Fabric API divergence requires a scoped lane adapter.
- Do not wait on remote CI for this phase.

## Verification

- Live Mojang manifest evidence is captured.
- `mise run package-cli` passes before using the packaged binary.
- The packaged supervisor starts from `build/docker/craftless/bin/craftless`.
- `craftless clients create latest-cli --version latest-release --loader fabric`
  returns either:
  - a runnable attached client with generated API evidence, or
  - a clear unsupported/error result showing the concrete resolved latest
    version and driver-lane blocker.
- Any launched client and supervisor processes are stopped.
- The checklist states the remaining latest/current completion requirement.
