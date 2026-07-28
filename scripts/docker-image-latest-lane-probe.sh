#!/usr/bin/env bash
# Prove the shipped Docker image runs the newest Fabric lane it packages.
#
# This probe builds the repository image, starts a matching vanilla Minecraft
# server, and drives create/attach/connect through the image's own public API
# until the server reports the client joined the game. A container that starts
# is not a join; only the server-side join line passes this probe.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
DOCKER_CONTEXT="$ROOT/build/docker/craftless"
DRIVER_MODS="$DOCKER_CONTEXT/driver-mods.json"
ARTIFACTS_DIR="${CRAFTLESS_DOCKER_PROBE_ARTIFACTS_DIR:-$ROOT/build/craftless-docker-image-lane-probe/artifacts}"
PROBE_ROOT="${CRAFTLESS_DOCKER_PROBE_ROOT:-$ROOT/build/craftless-docker-image-lane-probe}"
IMAGE="${CRAFTLESS_DOCKER_PROBE_IMAGE:-craftless-docker-lane-probe:local}"
PREFIX="${CRAFTLESS_DOCKER_PROBE_PREFIX:-craftless-docker-lane-probe}"
CLIENT_ID="${CRAFTLESS_DOCKER_PROBE_CLIENT_ID:-dockerlane}"
PLAYER_NAME="${CRAFTLESS_DOCKER_PROBE_PLAYER:-DockerLane}"
TIMEOUT_MS="${CRAFTLESS_DOCKER_PROBE_TIMEOUT_MS:-900000}"
SERVER_READY_TIMEOUT_SECONDS="${CRAFTLESS_DOCKER_PROBE_SERVER_TIMEOUT_SECONDS:-600}"
CREATE_ATTEMPTS="${CRAFTLESS_DOCKER_PROBE_CREATE_ATTEMPTS:-4}"
PLATFORM_ARG=()
if [ -n "${CRAFTLESS_DOCKER_PROBE_PLATFORM:-}" ]; then
  PLATFORM_ARG=(--platform "$CRAFTLESS_DOCKER_PROBE_PLATFORM")
fi

SERVER_CONTAINER="$PREFIX-server"
CRAFTLESS_CONTAINER="$PREFIX-craftless"
NETWORK="$PREFIX-net"
WORKSPACE_VOLUME="$PREFIX-workspace"
API=""

mkdir -p "$ARTIFACTS_DIR"

command -v docker > /dev/null || { echo "docker is required for the Docker image lane probe" >&2; exit 1; }
test -f "$DRIVER_MODS" || { echo "missing $DRIVER_MODS; run 'mise run package-cli' first" >&2; exit 1; }

# The newest Minecraft lane the packaged manifest ships is the lane the image must run.
MINECRAFT_VERSION="${CRAFTLESS_DOCKER_PROBE_VERSION:-$(
  DRIVER_MODS="$DRIVER_MODS" mise exec -- bun --eval '
  const fs = await import("node:fs/promises");
  const manifest = JSON.parse(await fs.readFile(process.env.DRIVER_MODS, "utf8"));
  const fabric = manifest.entries.filter((entry) => entry.loader === "FABRIC");
  const parts = (version) => version.split(".").map((part) => Number.parseInt(part, 10) || 0);
  const newer = (a, b) => {
    const [left, right] = [parts(a), parts(b)];
    for (let index = 0; index < Math.max(left.length, right.length); index += 1) {
      const diff = (left[index] ?? 0) - (right[index] ?? 0);
      if (diff !== 0) return diff > 0;
    }
    return false;
  };
  const latest = fabric.reduce((best, entry) => (best && !newer(entry.minecraftVersion, best) ? best : entry.minecraftVersion), "");
  if (!latest) throw new Error("packaged driver manifest lists no Fabric lane");
  process.stdout.write(latest);
  '
)}"
echo "docker image lane probe: newest packaged Fabric lane is $MINECRAFT_VERSION"

# Keep the server world beside its own version so a lane bump cannot reuse a
# world the new server refuses to load.
SERVER_DIR="$PROBE_ROOT/server-$MINECRAFT_VERSION"
mkdir -p "$SERVER_DIR"

collect_diagnostics() {
  docker logs "$CRAFTLESS_CONTAINER" > "$ARTIFACTS_DIR/craftless-container.log" 2>&1 || true
  docker logs "$SERVER_CONTAINER" > "$ARTIFACTS_DIR/minecraft-server-container.log" 2>&1 || true
  docker exec "$CRAFTLESS_CONTAINER" /bin/sh -c \
    'find /var/lib/craftless/instances -name client.log -exec tail -n 200 {} +' \
    > "$ARTIFACTS_DIR/client.log" 2>&1 || true
}

