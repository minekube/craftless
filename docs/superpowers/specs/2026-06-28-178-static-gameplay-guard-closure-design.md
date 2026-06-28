# Static Gameplay Guard Closure Design

## Problem

CL-02f needs concrete architecture guards, not another broad reminder that
Craftless should avoid static gameplay APIs.

Before this phase, one protocol policy test still referenced the deleted
`FabricActionBindings.kt` file. That made the guard stale and tied it to the
old descriptor/binding vocabulary. The checklist also compressed multiple
different guard surfaces into one checkbox, so future agents could not tell
which parts were actually protected.

## Design

Keep the guard boundary narrow and source-based:

- private Fabric execution adapters may reference operation id constants, but
  must not own public operation id literals, public descriptors, or public
  schemas;
- every execution adapter operation constant must be represented by a runtime
  graph operation source, either bootstrap seed or discovered probe;
- production CLI and daemon sources must not own static gameplay command
  catalogs or static generated-alias route families;
- scenario shortcut action ids remain rejected by protocol policy;
- live-event normalization must not synthesize gameplay action ids.

Tests may keep generated-spec fixture examples such as `player:chat` or
`world.block.break`; those fixtures prove projection behavior. The forbidden
ownership boundary is production CLI/daemon source and private Fabric
execution adapters.

## Non-Goals

- Do not remove every Fabric bootstrap operation definition in this phase.
- Do not add new gameplay actions.
- Do not change CLI, daemon, or Fabric runtime behavior.
- Do not claim latest/current or older-version support.
- Do not mark the whole project complete.

## Acceptance

- The stale `FabricActionBindings.kt` policy guard is replaced by a
  `FabricExecutionAdapters.kt` guard.
- The guard accepts operation constants from runtime graph sources, including
  discovered probes.
- Production CLI and daemon Kotlin sources are scanned for forbidden static
  gameplay action/path literals.
- The checklist splits CL-02f into named guard sub-gates.
- Focused protocol policy tests pass.
- Full architecture verification passes before CL-02f is marked complete.
