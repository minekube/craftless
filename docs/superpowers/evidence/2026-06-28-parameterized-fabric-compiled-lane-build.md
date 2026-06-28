# Parameterized Fabric Compiled Lane Build Evidence

## Scope

Phase 134 makes the single compiled Fabric driver lane build parameterized by
Gradle properties so compatibility probes can compile the same driver source
against real lane metadata without editing source constants. This phase does
not package additional driver artifacts, claim latest/older support, add
gameplay APIs, add route families, add CLI gameplay catalogs, or add scenario
shortcuts.

## Live Metadata Probe

- Official Mojang manifest on 2026-06-28 reported latest release `26.2` and
  latest snapshot `26.3-snapshot-1`.
- Fabric Loader metadata for `26.2` reported stable loader `0.19.3`.
- Fabric API Maven metadata had latest matching `26.2` artifact
  `0.153.0+26.2`.
- Yarn Maven metadata had no `26.2+...` mappings artifact, so a typed compiled
  latest lane cannot be claimed from the current Yarn-backed build.
- Representative older `1.20.6` metadata exists: Fabric Loader `0.19.3`,
  Fabric API `0.100.8+1.20.6`, and Yarn `1.20.6+build.3`.

## Red Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric compiled lane build is parameterized for compatibility probes*'`
  - Failed before implementation because `driver-fabric/build.gradle.kts` did
    not contain the required `craftless.fabric.*` property inputs.

## Green Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric compiled lane build is parameterized for compatibility probes*'`
  - Passed after the compiled lane constants became Gradle property-backed with
    current-lane defaults.
- `mise run fabric-lane-check-older`
  - Exited successfully as a probe and wrote
    `build/reports/fabric-lane-check-older.status` with
    `status=source-compatibility-blocked`.
  - The captured compile blockers are typed 1.21-era source references:
    `PlayerInput`, `RecipeDisplayEntry`, and `ClientWorldEvents`.
  - This proves the representative older lane is metadata-resolvable but not
    source-compatible with the current typed `v1_21_6` driver code.

## Local CI Evidence

- `mise run package-cli`
  - Passed. The task regenerated default `1.21.6` packaged artifacts after the
    older-lane probe and verified the packaged `driver-mods.json` identity
    fields.
- `git diff --check`
  - Passed.
- `mise run ci`
  - Passed. Ran lint, unused/dead-code checks through detekt, Gradle tests, and
    Bun Playwright helper/distribution tests.
