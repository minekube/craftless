# Installed CLI Driver Mod Distribution Design

## Problem

Phase 102 proved the packaged Docker-style runtime path by running from
`build/docker/craftless`, where `mods/craftless-driver-fabric.jar` is staged
and `CRAFTLESS_FABRIC_DRIVER_MOD` is set by the Docker image.

The normal release tar/zip installed by `install.sh` and the reusable GitHub
Action still contains only the Gradle CLI distribution. It does not include
the staged Fabric driver mod. Installed CLI users can start the supervisor, but
daemon-managed Fabric clients cannot automatically load the in-client driver
unless the user manually provides `CRAFTLESS_FABRIC_DRIVER_MOD`.

## Goals

- Include `mods/craftless-driver-fabric.jar` in the normal CLI tar/zip
  distribution.
- Keep Docker packaging using the same staged driver mod artifact.
- Make `craftless server start` use an explicit
  `CRAFTLESS_FABRIC_DRIVER_MOD` when set, otherwise auto-discover
  `mods/craftless-driver-fabric.jar` relative to the installed CLI
  distribution.
- Keep this as distribution/runtime wiring only.

## Non-Goals

- Do not add public gameplay actions, static descriptors, route families, CLI
  gameplay catalogs, Fabric bindings, scenario shortcuts, or version support
  claims.
- Do not change the Fabric driver public API shape.
- Do not introduce non-Ktor HTTP code or non-mise dependency flows.

## Acceptance Criteria

- A policy test proves `package-cli` stages the Fabric driver mod into both the
  normal CLI distribution and Docker context.
- A CLI test proves `server start` can create a Fabric client using the
  auto-discovered distribution-local driver mod when the explicit environment
  variable is absent.
- `mise run package-cli` produces tar/zip archives containing
  `mods/craftless-driver-fabric.jar`.
- Package smoke still verifies the staged driver mod has Fabric metadata and
  nested runtime jars.
- README and roadmap explain that install script and GitHub Action users get
  the packaged driver mod automatically after this release path lands.
