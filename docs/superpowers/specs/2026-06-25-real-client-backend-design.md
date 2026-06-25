# Real Client Backend Design

Date: 2026-06-25

## Status

Legacy spike design. This document describes a Go process-backed backend that
wraps HeadlessMC/HMC-Specifics through stdin/stdout. That approach may still be
used for research, but it is no longer the target architecture.

The current architecture direction is the JVM-first rewrite in
`docs/superpowers/specs/2026-06-25-jvm-first-rewrite-design.md`. Treat this file
as prior research and a possible temporary spike plan only.

## Purpose

This spec defines the first real Minecraft Java client backend for Craftwright.
It turns the existing in-memory engine boundary into a process-backed engine
that can launch one offline Fabric client, connect it to a server, send chat,
wait for chat/log events, stop cleanly, and collect artifacts.

The goal is not to build the whole product in one step. The goal is to prove
the critical path with real Minecraft code while preserving the CLI-first,
agent-friendly contract already defined for `mcw`.

## Research Summary

Two subagents independently researched real-client automation prior art and both
recommended HeadlessMC plus HMC-Specifics as the first backend.

The strongest alternatives were:

- HeadlessMC plus HMC-Specifics: best match. It launches a real Minecraft Java
  client, supports offline/headless CI, and exposes in-client commands through a
  version-specific mod.
- PortableMC or cmd-launcher: useful launcher references, but they do not provide
  headless rendering patches or in-game control.
- PrismLauncher: mature launcher and offline/quick-play reference, but GPL-3,
  GUI-first, and not an automation/control foundation.
- MC-Runtime-Test: strong CI smoke-test reference, but scoped to mod/runtime
  tests rather than general player/server automation.
- Baritone and AltoClef: real client control references, but they do not solve
  launch, headless CI, or a general test harness.
- Browser clients and Mineflayer/Prismarine projects: useful UX/proxy references,
  but not real Minecraft Java clients.

## Upstream Evidence

HeadlessMC 2.9.0 is MIT licensed and was released on 2026-04-04. Its release
assets include `headlessmc-launcher-wrapper-2.9.0.jar`, native launchers, and the
LWJGL patch jar. Source revision inspected:
`0d06ba77a11549696ab925ce61554e02f84a0160`.

HMC-Specifics 2.4.0 is MIT licensed and was released on 2026-04-07. Its release
assets include `hmc-specifics-1.21.6-2.4.0-fabric-release.jar`, matching
Craftwright's current default Minecraft version and loader. Source revision
inspected: `4f2d867d9e9fc4b9172381e4f4fac8a2d7239243`.

Source facts that shape this design:

- HeadlessMC `LaunchCommand` exposes `-commands`, `-lwjgl`, `-inmemory`,
  `-quit`, `-offline`, `--jvm`, and `--retries`. It refuses to launch without an
  account unless offline mode is configured.
- HeadlessMC `ProcessFactory` downloads libraries/assets, instruments the
  classpath, can auto-download version-specific mods, and starts Minecraft with
  `ProcessBuilder`.
- HeadlessMC supports interactive mode and startup `--command <command>`.
- HeadlessMC docs list `-specifics` as the launch flag that automatically
  downloads HMC-Specifics, and show a Fabric/headless launch example using
  `-specifics -lwjgl`.
- HMC-Specifics documents `gui`, `click`, `text`, `render`, `connect`,
  `disconnect`, `msg`, `/`, and `quit`.
- HMC-Specifics registers those commands in `MinecraftContext`; scheduled
  commands run on the Minecraft thread.
- HMC-Specifics `connect` calls real Minecraft connection code through
  `ConnectScreen.startConnecting`.
- HMC-Specifics chat and slash commands route through `LocalPlayer.connection`.
- HMC-Specifics logs chat with a `[CHAT]` marker and exposes GUI/render text via
  text command output.

CLI design guidance also applies here:

- clig.dev emphasizes human-first output while preserving composability through
  stdout/stderr, exit codes, JSON, plain output, config precedence, and
  non-interactive behavior.
- Peter Steinberger's `create-cli` skill reinforces designing the command tree,
  output contracts, error codes, safety flags, and config/env precedence before
  implementation.

## Recommended Approach

Use HeadlessMC as an external process first. Do not embed HeadlessMC Java APIs in
the first backend.

Craftwright will download or locate the pinned HeadlessMC launcher wrapper,
prepare a per-profile HeadlessMC directory, spawn the wrapper as the supervised
process, write the launch command to stdin, then drive HMC-Specifics commands
through the same process input and parse process output into Craftwright events.

The first launch command written to the process is:

```sh
launch fabric:1.21.6 -offline -specifics -lwjgl --jvm "-Djava.awt.headless=true -Xmx2G"
```

The implementation plan should start with a small process-boundary spike that
compares two launch paths: interactive wrapper plus stdin command, and wrapper
startup `--command <launch command>`. The production backend should use the path
that preserves one supervised process handle, writable command input after
Minecraft is controllable, and complete artifact capture. Source evidence favors
interactive stdin launch because HeadlessMC waits for the child process and keeps
stdin open unless quit/test mode is active.

