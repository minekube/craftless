# Generic Invocation Design

**Goal:** Invoke graph-projected operations through generic client-thread adapters instead of a static public action catalog.

**Architecture:** Public invocation remains `POST /clients/{id}:run` and generated aliases. Internally, the daemon validates against graph-projected OpenAPI, then dispatches to an execution adapter identified by the graph operation. Fabric adapters execute on the client thread and return graph-schema-compatible results.

**Invocation Flow:**
1. Consumer fetches `/clients/{id}/openapi.json`.
2. Consumer invokes an operation id with JSON args.
3. Daemon validates operation id, availability, arguments, and permissions from graph-projected metadata.
4. Driver resolves the operation's adapter key and target handles.
5. Adapter executes on the client thread.
6. Result is validated against the graph-projected result schema.
7. Result and related events are published to the event bus.

**Rules:**
- Do not add driver methods per gameplay action.
- Do not add public descriptor/binding pairs for new gameplay breadth.
- Existing bootstrap adapters may remain while graph-backed adapters replace them.
- Invocation errors are machine-readable and include unavailable, permission, schema, stale-handle, and runtime-mismatch cases.

**Completion Gate:**
- Tests prove invocation can dispatch from graph operation metadata.
- Tests prove unavailable graph operations do not invoke adapters.
- Existing smoke actions run through graph-backed invocation or explicit bootstrap compatibility paths marked as transitional.
