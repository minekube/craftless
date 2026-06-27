# Phase 45: Descriptor-Derived Graph Schemas Design

## Goal

Fabric runtime graph operations should reuse schemas from discovered Craftless
action descriptors when a descriptor already exists, instead of duplicating
weak argument-only metadata in `FabricCapabilityProbe`.

## Context

`FabricCapabilityProbeContext.operation()` currently projects descriptor
arguments into `RuntimeOperationNode.arguments`, but leaves
`RuntimeOperationNode.result` at the default empty object schema. That weakens
the generated per-client OpenAPI and descriptor projections even when the
discovered action descriptor already has a structured result contract.

This is not new gameplay breadth. It is a projection cleanup that makes the
runtime graph a more faithful input to generated OpenAPI.

## Requirements

- Resolve the existing discovered descriptor once for each bootstrap graph
  operation by checking live bindings first and then the transitional bootstrap
  descriptor fallback.
- Derive runtime operation argument schemas from descriptor argument schemas,
  including nested object properties and array items where present.
- Derive runtime operation result schemas from descriptor result schemas,
  including property required flags.
- Keep specialized runtime-only graph result schemas such as recipe query,
  recipe craft, entity query, and block query untouched when they are already
  explicit graph schemas.
- Keep this as an internal Fabric graph projection improvement.
- Do not add public gameplay action ids, generated route families, CLI
  gameplay catalogs, Fabric descriptor/binding pairs, or scenario shortcuts.
- Do not expose Fabric/Yarn/intermediary/raw Minecraft names.

## Non-Goals

- Do not redesign `DriverActionDescriptor`.
- Do not remove transitional bootstrap descriptors in this phase.
- Do not add new actions or resources.
- Do not change invocation behavior.
- Do not hand-author schemas for Minecraft-specific gameplay beyond reusing
  descriptors that already exist.

## Verification

- A focused `FabricCapabilityProbeTest` proves `player.raycast` graph result
  schemas derive `action`, `status`, `message`, and `data` from its action
  descriptor.
- `driver-fabric` capability probe tests pass.
- `mise run lint`, `mise run architecture-check`, and `mise run ci` pass before
  claiming this phase complete.