## Rejected First Approaches

### Embedded Java API

Embedding HeadlessMC APIs would avoid some log parsing, but it couples
Craftwright to HeadlessMC internals and forces the Go binary to supervise a JVM
with a tighter, harder-to-upgrade integration. It also raises classloader and
versioning complexity before the first smoke path exists.

Use this later only if the subprocess protocol cannot provide reliable launch
or shutdown semantics.

### Custom Launcher Core

Building a launcher around PortableMC, cmd-launcher, or PrismLauncher concepts
would create a cleaner programmatic model, but it would still need a headless
rendering strategy and an in-client control bridge. That makes it larger and
riskier than wrapping HeadlessMC first.

Use this later only if HeadlessMC blocks critical features.

### Browser Or Protocol Bot Backend

Browser clients, Mineflayer, and Prismarine clients are not acceptable as the
primary backend because they do not run a real Minecraft Java client. They may
be useful as test doubles, UX references, or compatibility probes.

## Backend Components

### Engine Selector

Add a backend selector behind `engine.Engine`.

- `memory`: current deterministic test engine.
- `headlessmc`: real client backend.

The default for CLI tests remains `memory` until real-client smoke tests are
explicitly opted in. The product default can switch to `headlessmc` after the
real smoke path is stable.

### Dependency Cache

`mcw cache prepare` should prepare the artifacts required for a profile:

- HeadlessMC launcher wrapper jar and checksum.
- HMC-Specifics jar for exact Minecraft version and loader.
- HeadlessMC working directory.
- Minecraft assets/libraries/modloader state through HeadlessMC prepare/launch
  behavior.
- Metadata file recording versions, URLs, checksums, and prepared time.

Craftwright must not vendor Minecraft client jars or Mojang assets. It can cache
files downloaded by official/upstream launch flows.

### Process Supervisor

The process supervisor owns one OS process per client.

For each client, it must track:

- Craftwright client name.
- Minecraft version, loader, offline mode, username, server, and artifact paths.
- `exec.Cmd`, stdin writer, stdout/stderr readers, process state, start time,
  stop time, and exit status.
- A bounded event buffer and log buffer.

The supervisor writes full stdout/stderr to artifact logs and converts selected
lines into structured events.

### Process Ownership Model

Real clients must be owned by a long-lived `mcw` process. A one-shot CLI process
cannot launch a client, return to the shell, and still safely supervise stdin,
stdout, shutdown, artifacts, and leaked child cleanup.

The first backend milestone therefore supports real clients in:

- `mcw scenario run`, because the scenario runner keeps one `mcw` process alive
  for the full scenario.
- `mcw daemon --stdio`, because SDKs, Playwright, and agents can keep the daemon
  process alive while issuing multiple operations.

Standalone multi-invocation commands such as `mcw client launch alice` followed
by `mcw client connect alice ...` require a persistent daemon transport or an
auto-started background daemon. That is a follow-on integration slice, not part
of the first process-backed engine. Until that exists, the implementation must
not create orphaned Minecraft processes to make one-shot CLI commands appear to
work.

### Command Bridge

Craftwright operations map to HMC-Specifics commands:

| Craftwright operation | HMC-Specifics command |
| --- | --- |
| `Connect(name, server)` | `connect <host> <port>` |
| `Chat(name, message)` | `. <message>` or `msg <message>` |
| `Command(name, command)` | `/<command without leading slash>` |
| `Stop(name, force=false)` | `quit`, then bounded process wait |
| `Stop(name, force=true)` | `quit`, then kill after timeout |
| `GUI(name)` | `gui` |
| `Render(name, duration)` | `render <milliseconds>` |

The first backend only needs `launch`, `connect`, `chat`, `wait --chat`, `logs`,
and `stop`. GUI/render commands remain design-compatible but can wait until the
chat smoke path is stable.

### Event Model

Craftwright should not expose raw HeadlessMC/HMC-Specifics text as the public
machine contract. It should map output into stable events:

```json
{"type":"client.state","client":"alice","state":"running"}
{"type":"client.log","client":"alice","level":"info","message":"..."}
{"type":"client.chat","client":"alice","message":"Welcome alice"}
{"type":"client.state","client":"alice","state":"connected","server":"localhost:25565"}
{"type":"client.state","client":"alice","state":"stopped"}
```

The first parser should recognize:

- Chat lines containing `[CHAT]`.
- HMC-Specifics connection intent lines such as `Connecting to server ...`.
- process start/exit as `running` and `stopped`.
- known auth/launch/cache failures as launch errors.

Connection readiness should not rely only on the command echo. The first smoke
test should wait for a server-side observable chat/log message when possible.
For Minekube tests, `wait --chat` is the first reliable user-facing condition.

### Historical Event Cursor

Current in-memory `Wait` scans retained events from the beginning. The real
backend should add an event cursor so `wait --chat` defaults to events observed
after the wait starts unless the caller explicitly asks for retained history.
This avoids false positives from earlier scenario steps.

### Artifacts

Each client gets an artifact directory:

```text
.craftwright/artifacts/<run-id>/<client>/
  craftwright-client.json
  stdout.log
  stderr.log
  events.jsonl
  headlessmc/
  minecraft/
  crash-reports/
```

