# API-Only CLI Evidence

Phase 193 removes generated route subcommands from the `craftless` CLI and
keeps `craftless api <endpoint>` as the route invocation surface.

## Behavior

- `craftless --help` lists `api <endpoint>` and `daemon start`.
- `craftless api /version --api <url>` performs a `GET`.
- `craftless api /clients -F id=bot -F version=latest-release -F loader=FABRIC`
  infers `POST` and sends a JSON request body.
- Nested fields such as `args[message]=hello` produce nested JSON bodies for
  `POST /clients/{id}:run`.
- Per-client generated routes such as `POST /clients/{id}/player:chat` are
  invoked through `craftless api /clients/<id>/player:chat`, with operation
  matching loaded from the live per-client OpenAPI document.
- `craftless api <endpoint> --help` prints route, summary, description, and
  schema-derived field details including required fields, defaults, and enums.
- `x-craftless-cli` is no longer serialized in OpenAPI.
- `GeneratedRouteCli.kt` was removed.

## Red Evidence

`mise exec -- gradle :cli:cleanTest :cli:test --tests com.minekube.craftless.cli.CraftlessCliTest`
failed before implementation because `api` was not registered and API route
requests returned unknown-command exits.

`mise exec -- gradle :protocol:test --tests com.minekube.craftless.protocol.OpenApiGenerationTest`
failed before implementation because supervisor OpenAPI still serialized
`x-craftless-cli`.

## Green Evidence

`mise exec -- gradle :cli:test --tests com.minekube.craftless.cli.CraftlessCliTest :protocol:test --tests com.minekube.craftless.protocol.OpenApiGenerationTest`
passed after the API-only CLI and OpenAPI changes.
