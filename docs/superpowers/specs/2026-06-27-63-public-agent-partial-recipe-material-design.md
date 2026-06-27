# Public Agent Partial Recipe Material Design

## Intent

The latest final gameplay rerun blocked with
`insufficient-public-evidence:inventory.query.recipe-material` even though the
public agent had already verified one collected log and the live client exposed
generic recipe discovery/crafting. Later material collection failed because
generated navigation could not start for nearby dropped material, but recipe
composition did not require the larger target material count once at least one
recipe input was already proven.

## Product Rules

- Keep this as external public-agent policy in `testkit`.
- Do not add gameplay actions, descriptors, generated route families, CLI
  gameplay catalogs, Fabric descriptor/binding pairs, or scenario shortcuts.
- Do not add material-specific product actions such as `collect.wood`,
  `craft.planks`, or `make.sword`.
- Continue using only generated generic public actions such as
  `inventory.query`, `world.block.break`, `entity.query`, `navigation.plan`,
  `navigation.follow`, `recipe.query`, and `recipe.craft`.
- Do not treat weak evidence as success: at least one public inventory material
  count must be observed before continuing to recipe composition after a later
  collection blocker.

## Design

When `recipe.query` and `recipe.craft` are available, the public-agent runner
should allow recipe composition to proceed after a later material collection
blocker if public `inventory.query` has already proven at least one material
item. Without recipe composition, or with zero proven materials, the existing
blocker behavior remains unchanged.

This keeps the survival scenario composed outside the product API while
removing an overly strict public-agent policy that required multiple material
items before attempting generic recipe discovery/crafting.

## Evidence

Tests and artifacts must show:

- a public-agent unit test covers the partial-material path;
- the runner still blocks when no material evidence exists;
- request bodies contain no scenario shortcut action ids;
- final gameplay can be rerun past the previous
  `inventory.query.recipe-material` blocker.
