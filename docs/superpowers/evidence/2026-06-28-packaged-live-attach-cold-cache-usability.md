# Packaged Live Attach And Cold-Cache Usability Evidence

## Reproduction

The first packaged live attach smoke used:

```sh
mise run package-cli
CRAFTLESS_FABRIC_DRIVER_MOD=build/docker/craftless/mods/craftless-driver-fabric.jar \
  build/docker/craftless/bin/craftless server start --port 18081 --workspace /tmp/craftless-packaged-attach.../workspace
build/docker/craftless/bin/craftless clients create attach-smoke --version 1.21.6 --loader fabric --offline-name AttachSmoke --api http://127.0.0.1:18081
```

It failed before the fix with:

```text
error: Request timeout has expired [url=http://127.0.0.1:18081/clients, request_timeout=300000 ms]
```

While the request was timing out, the workspace kept growing from asset
downloads, proving the blocker was cold-cache preparation latency rather than a
Fabric self-attach failure.

## Fixes

- CLI API calls now use Ktor `HttpTimeout` with a 15-minute default and
  `CRAFTLESS_HTTP_REQUEST_TIMEOUT_MS` override.
- Cache preparation now fetches independent Minecraft asset objects with
  bounded concurrency while reusing the existing checksum/resume writer.

## Focused Verification

```sh
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest.cache preparation fetches independent asset objects concurrently' :cli:test --tests 'com.minekube.craftless.cli.CraftlessCliTest.client create uses configured api request timeout'
```

Result: passed.

```sh
mise run package-cli
```

Result: passed.

## Live Packaged Smoke

The packaged smoke was rerun on the same warmed workspace after rebuilding the
package.

`clients create` succeeded and returned:

```json
{"id":"attach-smoke","state":"RUNNING"}
```

The supervisor events included:

```json
{"type":"client.created","client":"attach-smoke"}
{"type":"client.attached","client":"attach-smoke"}
```

The client log showed the in-client Ktor loopback endpoint:

```text
Responding at http://127.0.0.1:50499
```

Packaged CLI/API projections worked:

```sh
build/docker/craftless/bin/craftless clients attach-smoke actions --api http://127.0.0.1:18081
build/docker/craftless/bin/craftless clients attach-smoke resources --api http://127.0.0.1:18081
curl -sS http://127.0.0.1:18081/clients/attach-smoke/events:stream
```

Observed output included runtime-probe actions/resources and SSE lifecycle
events, including `client.created` and `client.attached`.

The client was stopped through packaged CLI:

```sh
build/docker/craftless/bin/craftless clients attach-smoke stop --api http://127.0.0.1:18081
```

Result: returned `state":"STOPPED"`.

## Scope Guard

This phase added no public gameplay action, generated route family, CLI
gameplay catalog, Fabric execution binding, scenario shortcut, compiled lane,
public version-specific API, or Minecraft support claim.