teardown() {
  set +e
  collect_diagnostics
  docker rm -f "$CRAFTLESS_CONTAINER" > /dev/null 2>&1
  docker rm -f "$SERVER_CONTAINER" > /dev/null 2>&1
  docker volume rm -f "$WORKSPACE_VOLUME" > /dev/null 2>&1
  docker network rm "$NETWORK" > /dev/null 2>&1
  # Verify the teardown instead of trusting it; a survivor corrupts the next run.
  local leftovers
  leftovers="$(docker ps -a --filter "name=^${PREFIX}-" --format '{{.Names}}')"
  local volumes
  volumes="$(docker volume ls --filter "name=^${WORKSPACE_VOLUME}$" --format '{{.Name}}')"
  local networks
  networks="$(docker network ls --filter "name=^${NETWORK}$" --format '{{.Name}}')"
  printf 'containers: %s\nvolumes: %s\nnetworks: %s\n' "${leftovers:-none}" "${volumes:-none}" "${networks:-none}" \
    > "$ARTIFACTS_DIR/teardown-verification.txt"
  if [ -n "$leftovers$volumes$networks" ]; then
    echo "docker image lane probe left resources behind: $leftovers $volumes $networks" >&2
  fi
}
trap teardown EXIT

docker rm -f "$CRAFTLESS_CONTAINER" "$SERVER_CONTAINER" > /dev/null 2>&1 || true
docker volume rm -f "$WORKSPACE_VOLUME" > /dev/null 2>&1 || true
docker network rm "$NETWORK" > /dev/null 2>&1 || true

docker build "${PLATFORM_ARG[@]}" -t "$IMAGE" "$ROOT" > "$ARTIFACTS_DIR/docker-build.log" 2>&1

# The image must carry the lane it claims and must not pin a single driver jar.
docker run --rm "${PLATFORM_ARG[@]}" --entrypoint /bin/sh "$IMAGE" -c \
  'test -f /opt/craftless/driver-mods.json && test -z "${CRAFTLESS_FABRIC_DRIVER_MOD:-}" && env | sort' \
  > "$ARTIFACTS_DIR/image-environment.txt"

SERVER_METADATA="$ARTIFACTS_DIR/minecraft-server-metadata.json"
MINECRAFT_VERSION="$MINECRAFT_VERSION" SERVER_METADATA="$SERVER_METADATA" SERVER_DIR="$SERVER_DIR" mise exec -- bun --eval '
const fs = await import("node:fs/promises");
const path = await import("node:path");
const version = process.env.MINECRAFT_VERSION;
const manifest = await fetch("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").then((response) => response.json());
const entry = manifest.versions.find((candidate) => candidate.id === version);
if (!entry) throw new Error(`Mojang version manifest has no entry for ${version}`);
const detail = await fetch(entry.url).then((response) => response.json());
const serverUrl = detail.downloads?.server?.url;
if (!serverUrl) throw new Error(`Mojang release ${version} has no server download`);
const summary = {
  minecraftVersion: version,
  javaMajorVersion: detail.javaVersion?.majorVersion ?? 21,
  serverUrl,
};
const jar = path.join(process.env.SERVER_DIR, "server.jar");
const bytes = Buffer.from(await fetch(serverUrl).then((response) => response.arrayBuffer()));
await fs.writeFile(jar, bytes);
summary.serverJarBytes = bytes.length;
await fs.writeFile(process.env.SERVER_METADATA, `${JSON.stringify(summary, null, 2)}\n`);
'
SERVER_JAVA_MAJOR="$(SERVER_METADATA="$SERVER_METADATA" mise exec -- bun --eval '
const fs = await import("node:fs/promises");
process.stdout.write(String(JSON.parse(await fs.readFile(process.env.SERVER_METADATA, "utf8")).javaMajorVersion));
')"

printf 'eula=true\n' > "$SERVER_DIR/eula.txt"
printf 'online-mode=false\nlevel-type=minecraft\\:flat\nspawn-protection=0\nmax-players=4\nview-distance=6\nsync-chunk-writes=false\n' \
  > "$SERVER_DIR/server.properties"

docker network create "$NETWORK" > /dev/null

# The server is plain Java, so it runs on the host platform; only the Craftless
# image is pinned, because Mojang ships client natives for linux/x64 only.
docker run -d --name "$SERVER_CONTAINER" \
  --network "$NETWORK" \
  -v "$SERVER_DIR:/server" \
  -w /server \
  "eclipse-temurin:${SERVER_JAVA_MAJOR}-jdk" \
  java -Xmx2G -jar server.jar nogui > /dev/null

