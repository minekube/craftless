# Daemon Module Instructions

This file is intentionally compact. Keep only daemon rules that future agents
must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#daemon`

When daemon-specific instructions need to change, update
`docs/agent-module-contracts.md#daemon`, not this file.

## Do Not Miss

- `daemon/` owns the Ktor supervisor/session API and runtime wiring, not
  gameplay catalogs.
- Keep stable lifecycle/client/version/cache/file routes separate from the
  generated per-client API.
- Do not add public static gameplay route families; use descriptors plus
  generic invocation and generated routes described by OpenAPI.
- Keep Minecraft-version knowledge in resolver data/services and compatibility
  probes, not per-version daemon routes, session APIs, or action catalogs.
- Do not claim generated gameplay support until a prepared session is replaced
  by the attached in-client driver and exposed through public API evidence.
