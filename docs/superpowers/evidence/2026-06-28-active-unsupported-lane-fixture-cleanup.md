# Active Unsupported Lane Fixture Cleanup Evidence

## Scope

Phase 105 removes the historical `latest-release-26-2` unsupported-lane id
from active smoke fixtures. Historical evidence files may still mention that
id, but active source fixtures should use the generic fallback shape.

This is not a runnable multi-version support claim.

## Red Check

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.active smoke fixtures do not keep static latest unsupported lane ids*'`
  failed because `testkit/src/test/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmokeTest.kt`
  still contained `latest-release-26-2`.

## Green Checks

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.active smoke fixtures do not keep static latest unsupported lane ids*'`
  passed.
- `mise exec -- gradle :testkit:test --tests '*LocalMinecraftServerSmokeTest.local server smoke records unsupported runtime lane without provisioning server*'`
  passed.

## Final Local Verification

- `git diff --check`
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.active smoke fixtures do not keep static latest unsupported lane ids*'`
- `mise exec -- gradle :testkit:test --tests '*LocalMinecraftServerSmokeTest.local server smoke records unsupported runtime lane without provisioning server*'`
- `mise run ci`

These commands passed locally on 2026-06-28.
