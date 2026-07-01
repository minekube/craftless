# Runtime Target Create Rejection Design

Date: 2026-07-02
Phase: 202

## Problem

`GET /versions/support-targets` can now describe every discovered Fabric Loader
runtime row, including rows that Craftless does not yet support with a packaged
driver lane. The create-client path also has to honor that matrix. If an API
caller requests a resolvable Minecraft/Fabric Loader runtime that has no
matching Craftless driver mod, `/clients` must reject it before launch with the
same machine-readable reason family instead of returning an ambiguous generic
bad request.

## Contract

- Keep the public support source aligned with the runtime matrix: supported
  rows launch, unsupported rows reject before any Minecraft process starts.
- Use Craftless-owned names only. The public error code is
  `UNSUPPORTED_RUNTIME_TARGET`; the message includes the existing
  `FabricSupportReason` value.
- Distinguish missing driver coverage from incompatible loader coverage:
  `NO_DRIVER_MOD` when no driver row exists for the Minecraft target, and
  `NO_COMPATIBLE_DRIVER_MOD` when a target has driver rows but not for the
  requested runtime identity.
- Do not add static gameplay catalogs, static per-version launch branches, or
  route-specific CLI behavior.

## Design

The configured driver-mod provider remains the single authority for whether a
resolved runtime identity has a packaged driver lane. When its manifest lookup
fails for a Fabric request, it throws `UnsupportedClientRuntimeTarget` with the
matrix reason and nearby manifest context.

`POST /clients` catches that typed failure and serializes it as:

```json
{
  "code": "UNSUPPORTED_RUNTIME_TARGET",
  "message": "NO_COMPATIBLE_DRIVER_MOD: no Craftless driver lane supports ..."
}
```

The rejection happens after Fabric metadata has resolved the requested loader
identity and before cache preparation or process launch.
