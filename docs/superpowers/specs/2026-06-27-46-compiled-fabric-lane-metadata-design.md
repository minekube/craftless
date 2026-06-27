# Phase 46: Compiled Fabric Lane Metadata Design

## Goal

Keep the current compiled Fabric lane metadata in one internal Kotlin source of
truth so compatibility checks, provider selection, smoke plans, and private
runtime evidence cannot drift independently.

## Context

Craftless now has a compatibility matrix and runtime/provider facades, but the
current compiled lane still repeats the same Minecraft, loader, Fabric API,
Java, provider, and mappings values in multiple Kotlin files. That makes future
multi-version work brittle because changing or adding a lane requires chasing
string copies before the graph/runtime system can make an honest availability
decision.

Gradle and Fabric resource metadata may still need build-time constants because
the module is compiled against one Loom target. This phase centralizes the
Kotlin runtime metadata used by driver code and tests. It does not claim new
Minecraft version support.

## Requirements

- Add one internal compiled-lane metadata object for the current Fabric/Loom
  target.
- Make the Fabric compatibility matrix derive the supported current lane from
  that metadata object.
- Make the current-lane runtime provider derive its provider id and supported
  Minecraft version from the same metadata object.
- Make smoke/final gameplay plan defaults derive their Minecraft version from
  the same metadata object.
- Preserve existing public generated OpenAPI action/resource ids and behavior.
- Preserve latest-release unsupported `26.2` lane evidence as an explicit
  unsupported runtime lane until a real compiled/runtime provider exists.
- Do not add public gameplay action ids, generated route families, CLI gameplay
  catalogs, Fabric descriptor/binding pairs, scenario shortcuts, or a new public
  version-specific API.

## Non-Goals

- Do not parameterize Loom compilation in this phase.
- Do not update Fabric dependencies or claim broad Fabric support.
- Do not remove transitional bootstrap action descriptors.
- Do not add a second compiled Minecraft dependency lane.
- Do not mark Craftless complete.

## Verification

- A focused runtime matrix test proves the default matrix current lane equals
  the compiled-lane metadata object.
- A focused current-lane provider test proves provider id and supported version
  come from the compiled-lane metadata object.
- A focused smoke-plan test proves client and final gameplay plan defaults use
  the compiled-lane metadata object.
- `driver-fabric` tests pass.
- `mise run lint`, `mise run architecture-check`, and `mise run ci` pass before
  claiming this phase complete.
