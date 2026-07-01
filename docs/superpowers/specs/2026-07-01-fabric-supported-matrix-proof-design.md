# Fabric Supported Matrix Proof Design

## Problem

`GET /versions/support-targets` now reports every Fabric Minecraft target as
supported or unsupported. That makes the matrix visible, but support rows must
also have automated product-surface proof. The current packaged distribution
has supported driver lanes for `26.2`, `1.21.6`, and `1.20.6`; the repository
already has product probes for latest/current `26.2` and representative older
`1.20.6`, but the packaged `1.21.6` current lane is not a named matrix probe.

## Goals

- Add an automated packaged `1.21.6` Fabric lane probe that creates, attaches,
  connects, reads generated per-client OpenAPI/actions/resources, invokes a
  generated action, and records artifacts through public Craftless surfaces.
- Add a matrix task that runs the packaged probes for every currently
  supported Fabric target.
- Add a scheduled/manual GitHub workflow for the supported Fabric matrix.
- Keep unsupported Fabric targets explicit through `NO_DRIVER_MOD`; do not
  claim unsupported rows launch.
- Keep public gameplay and CLI surfaces generated/adaptive.

## Non-Goals

- Do not add static gameplay commands, public scenario APIs, or per-version
  route families.
- Do not mark all Fabric/Minecraft targets runnable in this phase.
- Do not copy driver source trees for each version.
- Do not make the normal push CI launch every supported Minecraft client.

## Acceptance Criteria

- `mise run packaged-current-lane-probe` exists and targets Minecraft `1.21.6`
  with Fabric Loader `0.19.3`.
- `mise run packaged-fabric-supported-matrix-probe` exists and runs the
  supported packaged probes for `26.2`, `1.21.6`, and `1.20.6`.
- `.github/workflows/fabric-support-matrix.yml` runs the matrix task on
  `workflow_dispatch` and a cron schedule.
- Playwright distribution tests assert the new task and workflow are present.
- Evidence records focused local verification and explains that this proves
  all currently supported rows, while broader full-matrix support remains open
  until additional rows are supported and probed.
