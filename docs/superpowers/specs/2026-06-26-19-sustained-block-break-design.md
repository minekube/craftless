# Sustained Generic Block Break Design

## Intent

Make generic `world.block.break` represent real block-breaking progress instead
of only starting the Minecraft client's breaking state. Public-agent gameplay
cannot treat `started = true` as proof because ordinary survival blocks often
require multiple client break-progress ticks.

This remains a generic world action. It must not add `mine.log`,
`collect.wood`, `find.tree`, or any survival-specific operation.

## Product Rules

- Keep the action id `world.block.break`.
- Keep target handles and positions Craftless-owned.
- Add bounded generic progress control such as `ticks`, not material-specific
  behavior.
- Return machine-readable evidence for `started`, `changed`, `ticks`, and
  target identity.
- Public-agent policy must block when `world.block.break` reports no state
  change.

## Behavior

When invoked with a target:

1. Resolve the target handle or position.
2. Validate distance from the player eye.
3. Call `attackBlock` once to start the Minecraft client break.
4. Call `updateBlockBreakingProgress` for up to the bounded `ticks` argument.
5. Stop early when the block state at the target changes.
6. Swing the player's hand while progress is accepted.
7. Return `changed = true` only when public client state observed the block
   state changed.

If the block does not change within the budget, return `changed = false` and
leave final success/failure interpretation to the public agent.

## Evidence

Tests and live artifacts must show:

- `world.block.break` exposes generic `ticks` metadata;
- public-agent requests include a bounded `ticks` value;
- public-agent blocks on `changed = false`;
- live no-hold gameplay reaches inventory evidence after a sustained break;
- no scenario shortcuts are introduced.
