# Official Fabric Lane Module Instructions

This file is intentionally compact. Keep only official-lane rules that future
agents must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#driver-fabric-official`

When official-lane-specific instructions need to change, update
`docs/agent-module-contracts.md#driver-fabric-official`, not this file.

## Do Not Miss

- `driver-fabric-official/` is the latest/current Fabric lane boundary for
  official/unobfuscated mappings, not a second product API.
- Treat official-lane code as a compatibility/probe boundary. Durable behavior
  belongs in shared Fabric attach/runtime/discovery/projection modules by
  default.
- Do not clone Yarn/remap gameplay bindings or make this lane depend on the
  Yarn/remap lane.
- Package/support this lane only when launch, self-attach, generated
  OpenAPI/actions/resources, SSE, and public CLI/API evidence pass.
