# Transitional Fabric Action Allowlist Deletion Design

## Problem

Craftless still has `docs/architecture/transitional-fabric-action-allowlist.txt`,
a static list of hand-written Fabric gameplay action ids. Earlier phases moved
public action descriptors, schemas, availability, and invocation through the
runtime capability graph and private bootstrap operation definitions. The file
is now stale governance baggage: tests compare private bindings to it, and its
presence looks like a public static gameplay catalog.

The remaining blocker is not the file. The remaining blocker is that current
private executable bootstrap operations are still hand-maintained in
`fabricBootstrapOperationDefinitions()` until real generic runtime discovery can
replace that bootstrap layer.

## Goals

- Delete `docs/architecture/transitional-fabric-action-allowlist.txt`.
- Make the private Fabric binding guard compare binding operation ids to
  `fabricBootstrapOperationDefinitions()`.
- Keep the existing runtime graph operation set unchanged.
- Update active completion wording to name the real remaining blocker:
  transitional bootstrap operation definitions, not a static docs allowlist.

## Non-Goals

- Do not add or remove runtime operations.
- Do not add public gameplay APIs, route families, CLI gameplay catalogs,
  Fabric descriptor/binding pairs, or scenario shortcuts.
- Do not claim the broader binding exit is complete.
- Do not add compiled lanes, change Fabric dependency versions, or claim new
  Minecraft support.

## Acceptance Criteria

- A red test or source guard fails before implementation because
  `FabricDriverModuleTest` still reads the deleted allowlist file.
- The allowlist file is deleted.
- `FabricDriverModuleTest` compares `defaultFabricActionBindings()` operation
  ids with `fabricBootstrapOperationDefinitions()` ids instead.
- The existing runtime graph projection test still proves bootstrap definitions
  project into the graph.
- Active checklist/AGENTS wording no longer points at the deleted file as the
  remaining blocker.
- Focused tests, `git diff --check`, and `mise run ci` pass locally.
