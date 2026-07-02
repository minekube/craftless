# Session 019f121c CLI Help Evidence

## Session Source

Codex session `019f121c-6725-7aa3-85b2-c7bbe08ccd40` used Craftless as an
external real-client probe during a Minekube Connect production incident.

Relevant Craftless friction observed in the session:

- The agent used `craftless api /clients` to create a real client for a join
  smoke.
- It had to discover manually that `/clients` has both `GET` and `POST`
  operations, because `craftless api /clients --help` defaulted to the `GET`
  operation unless `--method POST` was supplied.
- Later Craftless behavior already addressed the other major ambiguity from the
  same session: field-bearing `craftless api /clients -F ...` infers `POST`,
  `POST /clients` descriptions explicitly warn that it launches a new real
  Minecraft process, and `presentation.window = NONE` now fails early with an
  actionable windowless-wrapper message when no real windowless strategy is
  available.

## Fix

`craftless api <endpoint> --help` now lists every matching OpenAPI operation
when the caller did not explicitly provide `--method` and did not provide body
fields or `--input`.

For `/clients`, help now includes both:

- `Route: GET /clients`, the list operation agents should use before creating
  another client.
- `Route: POST /clients`, the create operation with OpenAPI-derived request
  fields, enum values, defaults, and lifecycle description.

Actual invocation semantics are unchanged: `-F`, `--field`, `-f`,
`--raw-field`, or `--input` still infer `POST`, while body-less invocation still
defaults to `GET`.

## Verification

```sh
mise exec -- gradle :cli:test --tests 'com.minekube.craftless.cli.CraftlessCliTest.api help shows every matching method when route is ambiguous'
```

Result: passed. The test first failed because only `GET /clients` appeared in
ambiguous route help, then passed after multi-operation help was implemented.

```sh
mise exec -- gradle :cli:test
```

Result: passed.
