# Shipped Docker Image Runs The Newest Packaged Lane

The released Docker image could not run Minecraft `26.2`, the newest lane it
packages, and the failure was silent. This phase fixes lane selection in the
image, makes a dead client runtime impossible to report as `RUNNING`, and adds
a CI check that runs the shipped image itself.

## Why

`Dockerfile` set `CRAFTLESS_FABRIC_DRIVER_MOD=/opt/craftless/mods/craftless-driver-fabric.jar`
unconditionally. That path is the `1.21.6` Yarn lane jar, and in
`cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt` a preset
`CRAFTLESS_FABRIC_DRIVER_MOD` short-circuits packaged driver manifest
discovery, so `/opt/craftless/driver-mods.json` was never configured and every
client got the `1.21.6` jar. On `26.2` Fabric Loader refused to start:

```text
Mod 'Craftless Driver Fabric 1.21.6 compiled lane' (craftless-driver-fabric) 0.1.0-SNAPSHOT
requires version 1.21.6 of 'Minecraft' (minecraft), but only the wrong version is present: 26.2!
```

The client never attached, and `POST /clients` still answered `state: RUNNING`.
Nothing in CI could see it because every packaged probe runs the CLI tarball,
not the image.

Reproducing it here showed why the failure was so quiet. Under `xvfb-run` the
client has a display, so Fabric Loader did not exit on the rejected mod: it
opened its error window and waited for a human who does not exist. The process
stayed alive for as long as it was left running, which is why a launched-process
check alone still reported `RUNNING`.

## Removal, Not Fallback

The environment variable is already a fallback by design: the manifest wins
whenever `CRAFTLESS_DRIVER_MOD_MANIFEST` is configured
(`ConfiguredClientRuntimeDriverModProvider.modsFor`), and
`docs/superpowers/specs/2026-06-28-108-driver-mod-manifest-provider-design.md`
keeps the single-jar variable only "for current single-driver setups". The bug
was never the precedence rule; it was that the image opted itself out of
manifest discovery.

Demoting the variable inside `Main.kt` instead would have removed the only way
an operator can force a custom driver build, and would leave a distribution that
ships three lanes still shipping a lane pin. So the `ENV` line is removed and
the variable stays an explicit operator override.

## Changes

- `Dockerfile` no longer sets `CRAFTLESS_FABRIC_DRIVER_MOD`; the image resolves
  each client's lane from the packaged `driver-mods.json`.
- Windowless Fabric clients launch with `-Dfabric.noGui=true`, so a loader that
  rejects the driver mod fails the process instead of waiting forever on a
  window nobody can see. A visible client keeps its loader windows.
- `ClientState.FAILED` is added. A launched client runtime that exits on its own
  is reported `FAILED` with the exit code and client log tail;
  `POST /clients` answers `CLIENT_RUNTIME_FAILED` (HTTP 502) instead of
  `201 Created`, and `:attach`/`:connect` refuse a failed client.
- Client creation waits for the launched process to survive startup
  (`CRAFTLESS_CLIENT_STARTUP_PROBE_MS`, default 8000ms), so the create response
  itself reports a loader hard-fail.
- `scripts/docker-image-latest-lane-probe.sh` plus
  `.github/workflows/docker-image-lane-check.yml` build the repository image and
  drive create/attach/connect on the newest lane in the packaged manifest until
  the Minecraft server reports the player joined the game.

## Red-Green Evidence

Both runs used the repository image built from this worktree. The host is
arm64, so the image was pinned to `linux/amd64`; Mojang ships client natives for
`linux/x64` only.

Red, with the removed pin put back through the environment, which is exactly
what the released image did:

```sh
docker run -d --name craftless-negctl2 --platform linux/amd64 \
  -e CRAFTLESS_FABRIC_DRIVER_MOD=/opt/craftless/mods/craftless-driver-fabric.jar \
  -v craftless-negctl-ws:/var/lib/craftless craftless-docker-lane-probe:local
docker exec craftless-negctl2 /opt/craftless/bin/craftless api /clients \
  --api http://127.0.0.1:8080 -f id=negpin -f version=26.2 -f loader=FABRIC \
  -f 'profile[kind]=OFFLINE' -f 'profile[name]=NegPin'
```

