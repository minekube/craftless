# Documentation Instructions

`docs/` owns design notes, implementation plans, evidence records, and roadmap
material.

## Rules

- Keep README and docs aligned with current architecture.
- Make clear what is implemented now versus roadmap.
- Do not document removed TypeScript SDK or other inactive legacy surfaces as
  active implementation.
- Use `minekube.com` and `com.minekube.craftwright` for public domain/package
  references.
- Describe the bridge as evidence infrastructure only.
- Describe the durable driver direction as Fabric with generated per-client
  OpenAPI/action descriptors, adaptive CLI dispatch/help, and consolidated
  version-aware bindings where practical.
- Avoid stale public routes such as `/player/sendChat`; use
  `POST /clients/{id}:run` and generated aliases such as
  `POST /clients/{id}/player:chat` when discussing action invocation.

## Verification

For docs-only edits, run at least:

```sh
git diff --check
```