server_ready=0
for _ in $(seq 1 "$SERVER_READY_TIMEOUT_SECONDS"); do
  if docker logs "$SERVER_CONTAINER" 2>&1 | grep -q 'Done ('; then
    server_ready=1
    break
  fi
  sleep 1
done
if [ "$server_ready" != "1" ]; then
  echo "minecraft $MINECRAFT_VERSION server did not become ready" >&2
  exit 1
fi

docker run -d --name "$CRAFTLESS_CONTAINER" "${PLATFORM_ARG[@]}" \
  --network "$NETWORK" \
  -v "$WORKSPACE_VOLUME:/var/lib/craftless" \
  -p 127.0.0.1::8080 \
  "$IMAGE" > /dev/null

# Drive the API with the CLI the image ships, from inside the container.
craftless_api() {
  local endpoint="$1"
  shift
  docker exec "$CRAFTLESS_CONTAINER" /opt/craftless/bin/craftless api "$endpoint" \
    --api "http://127.0.0.1:8080" "$@"
}

HOST_PORT="$(docker port "$CRAFTLESS_CONTAINER" 8080/tcp | head -n 1 | sed 's/.*://')"
test -n "$HOST_PORT"
API="http://127.0.0.1:$HOST_PORT"
echo "docker image lane probe: craftless api on $API"

API="$API" ARTIFACTS_DIR="$ARTIFACTS_DIR" TIMEOUT_MS="$TIMEOUT_MS" mise exec -- bun --eval '
const fs = await import("node:fs/promises");
const path = await import("node:path");
const api = process.env.API;
const deadline = Date.now() + Number(process.env.TIMEOUT_MS);
while (Date.now() < deadline) {
  try {
    const response = await fetch(`${api}/openapi.json`);
    if (response.ok) {
      await fs.writeFile(path.join(process.env.ARTIFACTS_DIR, "supervisor-openapi.json"), `${await response.text()}\n`);
      process.exit(0);
    }
  } catch (_) {}
  await new Promise((resolve) => setTimeout(resolve, 500));
}
console.error(`timed out waiting for the containerized daemon at ${api}`);
process.exit(1);
'

# create: the shipped default presentation, no driver mod override, no lane hints.
#
# Craftless resolves Minecraft artifacts at create time, and an interrupted
# Mojang download fails the call without touching the lane under test. Such a
# call is retried, and each attempt is kept in the artifacts. A product answer -
# a failed client runtime or an unsupported runtime target - is never retried,
# so the defect this probe exists for still fails on the first attempt.
create_attempt=0
while :; do
  create_attempt=$((create_attempt + 1))
  if craftless_api /clients \
    -f "id=$CLIENT_ID" \
    -f "version=$MINECRAFT_VERSION" \
    -f loader=FABRIC \
    -f "profile[kind]=OFFLINE" \
    -f "profile[name]=$PLAYER_NAME" \
    > "$ARTIFACTS_DIR/clients-create-attempt-$create_attempt.json" 2>&1; then
    cp "$ARTIFACTS_DIR/clients-create-attempt-$create_attempt.json" "$ARTIFACTS_DIR/clients-create.json"
    break
  fi
  cp "$ARTIFACTS_DIR/clients-create-attempt-$create_attempt.json" "$ARTIFACTS_DIR/clients-create.json"
  if grep -qE 'CLIENT_RUNTIME_FAILED|UNSUPPORTED_RUNTIME_TARGET' "$ARTIFACTS_DIR/clients-create.json"; then
    echo "create client on $MINECRAFT_VERSION reported a product failure:" >&2
    cat "$ARTIFACTS_DIR/clients-create.json" >&2
    exit 1
  fi
  if [ "$create_attempt" -ge "$CREATE_ATTEMPTS" ]; then
    echo "create client on $MINECRAFT_VERSION failed $create_attempt times:" >&2
    cat "$ARTIFACTS_DIR/clients-create.json" >&2
    exit 1
  fi
  echo "docker image lane probe: create attempt $create_attempt failed on artifact resolution, retrying" >&2
  sleep 5
done

API="$API" CLIENT_ID="$CLIENT_ID" TIMEOUT_MS="$TIMEOUT_MS" mise exec -- bun --eval '
const api = process.env.API;
const clientId = process.env.CLIENT_ID;
const deadline = Date.now() + Number(process.env.TIMEOUT_MS);
while (Date.now() < deadline) {
  const client = await fetch(`${api}/clients/${clientId}`).then((response) => response.json()).catch(() => null);
  if (client && (client.state === "FAILED" || client.state === "STOPPED")) {
    console.error(`client ${clientId} runtime died before attaching: ${JSON.stringify(client)}`);
    process.exit(1);
  }
  const events = await fetch(`${api}/events`).then((response) => (response.ok ? response.json() : [])).catch(() => []);
  if (events.some((event) => event.type === "client.attached" && event.client === clientId)) process.exit(0);
  if (events.some((event) => event.type === "client.failed" && event.client === clientId)) {
    console.error(`client ${clientId} failed: ${JSON.stringify(events.filter((event) => event.client === clientId))}`);
    process.exit(1);
  }
  await new Promise((resolve) => setTimeout(resolve, 1000));
}
console.error(`timed out waiting for client.attached for ${clientId}`);
process.exit(1);
'

