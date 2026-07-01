# Fabric Loader Request Rejection Design

Date: 2026-07-02
Phase: 204

## Problem

`GET /versions/support-targets` enumerates discovered Fabric Loader runtime
identities. When an API caller asks `/clients` for a Fabric Loader version that
Fabric metadata does not list for the requested Minecraft target, the create
path used to fail during cache preparation with a generic `BAD_REQUEST`.

That leaves agents with ambiguity: the request is not a driver-manifest miss,
and it is not a supported runtime identity. It is a non-discoverable Fabric
loader/runtime combination and should be rejected with the same public
`UNSUPPORTED_RUNTIME_TARGET` family used by matrix-aligned driver failures.

## Contract

- If Fabric metadata does not list the requested loader version for the
  resolved Minecraft target, reject before cache artifact materialization and
  before process launch.
- Return public error code `UNSUPPORTED_RUNTIME_TARGET`.
- Include machine-readable reason `NO_COMPATIBLE_FABRIC_LOADER`.
- Include the requested Minecraft and Fabric Loader versions plus the
  available Fabric Loader versions for that target.
- Do not create static per-version route logic or special public APIs.

## Design

`CachePreparationService` already resolves Fabric metadata before preparing
cache artifacts. That boundary now converts a missing requested loader version
into `UnsupportedClientRuntimeTarget` with
`FabricSupportReason.NO_COMPATIBLE_FABRIC_LOADER`.

The existing `/clients` error mapping serializes that typed exception as:

```json
{
  "code": "UNSUPPORTED_RUNTIME_TARGET",
  "message": "NO_COMPATIBLE_FABRIC_LOADER: Fabric metadata does not list a compatible loader for ..."
}
```

The support reason enum is shared with OpenAPI, so adaptive CLI/help consumers
can discover the reason value from the supervisor spec.
