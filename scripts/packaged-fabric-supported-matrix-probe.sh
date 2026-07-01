#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
CRAFTLESS_BIN="${CRAFTLESS_PACKAGED_MATRIX_BIN:-"$ROOT/build/docker/craftless/bin/craftless"}"
MATRIX_ROOT="${CRAFTLESS_PACKAGED_MATRIX_ROOT:-$ROOT/build/craftless-packaged-fabric-supported-matrix}"
ARTIFACTS_DIR="$MATRIX_ROOT/artifacts"
WORKSPACE="${CRAFTLESS_PACKAGED_MATRIX_WORKSPACE:-$MATRIX_ROOT/workspace}"
DAEMON_PORT="${CRAFTLESS_PACKAGED_MATRIX_DAEMON_PORT:-18089}"
TIMEOUT_MS="${CRAFTLESS_PACKAGED_MATRIX_TIMEOUT_MS:-300000}"
SMOKE_TIMEOUT_MS="${CRAFTLESS_PACKAGED_MATRIX_SMOKE_TIMEOUT_MS:-900000}"
API="http://127.0.0.1:$DAEMON_PORT"
JOBS_TSV="$ARTIFACTS_DIR/probe-jobs.tsv"
DISCOVERY_ONLY="${CRAFTLESS_PACKAGED_MATRIX_DISCOVERY_ONLY:-}"

mkdir -p "$ARTIFACTS_DIR" "$WORKSPACE"
test -x "$CRAFTLESS_BIN"
test -f "$ROOT/build/docker/craftless/driver-mods.json"

DAEMON_PID=""

cleanup() {
  set +e
  if [ -n "$DAEMON_PID" ]; then
    kill "$DAEMON_PID" >/dev/null 2>&1
    wait "$DAEMON_PID" >/dev/null 2>&1
  fi
}
trap cleanup EXIT

"$CRAFTLESS_BIN" daemon start --port "$DAEMON_PORT" --workspace "$WORKSPACE" \
  > "$ARTIFACTS_DIR/matrix-daemon.log" 2>&1 &
DAEMON_PID="$!"

API="$API" ARTIFACTS_DIR="$ARTIFACTS_DIR" TIMEOUT_MS="$TIMEOUT_MS" mise exec -- bun --eval '
const fs = await import("node:fs/promises");
const path = await import("node:path");
const api = process.env.API;
const artifactsDir = process.env.ARTIFACTS_DIR;
const deadline = Date.now() + Number(process.env.TIMEOUT_MS);
while (Date.now() < deadline) {
  try {
    const response = await fetch(`${api}/openapi.json`);
    if (response.ok) {
      await fs.writeFile(path.join(artifactsDir, "supervisor-openapi.json"), `${await response.text()}\n`);
      process.exit(0);
    }
  } catch (_) {}
  await new Promise((resolve) => setTimeout(resolve, 500));
}
console.error(`timed out waiting for packaged daemon at ${api}`);
process.exit(1);
'

"$CRAFTLESS_BIN" api /versions/runtime-targets --api "$API" > "$ARTIFACTS_DIR/runtime-targets.json"
"$CRAFTLESS_BIN" api /versions/support-targets --api "$API" > "$ARTIFACTS_DIR/support-targets.json"

ROOT="$ROOT" ARTIFACTS_DIR="$ARTIFACTS_DIR" JOBS_TSV="$JOBS_TSV" mise exec -- bun --eval '
const fs = await import("node:fs/promises");
const path = await import("node:path");

const root = process.env.ROOT;
const artifactsDir = process.env.ARTIFACTS_DIR;
const jobsTsv = process.env.JOBS_TSV;
const manifest = JSON.parse(await fs.readFile(path.join(root, "build/docker/craftless/driver-mods.json"), "utf8"));
const runtimeTargets = JSON.parse(await fs.readFile(path.join(artifactsDir, "runtime-targets.json"), "utf8"));
const supportTargets = JSON.parse(await fs.readFile(path.join(artifactsDir, "support-targets.json"), "utf8"));

function signature(mod) {
  return [
    mod.loader,
    mod.minecraftVersion,
    mod.loaderVersion ?? "",
    mod.fabricApiVersion ?? "",
    mod.javaMajorVersion ?? "",
    mod.path,
  ].join("\u001f");
}

function labelFor(targetVersion, latestRelease) {
  if (targetVersion === latestRelease) {
    return "latest-current";
  }
  return `mc-${sanitize(targetVersion)}`;
}

function sanitize(value) {
  return String(value).replace(/[^A-Za-z0-9]+/g, "-").replace(/^-|-$/g, "") || "unknown";
}

function clientIdFor(label) {
  return `fabric-${label}`.replace(/[^A-Za-z0-9_-]/g, "-").slice(0, 64);
}

const manifestFabricMods = (manifest.entries ?? []).filter((entry) => entry.loader === "FABRIC");
const manifestSignatures = new Set(manifestFabricMods.map(signature));
const supportedTargets = (supportTargets.targets ?? []).filter((target) => target.supported === true);
const supportedMods = supportedTargets.flatMap((target) => target.driverMods ?? []);
const supportedSignatures = new Set(supportedMods.map(signature));

