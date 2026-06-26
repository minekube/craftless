# Public Agent Material Pickup Design

## Intent

Continue the public-agent acceptance scenario after a block has changed by
using generic navigation and inventory state to collect dropped material. A
successful block break is not the same as an inventory pickup.

This remains external agent policy. Craftless must not add `pickup.log`,
`collect.wood`, `mine.log`, or a survival macro.

## Product Rules

- Use public `world.block.query` output to choose targets.
- Prefer reachable material candidates from public position/distance evidence.
- Use `navigation.plan`, `navigation.follow`, and `inventory.query` for pickup
  verification.
- Do not add a product pickup shortcut.
- Keep blockers machine-readable when inventory state does not prove pickup.

## Behavior

The public-agent runner should:

1. Prefer lower material block positions from `world.block.query` so it does
   not target only canopy logs when base logs are also visible.
2. Break the selected target through `world.block.break`.
3. After `changed = true`, navigate to the selected target position with a
   small radius.
4. Query inventory only after that pickup movement.
5. Continue to `inventory.equip` only when inventory state contains collected
   material.

## Evidence

Tests and live artifacts must show:

- lower material candidates are selected from public query data;
- pickup movement is composed from generic navigation actions;
- no scenario shortcut strings are introduced;
- live no-hold evidence reaches inventory material proof before equip.
