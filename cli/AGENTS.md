# CLI Module Instructions

This file is intentionally compact. Keep only CLI rules that future agents
must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#cli`

When CLI-specific instructions need to change, update
`docs/agent-module-contracts.md#cli`, not this file.

## Do Not Miss

- `cli/` owns the JVM `craftless` command-line interface.
- Use Clikt for commands, Mordant for terminal output, and Ktor Client for API
  calls.
- Static CLI commands may cover daemon startup plus the generic `api`
  invoker.
- API route invocation/help must be adaptive from `/openapi.json`,
  `/clients/{id}/openapi.json`, and `/clients/{id}/actions`.
- Do not add static CLI trees for Minecraft versions, Fabric lanes, survival
  scenarios, recipes, combat flows, navigation flows, or version workarounds.
