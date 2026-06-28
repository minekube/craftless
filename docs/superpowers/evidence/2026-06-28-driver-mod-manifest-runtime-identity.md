# Driver Mod Manifest Runtime Identity Evidence

## Scope

Phase 133 makes packaged driver-mod manifests carry runtime identity fields
from the generated private Fabric driver lane catalog and makes daemon
selection honor known prepared-runtime identity fields. This phase does not add
a compiled lane, change Fabric or Minecraft dependency versions, claim
latest/older support, or add gameplay APIs/actions/routes/scenario shortcuts.

## Red Evidence

- `mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.manifest fabric api mismatch rejects runtime identity*'`
  - Failed before implementation at test compilation because
    `ClientRuntimeDriverModRequest` could not carry `fabricApiVersion`.
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.cli driver mod manifest projection carries runtime identity not build fields*'`
  - Failed before implementation because `cli/build.gradle.kts` did not project
    `fabricApiVersion`, `javaMajorVersion`, or `mappingsFingerprint`.
- `mise run package-cli`
  - Failed after the first implementation because the private
    `fabric-driver-lanes.json` catalog did not emit `mappingsFingerprint`;
    the public manifest projection correctly refused to invent that field.

## Green Evidence

- `mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*'`
  - Passed after optional manifest/request identity fields were added and
    Fabric API/Java major mismatches rejected manifest entries.
- `mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.*driver mod*'`
  - Passed after prepared-runtime tests asserted the resolved Fabric API and
    Java major identity are passed to the driver-mod provider.
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.cli driver mod manifest projection carries runtime identity not build fields*'`
  - Passed after the CLI manifest projection carried runtime identity fields
    and excluded build-only fields.
- `mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*driver mod manifest*'`
  - Passed, preserving packaged manifest behavior and old-style manifest
    compatibility.
- `mise run package-cli`
  - Passed after `writeFabricDriverLaneCatalog` emitted
    `mappingsFingerprint`. The task verified tar/zip manifests include
    `fabricApiVersion`, `javaMajorVersion`, and `mappingsFingerprint`, and
    still exclude `artifactKey` and `distributionPath`.

## Local CI Evidence

- `git diff --check`
  - Passed.
- `mise run ci`
  - Passed. Ran lint, unused/dead-code checks through detekt, Gradle tests, and
    Bun Playwright helper/distribution tests.
