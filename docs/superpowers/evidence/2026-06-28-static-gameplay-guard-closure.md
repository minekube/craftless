# Static Gameplay Guard Closure Evidence

Date: 2026-06-28

## Scope

Phase 178 targets CL-02f. It adds concrete policy coverage for static gameplay
surface regressions and refactors the checklist so every remaining guard
surface is named.

With the final architecture check recorded below, this phase closes CL-02f and
therefore closes CL-02 in the active checklist.

## Red

Command:

```sh
mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.private fabric gameplay bindings are limited to bootstrap operation id references*'
```

Observed before implementation:

- Exit code: `1`
- Failure: `NoSuchFileException` for
  `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`.

## Focused Green

Command:

```sh
mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.private fabric execution adapters are limited to discovered operation id references*' --tests '*NamespacePolicyTest.adaptive cli and daemon production sources do not own static gameplay catalogs*'
```

Observed after implementation:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

## Guard Coverage

- CL-02f.1: scenario shortcut action ids are rejected by existing protocol
  namespace policy.
- CL-02f.2: private Fabric execution adapters cannot own public operation id
  literals, descriptors, or schemas; adapter constants must appear in runtime
  graph operation sources.
- CL-02f.3: production CLI Kotlin source is scanned for static gameplay
  command catalog literals.
- CL-02f.4: production daemon Kotlin source is scanned for static gameplay
  generated-alias route family literals.
- CL-02f.5: daemon live-event normalization cannot synthesize gameplay action
  ids from fallback event types.
- CL-02f.6: final architecture verification passed.

## Final Local Verification

```sh
git diff --check
```

Observed:

- Exit code: `0`

Command:

```sh
mise run architecture-check
```

Observed:

- Exit code: `0`
- Gradle protocol, daemon, CLI, and driver-fabric checks passed.
- Bun Playwright helper/distribution tests: `19 pass`, `0 fail`.
