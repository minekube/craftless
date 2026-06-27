# Final Gameplay Join Handoff Design

## Intent

The latest final gameplay evidence shows that Craftless can complete the
honest survival acceptance path through the generated public API and then
enter the Robin confirmation hold. The remaining completion gate is human:
Robin must join or observe the held server and send the configured Minecraft
chat confirmation phrase.

The current ready artifact is machine-readable but does not include the exact
confirmation phrase and does not produce a concise human-readable join handoff.
That makes the final manual step harder to execute and audit than necessary.

## Product Rules

- Keep the public API surface unchanged.
- Do not add gameplay actions, descriptors, generated route families, CLI
  gameplay catalogs, or scenario shortcuts.
- Do not add Minecraft version support or public version-specific APIs.
- Keep the confirmation gate strict: this phase must not mark the goal
  complete, bypass Robin's chat confirmation, or treat timeout as success.
- Write only local final-gameplay evidence artifacts under the configured
  artifacts directory.
- The handoff artifact may include the server address, client id, base URL,
  artifacts directory, hold duration, and confirmation phrase because these
  are Craftless-owned run metadata, not Fabric/Minecraft implementation names.

## Evidence

Tests and final artifacts must show:

- `final-gameplay-ready.json` includes the configured confirmation phrase when
  one is required;
- a human-readable `final-gameplay-join-instructions.txt` is written at the
  same ready boundary;
- the join instructions include the server address and exact confirmation
  phrase;
- the existing confirmation and timeout artifacts remain unchanged in meaning;
- no public gameplay API breadth is added.
