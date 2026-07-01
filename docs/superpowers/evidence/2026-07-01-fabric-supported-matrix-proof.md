# Fabric Supported Matrix Proof Evidence

Phase 199 adds automation for proving every currently supported packaged
Fabric support-target row through public Craftless product surfaces.

## Verification

```sh
mise tasks | rg 'packaged-(current-lane|fabric-supported-matrix)-probe'
```

Result: passed. The task list includes:

- `packaged-current-lane-probe`;
- `packaged-fabric-supported-matrix-probe`.

```sh
bash -n scripts/packaged-fabric-lane-probe.sh
bash -n scripts/packaged-fabric-supported-matrix-probe.sh
```

Result: passed.

```sh
CRAFTLESS_PACKAGED_MATRIX_DISCOVERY_ONLY=1 \
CRAFTLESS_PACKAGED_MATRIX_TIMEOUT_MS=120000 \
bash scripts/packaged-fabric-supported-matrix-probe.sh
```

Result: passed. The generated `probe-jobs.json` contained three jobs discovered
from the packaged API: `26.2` requested through `latest-release`, `1.21.6`, and
`1.20.6`.

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Result: passed. The distribution tests assert the generic packaged Fabric lane
probe, the `1.21.6` current-lane task, the API-discovered supported-matrix
runner, and the scheduled/manual GitHub workflow.

```sh
mise run docs-site-verify
```

Result: passed.

```sh
mise run ci
```

Result: passed. This included lint, unused-check/detekt, Gradle tests,
packaged Craftless CLI smoke, and Playwright tests.

```sh
gh run watch 28545512512 --repo minekube/craftless --exit-status
```

Result: passed. The manual `fabric support matrix` workflow completed
successfully on main in 16m29s:
<https://github.com/minekube/craftless/actions/runs/28545512512>.

The run uploaded `fabric-support-matrix-reports` as artifact `8022012394`,
preserving packaged probe reports for the supported Fabric rows.

```sh
gh run watch 28546872180 --repo minekube/craftless --exit-status
```

Result: passed. After the matrix task was changed to generate row probes from
`/versions/support-targets`, the manual `fabric support matrix` workflow
completed successfully on main in 11m0s:
<https://github.com/minekube/craftless/actions/runs/28546872180>.

The run uploaded `fabric-support-matrix-reports` as artifact `8022429625`,
including the generated `probe-jobs.json` plan and row artifacts.

```sh
git diff --check
```

Result: passed.

## Current Scope

The supported-matrix task now starts the packaged daemon, reads
`/versions/runtime-targets` and `/versions/support-targets`, validates the
reported supported Fabric driver-mod descriptors against packaged
`driver-mods.json`, writes a generated probe job plan, and invokes the generic
packaged Fabric lane probe for each supported descriptor. On July 1, 2026, that
generated plan covered the supported packaged rows for `26.2`, `1.21.6`, and
`1.20.6`.

This proves the automation surface for every currently supported packaged row
reported by `/versions/support-targets`. The larger goal remains open: every
future Fabric row must either gain the same product-surface proof before it is
marked supported or remain explicitly unsupported with a machine-readable
reason.
