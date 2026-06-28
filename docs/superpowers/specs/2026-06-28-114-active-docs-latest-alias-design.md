# Active Docs Latest Alias Design

## Problem

Runtime code now supports `latest-release` and `latest-snapshot` aliases for
cache preparation, Java runtime resolution, and prepared driver-mod lane
selection. Active docs still show concrete versions as the normal user path
and describe `26.2` as "current latest" in roadmap prose. Concrete latest
values are valid historical evidence, but they are a bad active contract for
users because they drift as Mojang releases new versions.

This is documentation/product-surface alignment, not a gameplay API problem.

## Goals

- Make active README examples use `latest-release` where they demonstrate
  current-version client/cache usage.
- Document `latest-release` and `latest-snapshot` in client file/cache
  management docs.
- Reword active roadmap text so concrete latest values are described as
  historical probe evidence, while active flows use aliases.
- Add a docs guard so README and roadmap do not drift back to presenting a
  concrete latest id as the active "current latest" contract.

## Non-Goals

- Do not rewrite historical evidence, old specs, or old plans.
- Do not claim latest/current or older Fabric client lanes are runnable.
- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric gameplay bindings, scenario shortcuts, or public
  version-specific APIs.

## Acceptance Criteria

- A Bun docs test fails before implementation because README does not mention
  `latest-release` in cache/client examples and roadmap still says
  `current latest \`26.2\``.
- After implementation, README shows `latest-release` in the create-client and
  cache-prepare examples.
- After implementation, `docs/client-file-management.md` documents
  `latest-release` / `latest-snapshot` aliases.
- After implementation, `docs/roadmap.md` avoids active "current latest
  `<version>`" wording and describes concrete latest ids as historical probe
  evidence only.
- AGENTS/checklist/evidence record Phase 114 and keep runnable latest/older
  support open.