const missingFromApi = [...manifestSignatures].filter((item) => !supportedSignatures.has(item));
const extraFromApi = [...supportedSignatures].filter((item) => !manifestSignatures.has(item));
const invalidTargets = (supportTargets.targets ?? []).filter((target) => {
  const mods = target.driverMods ?? [];
  return (target.supported === true && mods.length === 0) ||
    (target.supported !== true && (!target.reason || mods.length > 0));
});

if (missingFromApi.length > 0 || extraFromApi.length > 0 || invalidTargets.length > 0) {
  await fs.writeFile(
    path.join(artifactsDir, "support-target-validation-error.json"),
    `${JSON.stringify({ missingFromApi, extraFromApi, invalidTargets }, null, 2)}\n`,
  );
  throw new Error("packaged /versions/support-targets does not match packaged Fabric driver mod manifest");
}

const latestRelease = runtimeTargets.latest?.release ?? null;
const baseLabelCounts = new Map();
for (const mod of supportedMods) {
  const baseLabel = labelFor(mod.minecraftVersion, latestRelease);
  baseLabelCounts.set(baseLabel, (baseLabelCounts.get(baseLabel) ?? 0) + 1);
}
const jobs = supportedMods.map((mod, index) => {
  const targetVersion = mod.minecraftVersion;
  const requestVersion = targetVersion === latestRelease ? "latest-release" : targetVersion;
  const baseLabel = labelFor(targetVersion, latestRelease);
  const label = baseLabelCounts.get(baseLabel) > 1 ?
    `${baseLabel}-loader-${sanitize(mod.loaderVersion ?? "default")}-${index}` :
    baseLabel;
  return {
    index,
    label,
    clientId: clientIdFor(label),
    targetVersion,
    requestVersion,
    loaderVersion: mod.loaderVersion ?? "",
    javaMajorVersion: mod.javaMajorVersion ?? "",
    daemonPort: 18090 + index,
    driverMod: mod,
  };
});

if (jobs.length === 0) {
  throw new Error("packaged /versions/support-targets did not report any supported Fabric rows");
}

await fs.writeFile(path.join(artifactsDir, "probe-jobs.json"), `${JSON.stringify(jobs, null, 2)}\n`);
await fs.writeFile(
  jobsTsv,
  jobs.map((job) => [
    job.targetVersion,
    job.requestVersion,
    job.loaderVersion,
    job.label,
    job.clientId,
    job.daemonPort,
    job.javaMajorVersion,
  ].join("\t")).join("\n") + "\n",
);
'

cleanup
trap - EXIT
DAEMON_PID=""

case "$DISCOVERY_ONLY" in
  1|true|TRUE|True)
    exit 0
    ;;
esac

while IFS=$'\t' read -r TARGET_VERSION REQUEST_VERSION LOADER_VERSION LABEL CLIENT_ID ROW_DAEMON_PORT JAVA_MAJOR_VERSION; do
  [ -n "$TARGET_VERSION" ] || continue
  ROW_ROOT="$MATRIX_ROOT/$LABEL"
  ENV_ARGS=(
    "CRAFTLESS_LOCAL_SERVER_SMOKE=1"
    "CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT=$ROW_ROOT"
    "CRAFTLESS_SMOKE_MINECRAFT_VERSION=$REQUEST_VERSION"
    "CRAFTLESS_PACKAGED_FABRIC_VERSION=$REQUEST_VERSION"
    "CRAFTLESS_PACKAGED_FABRIC_LABEL=$LABEL"
    "CRAFTLESS_PACKAGED_FABRIC_CLIENT_ID=$CLIENT_ID"
    "CRAFTLESS_PACKAGED_FABRIC_DAEMON_PORT=$ROW_DAEMON_PORT"
    "CRAFTLESS_PACKAGED_FABRIC_TIMEOUT_MS=$SMOKE_TIMEOUT_MS"
    "CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=$SMOKE_TIMEOUT_MS"
    "CRAFTLESS_SMOKE_ACTION_COMMAND_JSON=[\"$ROOT/scripts/packaged-fabric-lane-probe.sh\"]"
  )
  if [ -n "$LOADER_VERSION" ]; then
    ENV_ARGS+=("CRAFTLESS_PACKAGED_FABRIC_LOADER_VERSION=$LOADER_VERSION")
  fi
  if [ -n "$JAVA_MAJOR_VERSION" ]; then
    JAVA_EXECUTABLE_ENV="CRAFTLESS_PACKAGED_MATRIX_JAVA_${JAVA_MAJOR_VERSION}_EXECUTABLE"
    JAVA_EXECUTABLE="${!JAVA_EXECUTABLE_ENV:-}"
    if [ -n "$JAVA_EXECUTABLE" ]; then
      ENV_ARGS+=("CRAFTLESS_SMOKE_JAVA_EXECUTABLE=$JAVA_EXECUTABLE")
    fi
  fi
  printf 'Running packaged Fabric matrix row %s via %s\n' "$TARGET_VERSION" "$REQUEST_VERSION"
  env "${ENV_ARGS[@]}" mise exec -- gradle :driver-fabric:fabricClientSmoke
done < "$JOBS_TSV"
