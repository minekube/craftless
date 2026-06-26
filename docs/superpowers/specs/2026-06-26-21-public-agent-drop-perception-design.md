# Public Agent Drop Perception Design

## Intent

Use public entity perception to bridge the gap between "a block changed" and
"material is in inventory." Item drops are world entities before pickup, so the
public agent should observe them through `entity.query` instead of assuming
pickup happened.

This remains external agent policy. Do not add a product action such as
`pickup.log` or `collect.wood`.

## Product Rules

- Use `entity.query` as the public perception primitive for dropped item
  entities.
- Use generic navigation to move toward public entity positions.
- Verify success only through `inventory.query`.
- Keep final product actions generic and generated from the live client.

## Behavior

After `world.block.break` reports `changed = true`:

1. Navigate near the broken block target.
2. Invoke `entity.query`.
3. If a public object entity label indicates the desired material, navigate to
   that entity position.
4. Query inventory.
5. Continue to equip only when inventory state proves material pickup.

If no material entity is observed, the runner still queries inventory after
the block-target pickup movement and reports the existing inventory-evidence
blocker when needed.

## Evidence

Tests and live artifacts must show:

- `entity.query` runs before final material inventory verification;
- material drop positions can drive generic navigation;
- inventory remains the source of truth for pickup success;
- no pickup or survival shortcut action is introduced.
