# Phase 190: Automatic Release Please Plan

## Objective

Add scheduled Release Please automation while preserving the existing
tag-triggered artifact release pipeline.

## Steps

1. Add `.github/workflows/release-please.yml` with `workflow_dispatch`, `push`
   to `main`, and weekly `schedule` triggers.
2. Add `release-please-config.json` and `.release-please-manifest.json`,
   rooted at package `"."` and current version `0.1.2`.
3. Update `.github/workflows/release.yml` to generate GitHub release notes for
   tag releases.
4. Document the release lifecycle in the README.
5. Add distribution-surface tests for the workflow, config, manifest,
   changelog, release notes, and README wording.
6. Run focused verification, then commit and push `main`.

## Verification

- `mise exec -- bun test playwright`
- `git diff --check`
- `git status --short --branch`
