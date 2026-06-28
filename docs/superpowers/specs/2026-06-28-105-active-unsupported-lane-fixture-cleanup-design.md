# Active Unsupported Lane Fixture Cleanup Design

## Problem

Phase 93 removed static latest/older unsupported lane ids from product runtime
code. One active `testkit` smoke fixture still used the historical
`latest-release-26-2` id, which makes current test fixtures contradict the
generic unsupported-lane design even though historical evidence may still
mention that old id.

## Goals

- Keep historical evidence files unchanged.
- Replace active smoke fixture usage of `latest-release-26-2` with the generic
  `fabric-unsupported-26-2` fallback shape.
- Add a guard so active smoke fixtures cannot reintroduce
  `latest-release-26-2` or `older-release-1-20-6`.

## Non-Goals

- Do not add runnable version support.
- Do not add public gameplay actions, static route families, CLI gameplay
  catalogs, Fabric action bindings, scenario shortcuts, or version support
  claims.

## Acceptance Criteria

- A red/green guard fails when active smoke fixtures contain the historical
  latest/older unsupported lane ids.
- The affected `LocalMinecraftServerSmokeTest` still proves unsupported runtime
  lanes skip server provisioning and write runtime-lane evidence.
- Checklist and AGENTS record the cleanup as active-source alignment, not as
  multi-version support completion.
