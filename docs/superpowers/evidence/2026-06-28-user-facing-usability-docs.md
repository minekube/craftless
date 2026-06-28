# CL-05 User-Facing Usability Docs Evidence

Gate: CL-05 external-user and agent usability.

Result: closed for this gate only. This evidence does not close CL-06, CL-07,
CL-08, or final project completion.

## Scope

CL-05 proves that external users and agents can install, run, inspect, stream,
invoke, and debug Craftless from the packaged surfaces and current docs without
reading source.

The gate keeps the core design invariant intact:

- gameplay workflows are generated from live per-client OpenAPI and runtime
  graph projection;
- `/clients/{id}/actions` and `/clients/{id}/resources` remain projection
  evidence, not the source of truth;
- CLI gameplay help remains adaptive;
- no static gameplay SDK, scenario shortcut, server-provisioned inventory, old
  `craftwright` brand, `.dev` domain, Homebrew path, or active TypeScript SDK
  is presented as the user path.

## Red And Green Checks

### CLI group help

Regression test added:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.clients help prints stable and adaptive command guidance*'
```

Red result before implementation: failed because `craftless clients --help`
returned an unknown command.

Green result after implementation: `BUILD SUCCESSFUL`.

The new help output includes stable lifecycle/discovery commands and explicitly
points generated gameplay commands at each live client's OpenAPI document.

### README freshness guard

Distribution guard added:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Red result before README refresh: failed on stale latest/current wording.

Green result after README/roadmap refresh: `15 pass`, `0 fail`, `138 expect()`
calls.

## Package Build

Command:

```sh
mise run package-cli
```

Result: exit `0`.

Important output:

```text
BUILD SUCCESSFUL in 11s
BUILD SUCCESSFUL in 544ms
BUILD SUCCESSFUL in 13s
```

The package build restaged:

- `build/docker/craftless/mods/craftless-driver-fabric.jar`
- `build/docker/craftless/mods/fabric-1.20.6/craftless-driver-fabric.jar`
- `build/docker/craftless/mods/fabric-26.2/craftless-driver-fabric-official.jar`
- `build/docker/craftless/bin/craftless`

## Install Smoke

Command:

```sh
tmp="$(mktemp -d)"
CRAFTLESS_VERSION=v0.1.1 \
  CRAFTLESS_INSTALL_DIR="$tmp/bin" \
  CRAFTLESS_HOME="$tmp/home" \
  sh ./install.sh > "$tmp/install.log"
"$tmp/bin/craftless" server start --once --port 0 --workspace "$tmp/workspace"
```

Result: exit `0`.

Output:

```json
{"ok":true,"url":"http://127.0.0.1:63796","openapi":"/openapi.json","events":"/events","workspace":"/var/folders/1y/cjgf53nj31n_dxsspqnjfjvc0000gn/T/tmp.HKfmFfJ4PE/workspace"}
```

Installer output:

```text
craftless 0.1.1 installed to /var/folders/1y/cjgf53nj31n_dxsspqnjfjvc0000gn/T/tmp.HKfmFfJ4PE/bin/craftless
add /var/folders/1y/cjgf53nj31n_dxsspqnjfjvc0000gn/T/tmp.HKfmFfJ4PE/bin to PATH if craftless is not found
```

## Docker Runtime Smoke

Commands:

```sh
docker version --format '{{.Server.Version}}'
docker build -t craftless:cl05 .
docker run --rm craftless:cl05 /opt/craftless/bin/craftless server start --once --port 0 --workspace /tmp/craftless
```

Results:

```text
29.2.1
```

```text
naming to docker.io/library/craftless:cl05 done
```

```json
{"ok":true,"url":"http://127.0.0.1:46369","openapi":"/openapi.json","events":"/events","workspace":"/tmp/craftless"}
```

The Dockerfile copies `build/docker/craftless/` into `/opt/craftless/`; it does
not build Craftless inside the runtime image.

## Packaged CLI Help Smoke

Commands:

```sh
build/docker/craftless/bin/craftless --help
build/docker/craftless/bin/craftless clients --help
build/docker/craftless/bin/craftless clients sample run --help
```

Root help output includes:

```text
Generated gameplay commands are loaded from each live client's OpenAPI document.
Use `craftless clients <id> <resource...> <action> --help` after client discovery for action help.
```

`clients --help` output includes:

```text
Usage: craftless clients <command> [args]
clients create
clients <id> run <action>
clients <id> <resource...> <action>
Generated gameplay commands are loaded from each live client's OpenAPI document.
```

The generated action help smoke without a running daemon returned the expected
network failure:

```text
error: Connection refused
```

That confirms the command path is generated/live-OpenAPI based, not a
non-network static gameplay catalog.

## Stale Wording Guard

Command:

```sh
rg -n "gameplay actions still empty|latest/current compatibility work|setup-craftless@v0.1.0|minekube\\.dev|craftwright|Craftwright|brew install|Homebrew" \
  README.md docs/roadmap.md docs/agent-skills.md \
  .agents/skills/craftless-public-gameplay-agent/SKILL.md .github/actions/setup-craftless/action.yml \
  install.sh Dockerfile docker/entrypoint.sh -S
```

Result: exit `1`, no matches.

## Docs And Agent Surfaces

Updated user-facing docs now describe:

- install script quickstart;
- packaged CLI;
- Docker runtime image;
- reusable GitHub Action;
- supervisor OpenAPI;
- generated per-client OpenAPI;
- adaptive CLI;
- SSE;
- JSON-RPC query, subscription, and invocation;
- packaged latest/current `26.2` and representative older `1.20.6` evidence;
- CL-06 through CL-08 as still-open gates.

The repo-local public gameplay agent skill remains aligned with generated
OpenAPI/SSE/JSON-RPC composition, missing-primitive reporting, and the ban on
scenario shortcuts.

## Focused Verification

Commands:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.clients help prints stable and adaptive command guidance*'
mise exec -- bun test playwright/src/distribution.test.ts
git diff --check
```

Results:

- Gradle focused CLI test: exit `0`, `BUILD SUCCESSFUL in 977ms`.
- Bun distribution guard: exit `0`, `15 pass`, `0 fail`, `138 expect()`
  calls.
- `git diff --check`: exit `0`, no output.
