# Public Agent Task Composition Design

## Status

Draft for review. This spec records candidate design ideas only. It does not
approve implementation, change the roadmap by itself, or accept any
recommendation until it is reviewed and approved.

## Context

Craftless should prove that an external agent can complete real Minecraft
gameplay through the public generated API, SSE events, adaptive CLI, and agent
documentation. The product must stay thin: runtime discovery, generated
OpenAPI, generic invocation, resource/action projection, event streaming, and
client lifecycle.

The final gameplay proof needs an agent that can discover, plan, act, observe,
fail, and replan through Craftless-owned public contracts. That agent may have
higher-level policy, but Craftless itself must not become a catalog of
hand-written scenario shortcuts.

## Problem

Craftless needs an honest final gameplay proof such as collecting materials,
crafting or equipping useful tools, navigating to entities, fighting, building,
chatting, and verifying state. The current risk is repeatedly adding
scenario-specific product affordances when the real need is an external agent
policy that composes generic runtime-discovered actions.

The system should support this without Craftless exposing durable public
actions such as `find.tree`, `collect.logs`, `kill.cow`, `craft.sword`, or
`task.survival.*`.

## Non-Goals

- Do not add another Minecraft bot runtime as a Craftless dependency.
- Do not add a static LLM skill catalog as Craftless public API.
- Do not add static scenario actions for logs, animals, swords, houses, or
  survival tasks.
- Do not make this spec evidence that the candidate design is accepted.
- Do not implement this spec before a separate implementation plan is approved.

## Candidate Design

Craftless should keep two layers separate:

1. Product layer: generated per-client OpenAPI, generic actions, resources,
   handles, schemas, SSE events, JSON-RPC-style requests, adaptive CLI, and
   runtime discovery/projection.
2. Agent policy layer: a separate skill/client package that consumes the
   product layer to pursue goals such as "collect wood", "make a tool",
   "find food", "build shelter", or "play together".

The agent policy layer may expose higher-level procedures to the agent, but
those procedures must compile down to public Craftless discovery and invocation.
If a procedure cannot proceed, it should report a missing generic primitive or
resource projection instead of asking Craftless to add a scenario shortcut.

## Candidate Patterns

### Skill Contract

Agent-facing procedures should separate success, failure, and cancellation:

- success returns success/data;
- failure carries a machine-readable reason and enough context to replan;
- cancellation is distinct and should not double-trigger replanning.

A public agent procedure should emit structured state such as `running`,
`succeeded`, `failed`, `cancelled`, and `blocked`.

### Task And Action Lifecycle

Agent tasks should be able to produce one or more lower-level actions. Actions
need lifecycle visibility so agents and humans can understand what is happening:

- action accepted;
- action started;
- action progress/tick;
- action stopped/cancelled;
- action succeeded or failed;
- correlated result evidence.

Craftless should model this through public events and generic operation state.
The useful product requirement is the lifecycle and correlation model, not a
specific in-process task runner.

### Resource And Tracker Split

Keep resource goals separate from world tracking:

- resource policy maps desired materials to acquisition strategies;
- block/entity/resource projections maintain candidate targets;
- agent task composition decides mine, pick up, explore, craft, or smelt;
- failed targets can be blacklisted by the agent policy layer.

Craftless should keep the generic pieces in product API:

- block/entity/item/resource projections;
- target handles and positions;
- reachability/pathability metadata when available;
- bounded query windows;
- event streams for block, inventory, entity, and action changes.

The agent policy layer should own the higher-level recipes and choices.

### Bounded Perception

Generic perception must stay bounded and responsive:

- nearby scans;
- chunk or region scans when available;
- maximum chunks or regions;
- maximum matches per type;
- scan time budgets;
- stale candidate pruning.

Craftless should apply these constraints to generic resource projections so
`world.block.query`, future resource indexes, and entity queries remain safe for
headless and visible clients.

### Failure Feedback For Replanning

Failures should be machine-readable:

- `missing-generic-primitive:<id>`;
- `target-not-reachable`;
- `target-stale`;
- `action-timeout-ambiguous`;
- `inventory-unchanged`;
- `block-state-unchanged`;
- `event-not-observed`.

The public agent can then replan without hidden server commands, manual
intervention, or product shortcuts.

## Proposed Craftless Mapping

The product layer should prioritize these generic primitives and projections:

- `world.block.query` with bounded radius, categories, handles, positions, and
  stale-target handling;
- `entity.query` with categories, handles, positions, alive/dead state, and
  reachability/pathability hints when available;
- `inventory.query`, `inventory.equip`, and later generic inventory transfer;
- targetable `world.block.break` and `world.block.interact` using Craftless
  handles or positions;
- `navigation.plan`, `navigation.follow`, `navigation.stop`, and progress
  events;
- action lifecycle SSE events with correlation IDs;
- state verification projections for inventory, block state, entity state, and
  player pose.

The agent policy layer may define reviewable procedures such as:

- collect material;
- craft needed item;
- equip best available tool;
- find reachable entity;
- recover from stale target;
- explore when no candidate is found.

Those procedures must call only the generated Craftless API and must be kept out
of daemon/Fabric public action catalogs.

## Acceptance Criteria

- A public agent can discover needed actions from `/openapi.json`,
  `/clients/{id}/openapi.json`, `/clients/{id}/actions`, and resource
  projections.
- The agent can subscribe to SSE before acting and correlate action progress,
  success, failure, and verification events.
- Agent policy can complete a small survival workflow without invoking
  `task.survival.*`, server commands, manual movement, or static scenario
  actions.
- If the workflow blocks, artifacts name the missing generic primitive or
  projection instead of adding a shortcut.
- README and docs continue to describe Craftless as generated API
  infrastructure, not a bundled Minecraft bot brain.

## Review Questions

- Should this become an approved product phase after the current public-agent
  blockers, or should it remain background design context only?
- Should the external agent policy live inside this repository as a
  `.agents/skills` package, as `testkit` code, or as separate example/docs?
- Should Craftless expose resource projections as actions only, dedicated
  `/clients/{id}/resources/*` endpoints, or both through generated OpenAPI?
- Which candidate ideas should be explicitly rejected so future agents do not
  copy them accidentally?
