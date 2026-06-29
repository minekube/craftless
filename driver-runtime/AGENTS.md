# Driver Runtime Module Instructions

This file is intentionally compact. Keep only driver-runtime rules that future
agents must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#driver-runtime`

When driver-runtime-specific instructions need to change, update
`docs/agent-module-contracts.md#driver-runtime`, not this file.

## Do Not Miss

- `driver-runtime/` adapts the stable driver contract to concrete backends.
- Runtime adapters must stay version-neutral; version divergence arrives as
  metadata, graph nodes, lane decisions, or backend-specific adapters.
- Keep bridge details internal and never leak them into public results, action
  names, events, or errors.
- Do not compensate for a weak backend by adding runtime-side static gameplay
  branches.
