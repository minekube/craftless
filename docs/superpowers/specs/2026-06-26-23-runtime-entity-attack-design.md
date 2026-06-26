# Runtime Entity Attack Design

## Intent

Add the next missing generic primitive for final gameplay: attacking an entity
that was discovered through the live generated API. This phase must work on
the system layer, not by adding a cow-kill shortcut or a survival task macro.

## Product Rules

- Expose `entity.attack` only through the runtime graph and generated
  per-client OpenAPI/action projection.
- Accept Craftless-owned entity handles returned by `entity.query`.
- Keep Minecraft/Fabric/Yarn implementation names internal to the Fabric
  driver module.
- Do not add `kill.cow`, `combat.cow`, `task.survival.*`, or a hard-coded
  acceptance scenario action.
- Treat accepted attack invocation as weak evidence. Later gameplay must still
  verify entity state, inventory loot, and event stream evidence.

## Behavior

When the live client has player, world, and interaction manager support, the
runtime graph exposes:

- `entity.query` for public entity perception;
- `entity.attack` for a generic attack against a target handle.

`entity.attack` arguments:

- `target`: required object with a `handle` field from `entity.query`;
- `max-distance`: optional number, defaulted by the driver.

The Fabric adapter resolves the public handle on the client thread, checks
distance, calls the internal interaction manager attack operation, swings the
main hand, and returns public evidence such as target handle, distance, and
hit acceptance.

## Public-Agent Composition

The public gameplay runner may use `entity.attack` only after discovering it
from `/clients/{id}/actions` and discovering a target from `entity.query`.
It must invoke the action through `POST /clients/{id}:run`.

If `entity.attack` is absent, the runner may still complete earlier material
collection phases, but final gameplay remains incomplete. If a later combat
phase requires the primitive and it is absent, the blocker must be
`missing-generic-primitive:entity.attack`.

## Evidence

Tests and live artifacts must show:

- the runtime graph node exists with generic argument schema and correct
  availability;
- the graph adapter invokes through the Fabric backend and returns public
  result data;
- the public gameplay runner invokes `entity.attack` only through generated
  public action dispatch when a queried target handle exists;
- no scenario shortcut strings are introduced.