The call now fails instead of reporting a running client (exit 1):

```json
{"code":"CLIENT_RUNTIME_FAILED","message":"client negpin runtime exited with code 1 before the in-client driver attached; last client log lines: ... Mod 'Craftless Driver Fabric 1.21.6 compiled lane' (craftless-driver-fabric) 0.1.0-SNAPSHOT requires version 1.21.6 of 'Minecraft' (minecraft), but only the wrong version is present: 26.2! ..."}
```

`GET /clients/negpin` reports `"state":"FAILED"`, `POST /clients/negpin:connect`
answers `502`, and `/events` carries a `client.failed` entry. The probe treats
that answer as a product failure and stops without retrying, so this defect
fails the check on the first attempt.

Green, the shipped image with no driver mod override:

```sh
mise run package-cli
CRAFTLESS_DOCKER_PROBE_PLATFORM=linux/amd64 bash scripts/docker-image-latest-lane-probe.sh
```

The probe selected `26.2` from the packaged manifest, created the client with
the shipped default presentation, waited for `client.attached`, connected to the
`26.2` server container, and read the join from the server itself
(`build/craftless-docker-image-lane-probe/artifacts/`):

```text
[11:16:16] [Server thread/INFO]: DockerLane[/192.168.163.3:38660] logged in with entity id 1 at (10.5, -60.0, 10.5)
[11:16:16] [Server thread/INFO]: DockerLane joined the game
```

```json
{
  "status": "joined",
  "image": "craftless-docker-lane-probe:local",
  "minecraftVersion": "26.2",
  "clientState": "RUNNING",
  "serverJoinLine": "[11:16:16] [Server thread/INFO]: DockerLane joined the game",
  "actionCount": 1,
  "resourceIds": ["runtime","registry","event","client","player","inventory","recipe","world","world.block","world.time","entity","screen"]
}
```

The client log shows the lane the manifest chose, with no loader window to wait
on:

```text
Loading Minecraft 26.2 with Fabric Loader 0.19.3
craftless-driver-fabric-official
```

Teardown is verified rather than assumed; the run's
`teardown-verification.txt` reports `containers: none`, `volumes: none`,
`networks: none`.

Two limits worth stating. `clientState` is `RUNNING` rather than `CONNECTED`
because the daemon already records `client.connect.unobserved` for a connect it
cannot observe; the server-side join is the proof here, as it is for the
packaged probes. And the run needed `--platform linux/amd64` on this arm64 host
because Mojang ships client natives for `linux/x64` only, which is why the
workflow runs on `ubuntu-latest`.

The probe's narrow create retry is intentional: it retries only non-product
artifact-resolution failures, including interrupted Mojang downloads that
surface as `BAD_REQUEST` / `Not enough data available`; runtime failures and
unsupported targets fail on the first attempt. Better daemon retry and error
mapping for interrupted downloads remains a separate follow-up.

## Final Verification

```sh
mise run ci
```

Passed: Gradle lint, unused-check, `gradle test`, the packaged CI smoke
(`Craftless CI smoke passed`), and Bun Playwright with `34 pass`, `0 fail`,
`431 expect() calls`.

The new daemon coverage is in
`daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`:
a client whose launched process exits is answered with `CLIENT_RUNTIME_FAILED`,
reports `FAILED`, refuses `:connect`, and emits `client.failed` instead of
`client.created`; a windowless Fabric launch carries `-Dfabric.noGui=true` and a
visible one does not. `playwright/src/distribution.test.ts` fails if the
`Dockerfile` regains an `ENV CRAFTLESS_FABRIC_DRIVER_MOD` line, or if the
Docker image check stops building the image, waiting for attach, or asserting
the server-side join.