craftless_api "/clients/$CLIENT_ID:connect" \
  -f "host=$SERVER_CONTAINER" \
  -F port=25565 \
  > "$ARTIFACTS_DIR/clients-connect.json"

# A compile is not a join: the server itself must report the player in the world.
joined=0
for _ in $(seq 1 "$SERVER_READY_TIMEOUT_SECONDS"); do
  if docker logs "$SERVER_CONTAINER" 2>&1 | grep -q "$PLAYER_NAME joined the game"; then
    joined=1
    break
  fi
  sleep 1
done
docker logs "$SERVER_CONTAINER" > "$ARTIFACTS_DIR/minecraft-server-join.log" 2>&1
if [ "$joined" != "1" ]; then
  echo "client $CLIENT_ID never reached play state on the $MINECRAFT_VERSION server" >&2
  exit 1
fi

# The in-world client must also serve its generated per-client contract.
API="$API" CLIENT_ID="$CLIENT_ID" ARTIFACTS_DIR="$ARTIFACTS_DIR" TIMEOUT_MS="$TIMEOUT_MS" mise exec -- bun --eval '
const fs = await import("node:fs/promises");
const path = await import("node:path");
const api = process.env.API;
const clientId = process.env.CLIENT_ID;
const deadline = Date.now() + Number(process.env.TIMEOUT_MS);
const required = ["client", "player", "inventory", "world"];
while (Date.now() < deadline) {
  const response = await fetch(`${api}/clients/${clientId}/openapi.json`).catch(() => null);
  if (response?.ok) {
    const text = await response.text();
    const openapi = JSON.parse(text);
    const resources = Array.isArray(openapi["x-craftless-resources"]) ? openapi["x-craftless-resources"] : [];
    const ids = new Set(resources.map((resource) => resource.id));
    if (required.every((id) => ids.has(id))) {
      await fs.writeFile(path.join(process.env.ARTIFACTS_DIR, "client-openapi-connected.json"), `${text}\n`);
      process.exit(0);
    }
  }
  await new Promise((resolve) => setTimeout(resolve, 1000));
}
console.error(`timed out waiting for connected generated OpenAPI for ${clientId}`);
process.exit(1);
'

craftless_api "/clients/$CLIENT_ID" > "$ARTIFACTS_DIR/client-after-connect.json"
craftless_api "/clients/$CLIENT_ID/actions" > "$ARTIFACTS_DIR/client-actions.json"
craftless_api "/clients/$CLIENT_ID:stop" -X POST > "$ARTIFACTS_DIR/client-stop.json"

MINECRAFT_VERSION="$MINECRAFT_VERSION" IMAGE="$IMAGE" PLAYER_NAME="$PLAYER_NAME" ARTIFACTS_DIR="$ARTIFACTS_DIR" mise exec -- bun --eval '
const fs = await import("node:fs/promises");
const path = await import("node:path");
const artifactsDir = process.env.ARTIFACTS_DIR;
const read = async (name) => JSON.parse(await fs.readFile(path.join(artifactsDir, name), "utf8"));
const client = await read("client-after-connect.json");
const actions = await read("client-actions.json");
const openapi = await read("client-openapi-connected.json");
const serverLog = await fs.readFile(path.join(artifactsDir, "minecraft-server-join.log"), "utf8");
const joinLine = serverLog.split("\n").find((line) => line.includes(`${process.env.PLAYER_NAME} joined the game`));
if (!joinLine) throw new Error("server log has no join line");
const summary = {
  status: "joined",
  image: process.env.IMAGE,
  minecraftVersion: process.env.MINECRAFT_VERSION,
  clientState: client.state,
  serverJoinLine: joinLine.trim(),
  actionCount: Array.isArray(actions) ? actions.length : 0,
  resourceIds: (openapi["x-craftless-resources"] ?? []).map((resource) => resource.id),
};
await fs.writeFile(path.join(artifactsDir, "docker-image-lane-probe-summary.json"), `${JSON.stringify(summary, null, 2)}\n`);
console.log(JSON.stringify(summary));
'
