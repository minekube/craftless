# Runtime Target Create Rejection Plan

Date: 2026-07-02
Phase: 202

## Checklist

- [x] Add an HTTP regression test that requests a discoverable Fabric Loader
  runtime with no matching configured driver lane.
- [x] Assert `/clients` returns `400` with `UNSUPPORTED_RUNTIME_TARGET`,
  includes `NO_COMPATIBLE_DRIVER_MOD`, names the requested Minecraft and loader
  versions, and does not invoke the runtime launcher.
- [x] Replace the configured manifest miss for Fabric with a typed
  `UnsupportedClientRuntimeTarget` that carries `FabricSupportReason`.
- [x] Map that typed failure to the public error response in `POST /clients`.
- [x] Run focused daemon tests, full daemon tests, repository checks, commit,
  push, and watch main CI.
