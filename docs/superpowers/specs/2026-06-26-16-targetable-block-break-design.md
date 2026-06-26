# Targetable Generic Block Break Design

## Intent

Let agents break the block they discovered through public world perception. A
`world.block.query` result already exposes a Craftless-owned block handle and
position; `world.block.break` should be able to target that discovered handle
or position instead of relying only on whatever the current camera raycast hits.

This phase addresses the latest live public-agent blocker:
`world.block.query` found log blocks, navigation reached the area, raycast and
break were accepted, but inventory state did not prove collection. The action
evidence showed the raycast/break target was not necessarily the queried log.

## Product Rules

- Do not add `mine.log`, `collect.wood`, `find.tree`, `craft.sword`,
  `kill.cow`, `task.survival.*`, or another survival scenario action.
- Do not expose Fabric/Yarn/intermediary/raw Minecraft names.
- Keep the public action generic: `world.block.break` may accept a
  Craftless-owned `target` object or `handle`, but it must not become a log or
  survival-specific operation.
- Accepted break calls are still not success proof. Public agents must verify
  block or inventory state afterward.

## Behavior

`world.block.break` supports these generic targeting modes:

1. If `target.handle` is supplied and has the format `world.block:x:y:z`, parse
   it as a discovered Craftless block handle.
2. If `target.position` is supplied, use its `x`, `y`, and `z` values as a
   block position.
3. If no target is supplied, keep the existing raycast behavior for pointer-like
   control.

The Fabric adapter should:

- validate malformed handles and positions with stable argument errors;
- execute on the client thread;
- reject target blocks that are outside the supplied `max-distance`;
- start breaking the selected block through the existing interaction manager;
- return public evidence containing `hit`, `target-kind`, `started`, `block`,
  `handle`, `side`, and `position`;
- keep all names Craftless-owned.

The public-agent runner should:

- pass the selected block handle/position from `world.block.query` to
  `world.block.break`;
- verify final inventory or block-query state after the break;
- block honestly if state evidence still does not prove collection.

## Evidence

Artifacts and tests must show:

- `world.block.break` descriptors expose a generic `target` argument;
- Fabric invocation accepts target handles/positions without adding a new
  action id;
- public-agent request bodies pass the queried block target to the break action;
- fallback raycast mode remains available;
- scenario shortcut strings remain absent;
- live no-hold evidence either proves collection or identifies the next generic
  block-breaking/pickup primitive with stronger target evidence.

