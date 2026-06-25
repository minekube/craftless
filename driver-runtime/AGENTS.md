# Driver Runtime Module Instructions

`driver-runtime/` adapts the stable `driver-api` contract to concrete backends.

## Scope

- `BackendDriverSession`.
- `DriverBackend` abstractions.
- Temporary HMC bridge backend adapter.

## Rules

- Keep bridge details internal. Public results, events, actions, and errors must
  stay Craftless-owned.
- Do not let HMC-Specifics commands, console text, or command syntax become the
  public API.
- The bridge backend is evidence infrastructure only; do not present it as the
  final automation driver.
- Preserve generic action invocation and typed JSON args across the backend
  boundary.
- Prefer shared runtime behavior here over duplicating lifecycle/event logic in
  each driver backend.

## Verification

```sh
mise exec -- gradle :driver-runtime:test
```
