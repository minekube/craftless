# OpenAPI Route Authority Evidence

Date: 2026-06-28

## Scope

Phase 168 makes `ClientSessionService.routesFor(clientId)` derive route
metadata from the generated per-client OpenAPI document instead of calling a
separate driver action-list projection.

This is a system-level cleanup only. It does not add gameplay operations,
public route shapes, CLI commands, action adapters, static action catalogs,
scenario shortcuts, version lanes, or support claims.

## Red

Command:

```sh
mise exec -- gradle :daemon:test --tests '*ClientSessionServiceTest.client route list is projected from generated runtime graph openapi*'
```

Observed before implementation:

- Exit code: `1`
- Failing test:
  `ClientSessionServiceTest > client route list is projected from generated runtime graph openapi()`
- Failure: `GraphOnlyRouteTestDriverSession.actions()` threw
  `IllegalStateException`, proving current `routesFor(clientId)` depended on
  `actions()` instead of generated runtime graph OpenAPI.

## Green

Command:

```sh
mise exec -- gradle :daemon:test --tests '*ClientSessionServiceTest.client route list is projected from generated runtime graph openapi*'
```

Observed after implementation:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

## Focused Regression

Command:

```sh
mise exec -- gradle :daemon:test --tests '*ClientSessionServiceTest*'
```

Observed:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

## Broader Regression

Command:

```sh
mise exec -- gradle :daemon:test :protocol:test
```

Observed:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

## Guardrail Regression

Command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim*'
```

Observed:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

This keeps the shortened module `AGENTS.md` files aligned with
`docs/agent-module-contracts.md`: the module files stay as pointers, and the
durable latest/current and attach-lane rules are checked in the module contract
document.

## Checklist Cleanup

`docs/project-completion-checklist.md` now has a top-level
`Current Goal Worklist` that separates the active remaining work from the
historical phase evidence log. The worklist tracks:

- system design and governance;
- runtime graph as public API authority;
- binding-exit work;
- multi-version foundation;
- transport and consumers;
- distribution and agent usability;
- quality gates;
- final gameplay gate.

## Diff Check

Command:

```sh
git diff --check
```

Observed:

- Exit code: `0`

## Local CI

Command:

```sh
mise run ci
```

Observed after the module-contract guardrail update:

- Exit code: `0`
- Gradle lint, detekt/unused-check, JVM tests, and Bun Playwright tests all
  completed successfully.
