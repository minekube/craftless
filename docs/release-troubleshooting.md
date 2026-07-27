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

## The current release workflow protects the live image pointer

`ghcr.io/minekube/craftless:latest` is a live pointer. The `release.yml` on
`main` asks GitHub which release is newest (`gh release list --json isLatest`,
not a tag-name sort, which would order `v0.10.0` before `v0.9.0`) and only adds
the `:latest` tag to the manifest when the tag being published *is* that
newest release.

For a normal release the condition is true by construction, so the published tag
set is unchanged. When a workflow carrying this gate sees a tag that is not the
newest release, the pointer is left where it is. That failure direction is
deliberate: a stale `:latest` is visible and self-correcting on the next release,
a regressed `:latest` silently downgrades every consumer.

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

## Image movement is outside release repair

`release-repair.yml` publishes ASSETS ONLY. It holds no registry credential and
requests no registry scope, so it structurally cannot move
`ghcr.io/minekube/craftless:latest`. A repaired release gets its downloadable
build back; the container image is untouched and stays where it is.

A historical tag's image move cannot be performed by dispatching that tag's
own `release.yml`. GitHub compiles a `workflow_dispatch` run from the workflow
file **at the dispatched ref**, so an old tag runs its own pre-change file,
which retags `:latest` unconditionally - a production downgrade. The evidence
is in the historical workflow files: `v0.3.2`'s `release.yml` tags `:latest`
inside the build-push step (line 78), while `v0.3.4` and `v0.3.5` carry an
unconditional `--tag "${REGISTRY_IMAGE}:latest"` in the imagetools step (line
125). The newest-release gate added by this change exists only on `main`, so
only tags cut after it merges carry it.

Moving the image for an old tag would require a separately authorized,
credential-holding workflow on the default branch that checks out the tag and
applies the newest-release oracle. That is deliberately not built here. It
is a durable production-mutating capability and needs its own scoped decision;
it must not be arrived at by attrition through a runbook step.

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

## Releases that cannot be rebuilt

`v0.2.0`, `v0.3.0` and `v0.3.1` publish no downloadable build and never will.
This is not a backlog item; do not attempt to repair them.

**Read the reason precisely, because the obvious reading is wrong.** The
software at these tags is fine. Nothing about the product failed, no test found
a defect, and no build is broken. Each release carries no downloadable build
because a **self-invalidating metadata guard inside that tag prevents a clean
rebuild** - a check that tests nothing about the software and cannot pass under
any circumstances.

The guard asserts that `.release-please-manifest.json` still holds the
*previous* version - the exact version the release commit at that tag just
bumped. `playwright/src/distribution.test.ts` at each tag:

| Tag | `.release-please-manifest.json` | The tag's own assertion |
| --- | --- | --- |
| `v0.2.0` | `{".": "0.2.0"}` | `expect(manifest["."]).toBe("0.1.2")` |
| `v0.3.0` | `{".": "0.3.0"}` | `expect(manifest["."]).toBe("0.2.0")` |
| `v0.3.1` | `{".": "0.3.1"}` | `expect(manifest["."]).toBe("0.3.0")` |

The assertion compares one bookkeeping file against a hard-coded number. It
exercises no product code, so it can report nothing about product quality. It
was already false at the instant the tag was cut, and it will be false forever.
That is what stops `mise run ci`, before anything is built - which is also what
killed the manual `release.yml` dispatch for `v0.3.1`
([run 28568835457](https://github.com/minekube/craftless/actions/runs/28568835457)).

So the accurate sentence about these releases is: *the software is fine; the
release carries no downloadable build because a self-invalidating metadata guard
in that tag prevents a clean rebuild.* Do not describe them as failing quality
checks, failing tests, or having a broken build. They did not fail review - they
inherited an unpassable check, which is the same defect class this document
exists to eliminate.

The guard was made dynamic by `1eb7240 fix(ci): make release version guard
dynamic` and has been `expect(manifest["."]).toMatch(/^\d+\.\d+\.\d+$/)` since
`v0.3.2`, so no later tag inherits this exact assertion.

Repairing these three would mean either rewriting a published tag or bypassing
the tag's own test run. Both are worse than an honest empty release: a release
page that says "no assets", with the reason recorded here, is accurate, while a
rewritten tag breaks the one guarantee a tag makes. The reason is the useful
artifact, which is why it is recorded rather than worked around.

### `v0.3.5`, and the standing rule for repairs

`v0.3.5` is the same shape from a different guard - the Phase 210 release-truth
check above - and the same honest sentence applies: the software is fine, and
the release carries no downloadable build because a self-invalidating metadata
guard in that tag prevents a clean rebuild. It differs from the three above only
in that the guard is fixable, and is fixed on `main`.

Two rules govern any attempt to repair it:

1. **The repair path never gains a general test-bypass.** No `skip_tests` input,
   no "build-only" mode, no flag that runs `mise run package-cli` without the
   tag's test run. A repair mechanism that can skip a tag's own tests is a
   durable supply-chain capability that will be reached for again, by someone
   with less context, on a tag where the failing test *does* test software. The
   answer to that is no, permanently - which is why `release-repair.yml` runs
   the full `mise run ci` and offers no way around it.
2. **The only acceptable narrower shape is a named exclusion with evidence**,
   and only for `v0.3.5`. That means: run the build and every test that exercises
   the software, and treat exactly **one specifically-named metadata test** as
   not-applicable-for-repair, accompanied by the proof that it is deterministically
   red for bookkeeping reasons rather than software reasons. One named test, with
   evidence - not a category, not a pattern, not a flag.

If the repair cannot be expressed that narrowly, the answer is no and `v0.3.5`
stays empty-with-reason. **All four craftless releases standing as
empty-with-reason is a fully acceptable end state**, and a better one than a
repair path that can silently skip tests.

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
