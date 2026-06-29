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

`rg -n "craftless clients|clients \"\\$CLIENT_ID\"|clients create|clients <id>|GeneratedRouteCli|x-craftless-cli|generated alias|generated aliases|CLI alias|generated CLI subcommands|generated CLI help|adaptive CLI invocation|adaptive CLI" README.md .agents scripts playwright docs/*.md docs/agent*.md docs/final* docs/roadmap.md cli/AGENTS.md daemon/AGENTS.md docs/agent-operating-contract.md docs/agent-module-contracts.md cli/src/main protocol/src/main .agents/skills/test-suite-builder/SKILL.md -g '!**/build/**'`
returned no current-facing stale CLI design mentions.

`bash -n scripts/packaged-latest-current-probe.sh scripts/packaged-representative-older-probe.sh scripts/final-public-gameplay-probe.sh`
passed after converting packaged and final probe scripts to `craftless api`
route invocations.

`mise exec -- bun test playwright/src/distribution.test.ts`
passed after updating distribution assertions to the API-only CLI shape.

`mise exec -- gradle :cli:test :protocol:test :cli:ktlintCheck :protocol:ktlintCheck`
passed after removing the stale generated-command CLI helpers and ignored
legacy shortcut tests.

`git diff --check` passed.
