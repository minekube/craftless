# Driver API Module Instructions

This file is intentionally compact. Keep only driver-api rules that future
agents must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#driver-api`

When driver-api-specific instructions need to change, update
`docs/agent-module-contracts.md#driver-api`, not this file.

## Do Not Miss

- `driver-api/` is the stable JVM contract between daemon/runtime code and
  in-client automation implementations.
- Keep the contract small: lifecycle, session state, runtime metadata, events,
  action discovery, and generic invocation.
- Do not add one stable Kotlin method per Minecraft action. Chat, inventory,
  blocks, entities, recipes, navigation, and combat come through discovered
  actions/resources/handles/schemas.
- Keep it Minecraft-version-neutral; divergence belongs behind implementation
  lanes as data, metadata, availability, graph evidence, or adapter behavior.
