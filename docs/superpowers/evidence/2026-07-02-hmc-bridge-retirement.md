# Retired Bridge Evidence Path

Phase 209 removes the obsolete bridge evidence path from active Craftless
source, build, packaging, and current docs.

## Why

The bridge path was temporary launch/lifecycle evidence from the early JVM
foundation. The current product path is the in-client Fabric driver, runtime
capability graph discovery/projection, generated per-client OpenAPI, generic
invocation, SSE, and `craftless api`.

Keeping the old bridge module in `settings.gradle.kts`, `driver-runtime`, and
the Fabric driver package made the active architecture look like it still had
two product driver paths. It was not used by production client launch or
gameplay, and it exposed no gameplay capability.

## Changes

- Removed the `bridge-hmc` Gradle module and its tracked source/tests.
- Removed `HmcBridgeDriverBackend` from `driver-runtime`.
- Removed `bridge-hmc` from `driver-runtime` dependencies.
- Removed `bridge-hmc` from the nested `driver-fabric` package includes.
- Replaced stale bridge-retention tests with an absence guard that fails if the
  retired path returns to active source/build wiring.
- Updated README, roadmap, operating contract, module contracts, and
  file-management verification docs to describe the bridge path as retired.
- Removed the stale bridge limitations page from active docs.

## Red-Green Evidence

Before implementation:

```sh
mise exec -- gradle :protocol:test --tests 'com.minekube.craftless.protocol.NamespacePolicyTest.active build retires hmc bridge wiring'
```

Failed because active build files still included the bridge module/wiring.

After implementation:

```sh
mise exec -- gradle :protocol:test --tests 'com.minekube.craftless.protocol.NamespacePolicyTest.active build retires hmc bridge wiring' :driver-runtime:test :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricDriverModuleTest.official lane packages shared attach runtime dependencies without yarn remap gameplay lane'
```

Passed with `BUILD SUCCESSFUL`.

## Final Verification

```sh
mise exec -- gradle :driver-runtime:ktlintTestSourceSetCheck
mise exec -- gradle :protocol:ktlintTestSourceSetCheck
mise exec -- bun test playwright/src/distribution.test.ts
mise run ci
mise run docs-site-verify
```

All passed after fixing stale formatting and README guard expectations. The
docs-site verification rebuilt the static Fumadocs site, typechecked it, and
completed a Wrangler dry-run deploy.

Additional completion checks:

```sh
rg -n "bridge-hmc|HmcBridge|craftless-driver-bridge|bridge-evidence|:bridge-hmc" -S . -g '!docs/superpowers/**' -g '!**/build/**' -g '!**/.git/**' -g '!driver-fabric/run/**'
git diff --check
```

The active-source stale-reference scan returned no matches, and
`git diff --check` passed. The packaged Fabric driver jar was also inspected
for retired bridge classes/tokens and returned no matches.
