# Documentation Instructions

This file is intentionally compact. Keep only documentation rules that future
agents must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#docs`
- `docs/project-completion-checklist.md` and `docs/superpowers/phase-index.md`
  when updating status, phases, or evidence.

When docs-specific instructions need to change, update
`docs/agent-module-contracts.md#docs`, not this file.

## Do Not Miss

- `docs/` owns architecture, roadmap, evidence, specs, plans, and checklist
  material.
- Keep README and docs aligned with current architecture and clearly separate
  implemented behavior from roadmap.
- Do not document removed or inactive SDK/surface work as active product.
- Use `minekube.com` and `com.minekube.craftless`.
- Describe multi-version support as system work: manifests, Java/runtime
  selection, Fabric Loader/API resolution, driver mod manifests,
  compatibility lanes, runtime graph evidence, and public API verification.
- Put phase history and status in `docs/superpowers/phase-index.md` and
  `docs/project-completion-checklist.md`, not root or module AGENTS files.
