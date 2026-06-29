# HMC Bridge Module Instructions

This file is intentionally compact. Keep only bridge rules that future agents
must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#bridge-hmc`

When bridge-specific instructions need to change, update
`docs/agent-module-contracts.md#bridge-hmc`, not this file.

## Do Not Miss

- `bridge-hmc/` is temporary evidence infrastructure for launching and
  controlling real clients before Fabric driver coverage is complete.
- Never expose HeadlessMC or HMC-Specifics command strings as public API names,
  JSON fields, CLI verbs, SDK methods, or docs contracts.
- Do not use bridge behavior to justify product API shape, version support, or
  gameplay completion.
- Real-client smoke tests stay opt-in and guarded by environment variables.
