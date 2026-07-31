# R4 Trigger-Release Bridge: Already Satisfied

## Conclusion

Backlog item `craftless-trigger-release-bridge-r4` is already satisfied on
`main`. Commit `6159f020727bdb71da98c777cad27f77e171deae`
(`fix: decouple historical release assets from latest image (#14)`) added the
same release-dispatch bridge used by GeyserLite. Reimplementing it would
duplicate an existing, tested workflow.

No release, deployment, merge, production mutation, credential widening, Gate
release decision, or Moxy-consumption change is part of this closure.

## Diagnostic Separation

- **Initiating trigger:** Release Please creates a `vX.Y.Z` tag with
  `GITHUB_TOKEN`.
- **Masking condition:** A human can manually dispatch `release.yml`, making an
  affected release look as though the tag-triggered path worked.
- **Visible symptom without the bridge:** GitHub creates the tag and release
  object, but `release.yml` does not run, leaving the release without generated
  assets or a published runtime image.

GitHub's anti-recursion behavior is the causal boundary: a tag pushed with
`GITHUB_TOKEN` does not start another workflow from the tag's `push` event.
Craftless closes that gap explicitly rather than relying on a personal access
token or manual dispatch.

## Equivalent Workflow Shape

GeyserLite commit `569f02dfeffa48a21c8c83b43aa8c40afcd29cba`
introduced this bridge:

1. grant `actions: write`;
2. export `release_created` and `tag_name` from the Release Please job;
3. run `trigger-release` only when `release_created == 'true'`;
4. dispatch `release.yml` at the newly created tag using
   `gh workflow run ... --ref <tag>`;
5. keep `workflow_dispatch` enabled on `release.yml`.

Craftless has every element:

- `.github/workflows/release-please.yml` grants `actions: write`;
- the `release-please` job exports `release_created` and `tag_name`;
- `trigger-release` depends on that job and checks `release_created`;
- the job dispatches `release.yml` with `--ref "$TAG_NAME"`;
- `.github/workflows/release.yml` accepts `workflow_dispatch`.

The existing distribution contract in
`playwright/src/distribution.test.ts` guards the permission, conditional job,
dispatch command, and tag ref.

## Historical and Live Proof

The bridge was absent in parent commit `6159f020^` and was added by
`6159f020`. This is the smallest counterfactual: without those workflow
outputs, permission, and dispatch job, the automated release path stops after
tag creation.

The first subsequent release proves the complete path:

- Release Please run
  [30245851131](https://github.com/minekube/craftless/actions/runs/30245851131)
  created `v0.3.6`.
- Its `trigger-release` job logged `Dispatching release.yml at v0.3.6` and
  returned release run URL
  [30245869303](https://github.com/minekube/craftless/actions/runs/30245869303).
- Release run `30245869303` used the `workflow_dispatch` event on branch
  `v0.3.6`; its release, amd64 image, arm64 image, and image-manifest jobs all
  completed successfully.

Disconfirming evidence would be a Release Please run with
`release_created=true` whose `trigger-release` job was skipped or failed, or a
resulting `release.yml` run on a ref other than the new tag. The `v0.3.6`
records show neither condition.

## Verification

The historical contract check against `6159f020^` exited nonzero because the
parent workflow contained neither the bridge job nor its dispatch command. The
same focused contract on the current tree passed:

```text
mise exec -- bun test playwright/src/distribution.test.ts \
  --test-name-pattern "scheduled Release Please workflow creates release tags from main changes"

1 pass
0 fail
19 expect() calls
```

`git diff --check` also passed. No production workflow needs to be dispatched
again: the recorded `v0.3.6` run is end-to-end proof from the real user path.
