# Runtime Capability Graph Design

**Goal:** Add the internal graph model that represents what the running Minecraft client can observe, expose, invoke, and stream.

**Architecture:** The graph is a protocol/domain layer object, not Fabric code. Fabric probes fill graph nodes; projection code converts graph nodes into Craftless-owned OpenAPI actions, resources, handles, schemas, events, and fingerprints. The graph replaces hand-written gameplay descriptors as the authority for per-client generated API breadth.

**Graph Concepts:**
- `RuntimeCapabilityGraph`: one snapshot for one client runtime.
- `RuntimeResourceNode`: Craftless-owned resource such as `player`, `inventory`, `world.block`, `screen`, or future registry-backed resources.
- `RuntimeOperationNode`: callable affordance with arguments, result schema, availability, source evidence, and execution adapter key.
- `RuntimeEventNode`: observable event source with payload schema and stream topic.
- `RuntimeHandleNode`: opaque handle for object instances such as inventory slots, blocks, screens, entities, or containers.
- `RuntimeSchema`: JSON-compatible schema model shared by actions, resources, events, and handles.
- `RuntimeFingerprint`: deterministic fingerprint from Minecraft version, loader, mappings, mods, registries, server features, permissions, graph node ids, schemas, and availability.

**Rules:**
- Public node ids are Craftless-owned.
- Raw Fabric/Yarn/Minecraft names may appear only in private evidence fields excluded from public OpenAPI and streams.
- New gameplay breadth must enter through graph nodes, not through new public descriptor/binding pairs.
- Graph snapshots are immutable values suitable for tests and ETag generation.

**Completion Gate:**
- Protocol tests prove graph validation, uniqueness, fingerprint stability, and namespace hygiene.
- Current per-client OpenAPI can be generated from a graph snapshot.
- Existing bootstrap action descriptors are either graph-derived or explicitly isolated as compatibility input.
