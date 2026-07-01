# Structured runtime target error design

Fabric matrix failures must be machine-readable without forcing agents to parse
human error text. `UNSUPPORTED_RUNTIME_TARGET` already separates these failures
from generic bad requests, but the support reason and requested runtime identity
were only embedded in `message`.

## Contract

- Keep the existing `code` and `message` fields for compatibility.
- Add an optional `details` object to stable error responses.
- For `UNSUPPORTED_RUNTIME_TARGET`, populate:
  - `reason` with the `FabricSupportReason` value;
  - `loader`;
  - `minecraftVersion`;
  - `loaderVersion` when requested;
  - optional runtime identity fields when known;
  - `availableLoaderVersions`.
- Do not emit `details` on ordinary errors that have no structured context.
- Describe `details` in the stable supervisor OpenAPI error schema so adaptive
  clients and agents can discover it.

This moves the create-client rejection path closer to the full Fabric matrix
goal: unsupported combinations are rejected with structured, actionable
machine data rather than ambiguous text.
