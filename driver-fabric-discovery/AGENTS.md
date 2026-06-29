# Fabric Discovery Module Instructions

This file is intentionally compact. Keep only Fabric-discovery rules that
future agents must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#driver-fabric-discovery`

When fabric-discovery-specific instructions need to change, update
`docs/agent-module-contracts.md#driver-fabric-discovery`, not this file.

## Do Not Miss

- `driver-fabric-discovery/` owns shared Fabric Loader/runtime metadata and
  reusable protocol-level graph composition across Fabric lanes.
- Keep it free of Yarn, intermediary, official-mapping, and Minecraft
  game-class calls.
- Do not add gameplay actions, action descriptors, scenario shortcuts, CLI
  behavior, public route families, or version-specific public APIs here.
- Lane modules may pass lane-specific probes and graph fragments into shared
  helpers; this module must not mint gameplay catalogs itself.
