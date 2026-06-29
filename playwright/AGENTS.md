# Playwright Helper Instructions

This file is intentionally compact. Keep only Playwright-helper rules that
future agents must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#playwright`

When Playwright-specific instructions need to change, update
`docs/agent-module-contracts.md#playwright`, not this file.

## Do Not Miss

- `playwright/` contains external helper tests and fixtures, not a product SDK.
- Use Bun through mise: `mise exec -- bun ...`.
- Do not use npm, npx, yarn, pnpm, or globally installed Node tooling.
- Helpers should speak the daemon/OpenAPI/action API directly.
- Do not parse human CLI output, add TypeScript-side gameplay catalogs, or
  reintroduce a TypeScript SDK as an active product surface.
