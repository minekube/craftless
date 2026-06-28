# Official Client-State World Time Operation Design

## Problem

CL-03 requires the latest/current 26.x lane to become a real product lane. The
official lane already launches, attaches, and projects shared runtime,
registry, event, and client-state resources, but its runtime graph still
reported zero operations. That meant generated per-client OpenAPI and action
projection evidence could not prove any latest/current gameplay primitive.

The existing Yarn/remap lane already derives `world.time.query` from client
state rather than the bootstrap operation list. Keeping that projection local
to the Yarn/remap lane would force the official 26.x lane to duplicate public
operation shape.

## Design

Move the client-state `world.time.query` projection shape into the shared
Fabric discovery module. The shared helper:

- derives availability from `FabricClientStateGraphSnapshot.world`;
- emits `client-state` source evidence;
- owns the Craftless operation id and resource projection;
- accepts a private lane adapter key from the caller.

The Yarn/remap lane continues to get its private adapter key from existing
bootstrap adapter metadata. The official 26.x lane supplies only its private
adapter key and receives the same generated runtime graph operation and event
projection from shared discovery.

## Non-Goals

- Do not package the official 26.x lane as a supported driver.
- Do not claim CL-03 is complete.
- Do not add scenario shortcuts or static CLI/daemon gameplay commands.
- Do not copy Yarn/remap gameplay gateway or execution adapters into the
  official lane.
- Do not implement official `world.time.query` invocation in this phase.

## Acceptance

- The official backend runtime graph includes `world.time.query` when its
  client-state provider reports a world.
- The generated backend action projection includes `world.time.query`.
- The operation has `world.time` resource ownership, availability from
  client-state, and `client-state` source evidence.
- The Yarn/remap lane still exposes `world.time.query` from client-state.
- `mise run fabric-lane-check-latest-official` still reports
  `status=compiled`.
