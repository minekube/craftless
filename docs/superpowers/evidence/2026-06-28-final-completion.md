# CL-08 Final Completion Evidence

Gate: CL-08 publish completed state.

Result: closed after CL-07 implementation and evidence were committed and
pushed to `main`.

## Pushed CL-07 Commit

```text
b0bc559b801bb97adcd4de84c54769cffe4d3d29 feat: complete public gameplay probe
```

Push result:

```text
To github.com:minekube/craftless.git
   7f8b70de..b0bc559b  main -> main
```

Post-push branch state before this final evidence commit:

```text
## main...origin/main
```

## Verification Carried Into The Commit

The pushed CL-07 commit includes evidence for:

- final public gameplay probe passing through generated OpenAPI/JSON-RPC only;
- server provisioning disabled;
- no `task.*` or scenario shortcut use;
- useful material discovery, pickup, recipe query, craft, equip, block
  interaction, and entity attack through public primitives;
- final focused guards passing.

See:

```text
docs/superpowers/evidence/2026-06-28-final-public-gameplay.md
docs/project-completion-checklist.md
docs/superpowers/phase-index.md
```

## Final Post-Push Check

Final correction commit:

```text
2eb73033 chore: close final checklist corrections
```

That commit includes:

- the final checklist rewrite;
- ktlint formatting corrections found by `mise run ci`;
- removal of the remaining previous-brand literal from CL-05 evidence;
- the shared `world.time.query` operation id flow from capability probe into
  client-state discovery.

Verification after those corrections:

```text
mise run ci
BUILD SUCCESSFUL
23 pass
0 fail
198 expect() calls
```

```text
git diff --check
```

Result: exit `0`, no output.

```text
mise exec -- gradle :driver-fabric:test --tests '*completion gate does not accept unsupported version lanes as support'
BUILD SUCCESSFUL in 1s
```

Post-push status after `2eb73033`:

```text
## main...origin/main
```

After committing this final evidence/checklist index update, run:

```sh
git push origin main
git status --short --branch
```

Closure requires the final status to print:

```text
## main...origin/main
```
