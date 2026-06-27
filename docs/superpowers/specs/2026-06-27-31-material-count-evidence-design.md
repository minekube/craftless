# Material Count Evidence Design

## Intent

The 2026-06-27 final gameplay rerun reached the human-ready window, but the
process-external public agent reported
`insufficient-public-evidence:inventory.query.recipe-material`. Evidence showed
the agent broke a second generated log target through `world.block.break`, but
the pickup verification loop accepted the already-collected first `Oak Log` as
proof that the second break had produced new inventory material.

This phase improves external public-agent policy only. It must not add
`collect.wood`, `mine.log`, `craft.sword`, a survival macro, or any new product
gameplay action.

## Product Rules

- Keep material discovery on generated `world.block.query` results.
- Keep movement and pickup on generated `navigation.plan`,
  `navigation.follow`, optional generated `player.move`, generated
  `entity.query`, and generated `inventory.query`.
- Treat accepted `world.block.break` as insufficient until public state proves
  either a block-state change plus increased inventory material count or an
  explicit blocker.
- Do not count stale inventory material from a previous collection attempt as
  proof that a new material pickup succeeded.

## Behavior

For each material collection attempt:

1. Record the public material count known before breaking the next generated
   block target.
2. Break only a generated public block handle or position discovered from
   `world.block.query`.
3. Query public dropped entities and public inventory after the break.
4. Continue bounded pickup evidence attempts while the inventory material count
   is less than or equal to the pre-break count.
5. Treat the attempt as successful only when `inventory.query` proves the
   material count increased.
6. Return `insufficient-public-evidence:inventory.query.recipe-material` when
   recipe composition needs more material and the count does not increase.

## Evidence

Tests and live artifacts must show:

- a stale one-log inventory response does not satisfy the second material
  collection attempt;
- the runner continues generated pickup evidence attempts until the public log
  count increases;
- no `task.survival.*`, `collect.wood`, `mine.log`, or scenario shortcut
  appears;
- the final gameplay gate either reaches `publicAgentState=RAN` or reports a
  precise generic evidence blocker.
