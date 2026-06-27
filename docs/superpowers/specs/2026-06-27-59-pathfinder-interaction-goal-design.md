# Pathfinder Interaction Goal Design

## Intent

The Phase 57/58 final gameplay evidence preserved server artifacts and now
propagates blocked public-agent outcomes correctly. The remaining live blocker
is generic navigation reach: `navigation.follow` can report success while
public `player.query` proves the client is still too far from a discovered
material block for `world.block.break`.

Root cause investigation found that the private Fabric pathfinder adapter maps
Craftless block navigation to a private exact-block goal. Prior-art Baritone
source distinguishes exact-block goals from interaction goals such as
"get adjacent to this block". For generated block handles that will be broken,
interacted with, or inspected, Craftless should prefer the private
interaction-reachable goal when the runtime pathfinder exposes it.

## Product Rules

- Keep the public surface unchanged: generated `navigation.plan` and
  `navigation.follow`.
- Keep private pathfinder class names out of public OpenAPI, action ids, events,
  CLI output, README, and docs contracts.
- Do not add gameplay shortcuts such as `find.tree`, `mine.log`,
  `collect.wood`, `craft.sword`, `kill.cow`, or a survival macro.
- Do not add static gameplay descriptors, generated route families, CLI
  gameplay catalogs, new compiled lanes, or public version-support claims.
- The versioned Fabric driver may use private pathfinder classes by reflection
  as implementation inputs.
- If the private interaction goal is absent, preserve the current exact-block
  fallback and existing unavailable behavior.

## Evidence

Tests and live artifacts must show:

- the reflective Fabric pathfinder backend prefers the private
  interaction-reachable block goal when available;
- the backend falls back to the existing exact-block goal when no interaction
  goal is available;
- public navigation status/events remain Craftless-owned and do not expose
  backend names;
- the next final gameplay rerun either advances beyond the material navigation
  blocker or records a newer precise generic blocker.
