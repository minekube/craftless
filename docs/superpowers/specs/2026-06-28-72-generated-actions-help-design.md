# Generated Actions Help Design

## Problem

The CLI can already fetch live per-client actions as JSON and run generated
action aliases from a client's OpenAPI document. However, `craftless clients
<id> actions --help` still behaved like the JSON listing path instead of
showing an agent-friendly generated command overview.

That left users and agents with two weaker options: inspect raw JSON manually
or guess generated aliases. Craftless needs the CLI to stay adaptive while
making live action discovery easy to use.

## Design

Keep `clients <id> actions` as the JSON action projection. Add a `--help`
branch that still fetches and decodes the live per-client OpenAPI document, then
renders a plain-text overview from `x-craftless-actions` and the generated alias
route metadata.

Each line should be derived from live action ids and argument metadata:

- command shape: `craftless clients <id> <action id as segments>`;
- argument flags from the action schema;
- route evidence from the generated alias route when present, otherwise
  generic `POST /clients/{id}:run`.

This improves adaptive CLI usability without introducing static gameplay
commands.

## Boundaries

- Do not add hard-coded gameplay command catalogs.
- Do not add new public gameplay actions, route families, Fabric bindings, or
  scenario shortcuts.
- Do not treat action help as an independent source of truth; it is a rendering
  of the live per-client OpenAPI document.
- Keep JSON output unchanged for `clients <id> actions` without `--help`.

## Verification

- A failing CLI test proves `clients alice actions --help` initially emits JSON
  instead of generated help.
- The same test passes after the CLI renders live action ids, argument flags,
  and route evidence from OpenAPI.
- Broader CLI and repository gates verify the change does not break existing
  adaptive invocation or tooling.
