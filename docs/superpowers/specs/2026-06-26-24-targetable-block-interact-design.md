# Targetable Block Interact Design

## Intent

Make block placement/building testable through the generated public API. The
current `world.block.interact` action is raycast-only, which is unreliable for
external agents that discover block handles from `world.block.query`.

This phase adds targetable generic block interaction. It must not add
`build.house`, `place.log`, or another scenario shortcut.

## Product Rules

- Keep the public action id `world.block.interact`.
- Add generic target metadata to the existing action instead of creating a
  static placement route.
- Accept Craftless-owned block handles or positions discovered by public block
  queries.
- Keep Fabric/Minecraft implementation names internal.
- Return state evidence when the interaction changes an adjacent block.

## Behavior

`world.block.interact` accepts optional arguments:

- `target`: object with a `handle` or `position` for the block being
  interacted with;
- `side`: string side name such as `up`, used with targetable interaction;
- `max-distance`: optional number;
- `include-fluids`: optional boolean for raycast fallback.

When `target` is present, the Fabric binding creates an internal block hit on
the target block and side, invokes the client interaction manager with the main
hand, swings on accepted result, and returns public evidence:

- `accepted`;
- `changed`;
- interacted target handle/position;
- adjacent changed position/handle for placement-style interactions.

When `target` is absent, existing raycast interaction behavior remains.

## Public-Agent Composition

The public gameplay runner may compose placement only when the generated
descriptor for `world.block.interact` includes `target`. It should:

1. keep using public inventory evidence to equip a collected block;
2. query nearby block support through `world.block.query`;
3. invoke `world.block.interact` with a public target handle and `side = up`;
4. treat `changed = false` as insufficient evidence.

## Evidence

Tests and live artifacts must show:

- descriptor/schema includes targetable interaction arguments;
- the Fabric binding accepts public block handles and returns change evidence;
- the public gameplay runner invokes targetable interaction only through
  generated dispatch when the descriptor supports it;
- no placement/building scenario shortcut strings are introduced.
