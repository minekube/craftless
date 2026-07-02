# Active Release Truth Cleanup

Phase 210 cleans up stale release truth in the active completion board and adds
a guard so the same drift is caught by the distribution test suite.

## Why

`docs/project-completion-checklist.md` is the active handoff board for agents.
It still named `v0.3.2` as the latest published release even though
`.release-please-manifest.json`, the Git tag list, and GitHub Releases identify
`v0.3.4` as current. That makes future agents reason from stale release and CI
state.

This cleanup is product-aligned because it improves the control surface agents
read before continuing work. It does not change product behavior, add gameplay
shortcuts, or claim completion of the broader Fabric matrix proof.

## Changes

- Updated the active completion checklist to name published release `v0.3.4`.
- Added Phase 210 to the maintained phase index.
- Added a distribution guard that reads `.release-please-manifest.json` and
  fails unless `docs/project-completion-checklist.md` names the current `v*`
  release tag.

## Red-Green Evidence

Before the checklist update, the new guard failed for the intended stale-truth
reason:

```sh
mise exec -- bun test playwright/src/distribution.test.ts -t "active completion checklist names the current published release"
```

Failure:

```text
Expected to contain: "v0.3.4"
```

External release evidence:

```sh
gh release view v0.3.4 --repo minekube/craftless --json tagName,name,publishedAt,url,isDraft,isPrerelease,targetCommitish
```

Returned published, non-draft, non-prerelease release
`https://github.com/minekube/craftless/releases/tag/v0.3.4`.

After the checklist update:

```sh
mise exec -- bun test playwright/src/distribution.test.ts -t "active completion checklist names the current published release"
```

Passed with `1 pass`, `0 fail`, and `3 expect() calls`.

## Final Verification

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Passed with `21 pass`, `0 fail`, and `296 expect() calls`.

```sh
git diff --check
mise run ci
mise run docs-site-verify
```

`git diff --check` exited cleanly. `mise run ci` passed Gradle lint/tests,
packaged CLI smoke, CI smoke, and Bun Playwright tests with `28 pass`,
`0 fail`, and `315 expect() calls`. `mise run docs-site-verify` rebuilt the
Fumadocs static site, typechecked it, and completed a Wrangler dry-run deploy.
