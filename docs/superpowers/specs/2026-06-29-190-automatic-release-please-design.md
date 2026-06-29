# Phase 190: Automatic Release Please Design

## Goal

Craftless should create release candidates automatically when `main` has
releasable changes since the last published `v*` release, without replacing the
existing tag-driven artifact publisher.

## Design

- Add a Release Please manifest workflow for the root Craftless package.
- Run it on pushes to `main`, manual dispatch, and a weekly cron backstop.
- Anchor the first manifest version at the latest existing tag, `v0.1.2`.
- Keep `.github/workflows/release.yml` responsible for publishing artifacts
  whenever a `v*` tag exists.
- Generate GitHub release notes in the tag publisher so Release Please-created
  tags and manual tags both produce useful release pages.

## Why This Shape

Release Please owns version calculation, changelog updates, release PRs, and tag
creation. The current release workflow already owns CI, CLI packaging,
checksums, and Docker publishing. Keeping those responsibilities separate avoids
a second ad hoc versioning system.

## Acceptance

- A scheduled workflow re-checks `main` for releasable changes.
- Release Please uses explicit config and manifest files checked into the repo.
- Merging the generated release PR creates `vX.Y.Z`, which triggers the existing
  release workflow.
- Distribution tests guard the workflow, config, manifest, README wording, and
  release-note publishing.
