# Incremental Public-Agent Artifacts Design

## Intent

The 2026-06-27 final gameplay rerun after Phase 33 entered a long generated
`navigation.follow` request. Thread dumps showed the request waiting inside
the reflective pathfinder follow path, but `public-agent-gameplay-results.jsonl`
still contained stale earlier smoke entries because public-agent action
artifacts were written only after the runner finished.

This phase improves evidence plumbing only. It must not add survival
shortcuts, pathfinder-specific public API, scenario macros, or any new product
gameplay action.

## Product Rules

- Initialize public-agent artifact files immediately after public discovery,
  so stale prior-run action evidence is removed before gameplay begins.
- Append `public-agent-action-started` before each generated
  `POST /clients/{id}:run` request.
- Append `public-agent-action` when a generated request returns or fails.
- Append `public-agent-blocked` as soon as a public blocker is known.
- Preserve the in-memory `PublicAgentGameplayResult` action log and existing
  final state semantics.
- Keep action ids and blockers Craftless-owned and sourced from the generated
  public API path.

## Evidence

Tests and live artifacts must show:

- a generated action request failure records an `action-started` event before
  the failure response;
- final blocked artifacts still include the failed action response and blocker;
- successful public-agent runs still write normal `public-agent-action` lines;
- stale prior-run gameplay artifacts are truncated at the start of a new run;
- no scenario shortcut appears in public-agent request bodies or artifacts.
