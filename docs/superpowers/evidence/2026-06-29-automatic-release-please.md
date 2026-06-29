# Phase 190 Evidence: Automatic Release Please

## Scope

Phase 190 adds automatic Release Please release management in front of the
existing tag-driven Craftless artifact publisher.

## Product Surface

- `.github/workflows/release-please.yml` runs on pushes to `main`, manual
  dispatch, and a weekly cron schedule.
- `release-please-config.json` defines the root Craftless package with
  `release-type: simple` and `vX.Y.Z` tags.
- `.release-please-manifest.json` starts from the latest published release,
  `0.1.2`.
- `.github/workflows/release.yml` continues to publish CLI archives, checksums,
  Docker images, and GitHub releases for `v*` tags, now with generated release
  notes enabled.

## Verification

- `mise exec -- bun test playwright` passed with 24 tests and 215 assertions.
- `git diff --check` passed before full CI.
- `mise run ci` passed, including lint, detekt, Gradle tests,
  `ci-craftless-smoke`, and Playwright helper tests.
