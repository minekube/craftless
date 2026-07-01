# Headless Presentation Truth Evidence

Phase 197 fixes the false-headless fallback found during a manual macOS probe.

## Behavior

- `presentation.window = NONE` now requires a real windowless launcher
  strategy. Current supported strategies are a configured
  `CRAFTLESS_WINDOWLESS_WRAPPER` or auto-discovered Linux `xvfb-run`.
- If no strategy is available, Craftless rejects the launch before copying
  prepared mods, writing muted sound options, creating logs, or starting Java.
- `presentation.window = VISIBLE` still launches directly and bypasses any
  windowless wrapper.
- HeadlessMC research confirmed that a normal Minecraft launch is not a
  no-window strategy. Durable future options are virtual displays or
  Craftless-owned LWJGL/offscreen instrumentation.

## Verification

```sh
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.process client runtime launcher rejects windowless request when no windowless wrapper is available'
```

Result: passed after failing first against the stale direct-launch behavior.

```sh
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.server rejects default windowless client creation when no windowless wrapper is available'
```

Result: passed. `POST /clients` returned `400` with the windowless strategy
guidance and did not materialize muted options for the rejected client.

```sh
mise exec -- gradle :daemon:test
```

Result: passed.

```sh
mise run docs-site-verify
```

Result: passed.

```sh
git diff --check
```

Result: passed.

```sh
mise run package-cli
```

Result: passed.

## Packaged Smoke

The freshly packaged CLI was started from `build/docker/craftless/bin/craftless`
with:

```sh
CRAFTLESS_WINDOWLESS_WRAPPER=none build/docker/craftless/bin/craftless daemon start --host 127.0.0.1 --port 18101 --workspace build/craftless-packaged-latest-current-probe/workspace
```

Then:

```sh
build/docker/craftless/bin/craftless api /clients --api http://127.0.0.1:18101 -F id=headless-truth -F version=latest-release -F loader=FABRIC
```

Result: returned `BAD_REQUEST` with:

```json
{"code":"BAD_REQUEST","message":"windowless presentation requires a windowless launcher strategy; configure CRAFTLESS_WINDOWLESS_WRAPPER or create the client with presentation.window=VISIBLE"}
```

Fresh cleanup checks showed no listener on `18101`, no `headless-truth` client
in `GET /clients`, and no matching Minecraft/Knot client process.
