# Release Troubleshooting

How a Craftless release actually publishes, how to repair one that published
empty, and which releases are permanently beyond repair and why.

## The normal path

1. `release-please.yml` opens or updates the release PR on pushes to `main`,
   manual dispatch, and the weekly schedule.
2. Merging that PR makes release-please create the `vX.Y.Z` tag and the GitHub
   Release.
3. `release-please.yml`'s `trigger-release` job dispatches `release.yml` on the
   new tag.
4. `release.yml` runs `mise run ci`, packages the CLI archives and
   `SHA256SUMS`, publishes them onto the release, builds the per-arch runtime
   images, and publishes the multi-arch manifest.

Step 3 is load-bearing and easy to lose. release-please pushes the tag with
`GITHUB_TOKEN`, and GitHub's anti-recursion safeguard means a `GITHUB_TOKEN`
tag push does **not** fire `release.yml`'s `push: tags: ["v*"]` trigger. Without
`trigger-release`, the tag and the release page exist while `release.yml` never
runs - a green-looking release with zero assets and no image. Every craftless
release from `v0.2.0` onward that carries assets got them because a human
remembered to dispatch `release.yml` by hand.

## `:latest` only ever moves forward

`ghcr.io/minekube/craftless:latest` is a live pointer. `release.yml` asks GitHub
which release is newest (`gh release list --json isLatest`, not a tag-name sort,
which would order `v0.10.0` before `v0.9.0`) and only adds the `:latest` tag to
the manifest when the tag being published *is* that newest release.

For a normal release the condition is true by construction, so the published tag
set is unchanged. When it is false - re-running `release.yml` at an older tag -
the pointer is left where it is. That failure direction is deliberate: a stale
`:latest` is visible and self-correcting on the next release, a regressed
`:latest` silently downgrades every consumer.

## Repairing a release that published empty

`release-repair.yml` rebuilds a tagged release from its own source on CI runners
and uploads the missing assets.

```sh
gh workflow run release-repair.yml --repo minekube/craftless -f release_tag=v0.3.5
```

Two things about its shape are not incidental:

- **It lives on `main` and is dispatched on `main`, checking out the tag.**
  GitHub compiles a `workflow_dispatch` run from the workflow file *at the
  dispatched ref*, so a "repair" input added to `release.yml` would not exist
  when `release.yml` is dispatched at an old tag - the old file is what runs.
  Dispatching a default-branch workflow and checking out the tag is the only
  shape that reaches every tag, including ones cut before the workflow existed.
- **It holds no registry credential and requests no `packages: write`.** It
  therefore *cannot* move `:latest`, forwards or backwards. That is a capability
  boundary, not an `if:` a reviewer has to verify. Keep it that way: adding a
  `docker/login-action` or `packages: write` to this workflow removes the entire
  safety property.

It refuses to touch a release that already has both a CLI archive and
`SHA256SUMS`, never clobbers a good asset, verifies that any asset it keeps is
byte-identical to what it just built, uploads through `gh release upload` (never
through an action that `PATCH`es the release and rewrites historical notes), and
finally re-reads the *published* release from the API to prove a real archive is
downloadable. A repair that reports success on an empty release would be the
original bug one level up.

### Moving the container image after a repair

Deliberately a separate step, in this order:

1. Repair the assets. Confirm the run's `Verify published release assets` step
   passed.
2. Smoke the real artifact - install from the published tarball and exercise it.
3. Only then dispatch `release.yml` on that tag to build and publish the runtime
   image. `:latest` moves only if that tag is the newest release.

Step 3 re-publishes the release assets as a side effect of `release.yml`'s
`release` job. That is harmless - it is the same tagged source producing the
same archive names - but it is why the image move is worth doing knowingly
rather than as a reflex.

## The release commit must not invalidate its own gate

The failure below has now happened twice, in two different guards, and it is the
single most expensive shape in this repo's release history. Both times the
mechanism was identical: **a test asserts something about the version, and the
release commit changes the version without changing the thing the test reads.**
The tag is then red at itself, forever, so `release.yml`'s `Verify` step fails
and the release publishes empty.

- Old form: `distribution.test.ts` asserted the release-please manifest equalled
  a hard-coded *previous* version. Fixed for good in `v0.3.2` by
  `1eb7240 fix(ci): make release version guard dynamic`.
