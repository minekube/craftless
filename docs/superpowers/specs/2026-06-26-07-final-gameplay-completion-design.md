# Final Gameplay Completion Design

**Goal:** Prove Craftless is usable by playing Minecraft with Robin on a real server session and fixing issues found during that session.

**Architecture:** The final test is an end-to-end user workflow, not a unit test. Craftless launches or attaches to a real Fabric client, joins a server, exposes live OpenAPI, streams SSE events, invokes graph-discovered capabilities, performs gameplay, records artifacts, and stays running long enough for Robin to join and confirm completion in Minecraft chat.

**Gameplay Scenario:**
- Start local or agreed server with evidence collection.
- Launch visible or headless Craftless-controlled Fabric client.
- Fetch per-client OpenAPI and runtime graph artifacts.
- Subscribe to SSE lifecycle, capability, inventory, chat, movement, screen, and world interaction events.
- Send chat.
- Observe inventory/world state.
- Find, obtain, or test-fixture provision a tool such as a wooden sword or iron pickaxe.
- Equip the tool through discovered metadata.
- Mine at least one block and place/build a small structure.
- Record artifacts: OpenAPI, graph snapshot, event stream, invocation transcript, server evidence, screenshots/logs when available.
- Invite Robin using macOS `say` when human participation is needed.
- Continue fixing discovered issues until Robin writes in Minecraft chat that the goal may be completed.

**Completion Gate:**
- `mise run ci` passes.
- Real gameplay evidence is captured.
- No static-action workaround is used to bypass graph/SSE/invocation requirements.
- Robin confirms in Minecraft chat that the goal may be completed.
