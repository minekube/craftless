# Phase 38: Combat Miss Retry Design

## Problem

The 2026-06-27 final gameplay rerun reached the human-ready window but exposed
a public-agent combat retry gap. The agent legitimately collected materials,
crafted and equipped a Wooden Sword, found a Cow through generated
`entity.query`, closed reach with generated navigation, and landed one
generated `entity.attack`. The Cow then drifted between public reach evidence
and the next generated attack. The second `entity.attack` returned
`hit=false` with `entity-target-out-of-range`, and the runner immediately
blocked with `insufficient-public-evidence:entity.attack.hit`.

A generated attack miss is not final evidence that combat cannot proceed. It
is a signal that the external policy must refresh public state, re-focus the
target, and retry while its bounded evidence budget remains.

## Design

Keep this correction in the process-external public-agent policy. Do not add a
new product gameplay action, route, CLI command, Fabric binding, or scenario
shortcut.

When generated `entity.attack` returns a public result with `hit=false` and
combat attempts remain, the runner should:

- pause using the configured combat retry delay;
- query generated public entity state again;
- prefer the same public target handle when still visible;
- fall back to another configured public combat evidence target only through
  generated `entity.query`;
- re-run the existing focus path, including generated navigation and
  `player.move` fallback when available;
- retry generated `entity.attack`.

The runner should still block when:

- the miss happens on the final configured combat attempt;
- public entity perception cannot provide any acceptable combat evidence
  target;
- focus cannot prove the target is reachable;
- no attack outcome, dropped loot, inventory loot, or not-alive evidence is
  observed before the bounded attempt budget expires.

## Acceptance

- Focused test proves a generated `entity.attack` miss does not immediately
  block when retry attempts remain.
- The retry path records additional generated public perception before the
  next attack.
- Existing combat blocker tests still prove missing outcome evidence remains a
  blocker.
- No new public gameplay action ids, scenario macros, static route families, or
  survival shortcuts are added.
