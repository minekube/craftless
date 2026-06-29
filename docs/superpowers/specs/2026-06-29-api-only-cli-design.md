# API-Only CLI Design

## Goal

Make `craftless api` the only CLI surface for HTTP API routes. Keep `craftless daemon start` as the local daemon launcher, preserve `server start` only as a hidden compatibility alias, and remove generated route command trees such as `clients create`, `clients <id> run`, and `clients <id> player move`.

## Problem

The CLI currently has two route-command systems:

- static Kotlin dispatch branches for selected supervisor and client lifecycle routes;
- `x-craftless-cli` metadata in generated OpenAPI, consumed by `GeneratedRouteCli` to synthesize additional commands.

That gives agents multiple ways to say the same thing and makes the OpenAPI document carry CLI-specific binding metadata even though the normal OpenAPI request schema already describes fields, required properties, enum values, defaults, and descriptions.

## User-Facing Shape

The public command shape is:

```text
craftless api <endpoint> [flags]
craftless daemon start [flags]
```

`craftless api` is modeled after the useful parts of `gh api`:

- `-X, --method <method>` chooses `GET` or `POST`.
- `-F, --field key=value` adds a typed JSON field. Values `true`, `false`, `null`, integers, and numbers become JSON values.
- `-f, --raw-field key=value` adds a string JSON field.
- bracket paths such as `profile[name]=Alice` and `args[message]=hello` build nested JSON objects.
- `--input <path>` sends a JSON request body from a file.
- `--api <url>` selects the daemon URL, defaulting to `CRAFTLESS` or `http://127.0.0.1:8080`.
- `--help` loads the matching OpenAPI operation and prints route, summary, description, request fields, required flags, defaults, and enum values from schemas.

When `--method` is omitted, `api` defaults to `POST` if fields or input are present and `GET` otherwise. The command sends the endpoint exactly as a REST path, for example:

```text
craftless api /clients
craftless api /clients -F id=bot -F version=latest-release -F loader=FABRIC
craftless api /clients/bot:run -F action=player.chat -F args[message]=hello
craftless api /clients/bot/player:chat -F message=hello
```

## OpenAPI Contract

The OpenAPI document should not emit `x-craftless-cli`. Adaptive consumers infer command behavior from standard OpenAPI paths, operation summaries/descriptions, request bodies, schemas, required fields, enums, defaults, and Craftless operation extensions.

`ApiRoute` may continue to carry route ownership metadata such as owner, member, source, target, and action id because those describe the API itself, not a CLI alias grammar.

## Matching

`craftless api` first fetches `/openapi.json` and tries to match the endpoint against exact and templated supervisor paths such as `/clients/{id}:connect`.

If no supervisor operation matches and the endpoint looks client-scoped, it fetches `/clients/{id}/openapi.json` and matches the concrete per-client paths there. This keeps gameplay invocation adaptive to the running client without generated command aliases.

## Non-Goals

- No new gameplay actions, resource catalogs, scenario shortcuts, or action-specific CLI commands.
- No generated subcommands outside `api`.
- No custom HTTP method enum.
- No CLI-only request binding extension in OpenAPI.
