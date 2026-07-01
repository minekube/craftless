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
```

Result: passed.

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Result: passed. The distribution tests assert the generic packaged Fabric lane
probe, the `1.21.6` current-lane task, the supported-matrix task, and the
scheduled/manual GitHub workflow.

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
git diff --check
```

Result: passed.

## Current Scope

The supported-matrix task currently composes:

- `mise run packaged-latest-current-probe` for `26.2`;
- `mise run packaged-current-lane-probe` for `1.21.6`;
- `mise run packaged-representative-older-probe` for `1.20.6`.

This proves the automation surface for every currently supported packaged row
reported by `/versions/support-targets`. The larger goal remains open: every
future Fabric row must either gain the same product-surface proof before it is
marked supported or remain explicitly unsupported with a machine-readable
reason.
