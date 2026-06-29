# Testkit Module Instructions

This file is intentionally compact. Keep only testkit rules that future agents
must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#testkit`

When testkit-specific instructions need to change, update
`docs/agent-module-contracts.md#testkit`, not this file.

## Do Not Miss

- `testkit/` owns fake clients, fixtures, integration helpers, and outside-user
  verification harnesses.
- Fakes must exercise the same descriptors, generated OpenAPI, and generic
  invocation paths as real drivers.
- Keep tests deterministic and offline by default.
- Do not hide product behavior in test-only shortcuts that bypass public
  protocol or driver contracts.
- Gameplay scenarios may be composed for verification, but only as an external
  user or agent through public API/CLI/SSE paths.
