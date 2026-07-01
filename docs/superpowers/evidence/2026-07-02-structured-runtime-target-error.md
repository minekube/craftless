# Structured runtime target error evidence

Phase 206 makes unsupported Fabric runtime target rejections machine-readable
without parsing `message`.

## Behavior

- `UNSUPPORTED_RUNTIME_TARGET` responses now include a `details` object.
- `details.reason` contains the `FabricSupportReason` value.
- Runtime identity fields include `loader`, `minecraftVersion`, optional
  `loaderVersion`, optional deeper lane metadata, and
  `availableLoaderVersions`.
- Ordinary errors still use the existing `code` and `message` shape without
  emitting empty details.
- Stable supervisor OpenAPI error schemas describe optional `details`.

## Red

```bash
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.prepared runtime rejects non discoverable Fabric loader version with matrix reason'
```

Failed before implementation because the response body had no `details`
object.

```bash
mise exec -- gradle :protocol:test --tests 'com.minekube.craftless.protocol.OpenApiGenerationTest.stable routes describe machine readable error responses'
```

Failed before implementation because the error schema only described `code`
and `message`.

## Green

```bash
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.prepared runtime rejects non discoverable Fabric loader version with matrix reason'
mise exec -- gradle :protocol:test --tests 'com.minekube.craftless.protocol.OpenApiGenerationTest.stable routes describe machine readable error responses'
mise exec -- gradle :protocol:test
mise exec -- gradle :daemon:test
```

All passed.
