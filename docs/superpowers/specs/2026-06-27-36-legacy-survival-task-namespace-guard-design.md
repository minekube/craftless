# Phase 36: Legacy Survival Task Namespace Guard Design

## Problem

Phase 29 removed the diagnostic `task.survival.*` product path, but protocol
tests still used `task.survival.obtain-weapon` as a positive serialization
example. That made the legacy scenario namespace look like an acceptable
durable public task id even though final gameplay must be composed externally
through generated actions and state evidence.

## Design

Reject `task.survival.*` in `NavigationTaskRequest` validation and in
`NavigationProgressEvent.type` validation so the removed scenario namespace is
not accepted as either client-supplied task data or server-emitted progress
metadata.

This is a protocol guardrail only:

- it does not add or remove generated gameplay actions;
- it does not add a scenario macro;
- it does not change `task.run` availability;
- it keeps generic future task ids such as `task.generic.obtain-materials`
  valid protocol data, while Fabric currently reports
  `task-executor-unavailable`.

Tests should use neutral generic task ids when exercising generic task
metadata. Tests that need legacy evidence should assert protocol rejection or
assert absence of `task.survival.*` in generated requests/artifacts.

## Acceptance

- `NavigationTaskRequest(task = "task.survival.obtain-weapon")` fails
  validation.
- `NavigationProgressEvent(type = "task.survival.progress")` fails
  validation.
- Navigation serialization tests use a neutral generic task id.
- Fabric task-adapter tests no longer construct a valid `task.survival.*`
  request; they continue to prove generic task execution is unavailable without
  an executor.