- New form: the Phase 210 release-truth guard asserts
  `docs/project-completion-checklist.md` names the manifest's version.
  release-please bumped the manifest and nothing else, so `v0.3.5` was red at
  `v0.3.5` - which is why `v0.3.5` published with no assets.

The fix is not to weaken the guard - the guard is right, and a checklist naming
a stale release is a real defect. The fix is to make the release commit carry
the checklist too, via `extra-files` in `release-please-config.json` plus
`<!-- x-release-please-version -->` annotations on the checklist rows that name
the tag. `distribution.test.ts` asserts that wiring directly, because
release-please's own silence when an annotation is missing is what makes this
class expensive.

**Before adding any test that reads a version, ask what bumps it.** If the
answer is "the release commit, and only the release commit", the test needs to
be release-please-managed or dynamic, or it will quietly cost the next release
its assets.

## Permanently unrepairable releases

`v0.2.0`, `v0.3.0` and `v0.3.1` publish no assets and **never will**. This is
not a backlog item; do not attempt to repair them.

Each of those tags carries a test that asserts the release-please manifest still
holds the *previous* version - the exact version the release commit at that tag
just bumped. `playwright/src/distribution.test.ts` at each tag:

| Tag | `.release-please-manifest.json` | The tag's own assertion |
| --- | --- | --- |
| `v0.2.0` | `{".": "0.2.0"}` | `expect(manifest["."]).toBe("0.1.2")` |
| `v0.3.0` | `{".": "0.3.0"}` | `expect(manifest["."]).toBe("0.2.0")` |
| `v0.3.1` | `{".": "0.3.1"}` | `expect(manifest["."]).toBe("0.3.0")` |

So `mise run ci` is red at those tags, deterministically, forever - it fails
before anything is built. That is not a flake and not an environment problem; it
is what the tagged source says. It is also what actually killed the manual
`release.yml` dispatch for `v0.3.1`
([run 28568835457](https://github.com/minekube/craftless/actions/runs/28568835457)).

The guard was made dynamic by `1eb7240 fix(ci): make release version guard
dynamic` and has been `expect(manifest["."]).toMatch(/^\d+\.\d+\.\d+$/)` since
`v0.3.2`, so no later tag has this problem.

`v0.3.5` is a different case and is **not** in this list. It is red at itself
for the Phase 210 reason above, which is a fixable defect fixed on `main` - not
an assertion that can never hold. Repairing it still requires deciding whether
the repair may build with `mise run package-cli` (which runs ~40 artifact
assertions against tagged source) without the repo-wide `mise run ci` that the
tag's own checklist row fails. That is a provenance-standard call, not an
engineering one, and it is not settled here.

Repairing the three tags above would mean either rewriting a published tag or
publishing artifacts that skipped the tag's own gate. Both are worse than an honest empty
release: a release page that says "no assets" is accurate, while a rewritten tag
breaks the one guarantee a tag makes. The reason is the useful artifact here,
which is why it is recorded rather than worked around.

## What this does not fix

Stated plainly, because it is what makes the rest trustworthy:

- **Repair still runs a build.** If the tagged source does not build, or its own
  gate is red, the repair fails loudly. Merging this tooling is not the same
  thing as a backfill, and the two should never be reported as one.
- **The newest-release oracle can be wrong.** If `gh release list --json
  isLatest` ever misreports, a legitimately newest release quietly does not move
  `:latest`. The pointer goes stale rather than backwards, and the next release
  corrects it - but it is a real failure mode, not an impossibility.
- **`release-repair.yml` duplicates `release.yml`'s build invocation** (`mise run
  ci` + `mise run package-cli` + `SHA256SUMS`). Two lines today, and they can
  drift. If they ever do, collapse them into a composite action rather than
  letting the repair path build something different from the release path.
- **Archives are not guaranteed byte-reproducible.** On a *partially* populated
  release the sha256 reconciliation of a retained asset can fail even though
  nothing is wrong beyond archive nondeterminism. It fails loudly and needs a
  human call; it does not silently publish a `SHA256SUMS` describing bytes
  nobody can download.
- **A repair still mutates a live release object.** It only fills holes and
  never rewrites notes, but it is a mutation and should be treated as one.
- **`trigger-release` fixes future releases, not past ones.** Releases already
  minted empty still need the repair path.
