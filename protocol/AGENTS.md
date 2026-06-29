# Protocol Module Instructions

This file is intentionally compact. Keep only protocol rules that future agents
must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#protocol`

When protocol-specific instructions need to change, update
`docs/agent-module-contracts.md#protocol`, not this file.

## Do Not Miss

- `protocol/` owns Craftless-owned API metadata, DTOs, OpenAPI generation, and
  naming rules for adaptive users.
- OpenAPI/action descriptors are the authority for agents, SDKs, CLI help, and
  tests.
- Keep HTTP verbs as protocol data strings such as `"GET"` and `"POST"`; do
  not add a Craftless HTTP method enum.
- Version-specific facts are metadata, fingerprints, availability reasons,
  graph evidence, schemas, and descriptors, not duplicated DTO families.
- Do not encode one gameplay plan as protocol concepts.