`craftwright-client.json` records launch config, versions, checksums, process
exit status, and timestamps. Full raw logs stay available even if the public CLI
only prints compact summaries.

### Shutdown

Graceful stop sends `quit` and waits for a bounded timeout. If the process does
not exit, Craftwright kills it and marks the client as stopped with a forced flag
in the artifact metadata.

Interrupt handling must be crash-tolerant:

- First Ctrl-C: stop known clients with bounded cleanup.
- Second Ctrl-C or cleanup timeout: kill child processes and exit.
- On next run, stale metadata should be detected and either cleaned or reported.

## CLI And Daemon Contract

The existing `mcw` shape remains canonical. The backend must not leak extra
HeadlessMC commands as primary UX.

Important contracts:

- `mcw cache prepare --mc 1.21.6 --loader fabric`
- `mcw scenario run smoke.yaml`
- `mcw daemon --stdio`, with JSON-RPC calls for launch/connect/chat/wait/logs/stop
- future persistent CLI transport for `mcw client launch alice --offline`
  followed by separate `mcw client connect/chat/wait/logs/stop` invocations

Human output stays compact. Machine output uses the existing `--json`, `--jsonl`,
and daemon JSON-RPC contracts. Primary data stays on stdout; progress,
downloads, launch diagnostics, and errors stay on stderr.

Configuration precedence remains:

1. flags
2. environment variables
3. project `craftwright.yaml`
4. user config
5. built-in defaults

Proposed new config keys:

```yaml
version: 1
defaults:
  minecraft: "1.21.6"
  loader: "fabric"
  offline: true
  timeout: "2m"
backend:
  type: "headlessmc"
  headlessmcVersion: "2.9.0"
  specificsVersion: "2.4.0"
  java: "java"
  jvmArgs:
    - "-Djava.awt.headless=true"
    - "-Xmx2G"
paths:
  artifacts: ".craftwright/artifacts"
  cache: ".craftwright/cache"
```

## Testing Strategy

The implementation plan should use TDD with three test bands:

- Unit tests for config parsing, dependency metadata, command construction,
  output parsing, event cursor behavior, and artifact path layout.
- Integration tests with a fake process runner that simulates HeadlessMC output,
  process exit, hangs, and parseable chat lines.
- An opt-in real smoke test guarded by an environment variable such as
  `CRAFTWRIGHT_REAL_CLIENT=1`, using Minecraft `1.21.6`, Fabric, offline mode,
  HeadlessMC `2.9.0`, and HMC-Specifics `2.4.0`.

The first real smoke should run against a local offline-mode server that prints a
known welcome/chat line. Minekube Gate/Connect tests can build on that once the
client path is proven.

## Risks

- HMC-Specifics exposes a text command/log surface, not a structured event API.
  Keep parser logic isolated and covered by fixtures.
- Minecraft startup is slow and network/cache-dependent. Keep real smoke tests
  opt-in until CI caching is tuned.
- Version support is explicit. Start with `1.21.6` plus Fabric and expand only
  after tests prove each row.
- Headless rendering patches can break across LWJGL or Minecraft changes. Pin
  versions and checksums.
- Online-mode auth is out of scope for this backend milestone. Avoid flags for
  secrets when it is added later.

## Milestone Done Definition

This backend milestone is done when:

- `mcw cache prepare --mc 1.21.6 --loader fabric` prepares deterministic cache
  metadata for HeadlessMC and HMC-Specifics.
- `mcw scenario run` can launch a real Minecraft Java client through
  HeadlessMC/HMC-Specifics, connect, chat, wait for chat, collect logs, and stop
  within one long-lived process.
- daemon `client.launch` and `client.status` continue to work, and daemon methods
  for connect/chat/wait/logs/stop are added for long-lived SDK, Playwright, and
  agent control.
- standalone one-shot CLI commands do not claim persistent real-client behavior
  until a background daemon or TCP transport owns the client processes.
- artifacts include raw logs, structured events, process metadata, and crash
  reports if present.
- unit and fake-process integration tests pass by default.
- one opt-in real-client smoke path is documented and passes locally before CI
  automation is made mandatory.

## References

- HeadlessMC: https://github.com/headlesshq/headlessmc
- HeadlessMC release 2.9.0: https://github.com/headlesshq/headlessmc/releases/tag/2.9.0
- HMC-Specifics: https://github.com/headlesshq/hmc-specifics
- HMC-Specifics release 2.4.0: https://github.com/headlesshq/hmc-specifics/releases/tag/2.4.0
- MC-Runtime-Test: https://github.com/headlesshq/mc-runtime-test
- PortableMC: https://github.com/theorzr/portablemc
- cmd-launcher: https://github.com/telecter/cmd-launcher
- PrismLauncher: https://github.com/PrismLauncher/PrismLauncher
- Baritone: https://github.com/cabaletta/baritone
- AltoClef: https://github.com/gaucho-matrero/altoclef
- CLI Guidelines: https://clig.dev/
- Peter Steinberger agent scripts: https://github.com/steipete/agent-scripts
