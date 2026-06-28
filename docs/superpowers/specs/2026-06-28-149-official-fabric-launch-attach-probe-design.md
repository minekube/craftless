# Official Fabric Launch Attach Probe Design

## Problem

The latest/current official Fabric lane now compiles, uses shared attach code,
and packages the runtime dependencies needed for metadata-only self-attach.
There is still no reusable probe that launches the official Fabric client lane
with a Craftless daemon attach environment and records whether the in-client
driver actually attaches.

Without this probe, later work can only prove compile/package boundaries, not
runtime launch/attach behavior.

## Goal

Add an opt-in diagnostic launch/self-attach probe for
`driver-fabric-official`. The probe should:

- start a local Craftless daemon with a real client record;
- launch the official Fabric `runClient` task with `CRAFTLESS_CLIENT_ID` and
  `CRAFTLESS_DAEMON_URL`;
- poll the daemon for `client.attached` and generated per-client OpenAPI;
- fetch daemon events and per-client OpenAPI while the launched client is still
  attached, before terminating the child process;
- write machine-readable evidence under
  `driver-fabric-official/build/craftless-official-attach-probe/`;
- fail with useful artifacts if attach is not observed before timeout.

## Non-Goals

- Do not add a packaged 26.x driver manifest entry.
- Do not claim latest/current support.
- Do not add gameplay actions, static catalogs, scenario shortcuts, or public
  route families.
- Do not require server join, chat, inventory, navigation, or gameplay in this
  phase.
- Do not put daemon/test probe dependencies into the official production mod
  jar.

## Design

Add a test-scope probe runner in `driver-fabric-official/src/test`. It may use
the daemon module and Ktor client because it is a diagnostic harness, not mod
runtime code. The production official jar remains governed by Phase 148.

Add a Gradle task `officialFabricAttachProbe` in `driver-fabric-official`.
The task is opt-in through `CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1`.
When enabled, it should default the client command to:

```sh
mise -C <repo> exec java@temurin-25.0.3+9.0.LTS gradle@9.6.0 -- \
  gradle -p <repo> :driver-fabric-official:runClient
```

The runner should allow overriding the command with
`CRAFTLESS_OFFICIAL_ATTACH_PROBE_CLIENT_COMMAND_JSON` so CI/local debugging can
swap in a controlled process when needed.

## Acceptance

- `driver-fabric-official` has an opt-in `officialFabricAttachProbe` Gradle
  task.
- The probe runner lives under `driver-fabric-official/src/test`.
- The production official jar does not include daemon/test probe classes.
- Architecture tests prove the task uses Java 25/mise and launches
  `:driver-fabric-official:runClient`.
- Unit tests prove the probe environment injects `CRAFTLESS_CLIENT_ID` and
  `CRAFTLESS_DAEMON_URL` into the launched process command.
- Probe guards prove per-client OpenAPI is captured before stopping the
  launched client, and expected output-stream closure during child shutdown
  does not produce noisy stack traces or false reader failures.
- Running the task without the opt-in environment skips safely.
- Running the opt-in task with the default `runClient` command observes
  `client.attached` and writes generated OpenAPI metadata for Minecraft `26.2`
  without claiming gameplay support.
- No public support claim, packaged manifest entry, or gameplay action is
  added.
