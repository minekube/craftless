# Public Agent Material Equip Design

## Intent

Extend the external public-agent gameplay proof by selecting material that was
collected through the generated API. This keeps the acceptance scenario on the
public surface while preparing later placement, crafting, and combat checks.

This phase must not hard-code survival tasks. The runner may choose an item
from public `inventory.query` state, but the product API remains generic:
`inventory.query`, `inventory.equip`, and subsequent public state verification.

## Product Rules

- Use only actions discovered from `GET /clients/{id}/actions`.
- Invoke through `POST /clients/{id}:run`.
- Do not add `collect.wood`, `equip.log`, `craft.sword`, `kill.cow`,
  `task.survival.*`, or any other scenario-specific action.
- Treat accepted action responses as insufficient without state evidence.
- If the collected material is not in a hotbar slot, stop with a
  machine-readable blocker instead of inventing an inventory-move shortcut.

## Behavior

After public material collection proves an inventory log item exists:

1. Parse `inventory.query` data for a non-empty hotbar slot whose public item
   name contains `log`.
2. Invoke `inventory.equip` with that slot through generated action dispatch.
3. Re-query `inventory.query`.
4. Verify `selected-slot` matches the requested hotbar slot.
5. Continue to later generic observation actions.

If no hotbar log slot is present, return
`insufficient-public-evidence:inventory.query.hotbar-log`.

If the selected slot is not reflected by the follow-up inventory query, return
`insufficient-public-evidence:inventory.equip.selected-slot`.

## Evidence

Tests and live artifacts must show:

- the runner requires `inventory.equip` as a discovered primitive;
- `inventory.equip` is invoked after material collection;
- the requested slot comes from public inventory state;
- a follow-up `inventory.query` verifies selected-slot state;
- blockers are explicit when hotbar or selected-slot evidence is missing;
- no scenario shortcut strings are introduced.
