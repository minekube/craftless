#!/usr/bin/env bash
# Turns the probe's machine-readable outcome document into the canary's reported
# result: job outputs, a step summary, and GitHub annotations.
#
# This script does not classify failures. Classification lives with the probe
# harness (CanaryFailureClassifier), the only place that can see the failure text.
# The single judgement made here is what to do when the harness never produced a
# document: that is reported as "unclassified" and carries product severity, so a
# canary that could not explain itself is never assumed to be harmless noise.
set -euo pipefail

: "${CANARY_OUTCOME_PATH:?CANARY_OUTCOME_PATH is required}"

CANARY_OUTCOME_PATH="$CANARY_OUTCOME_PATH" \
CANARY_PROBE_LOG_PATH="${CANARY_PROBE_LOG_PATH:-}" \
CANARY_PROBE_EXIT_CODE="${CANARY_PROBE_EXIT_CODE:-}" \
  mise exec -- bun --eval '
const fs = await import("node:fs/promises");

const outcomePath = process.env.CANARY_OUTCOME_PATH;
const probeLogPath = process.env.CANARY_PROBE_LOG_PATH;

// An absent or unparseable exit code means the probe step did not report one,
// which is a failure to obtain a verdict, never silently treated as success.
const rawExitCode = (process.env.CANARY_PROBE_EXIT_CODE ?? "").trim();
const parsedExitCode = rawExitCode ? Number(rawExitCode) : Number.NaN;
const probeExitCode = Number.isFinite(parsedExitCode) ? parsedExitCode : 1;

async function readOutcome() {
  try {
    return JSON.parse(await fs.readFile(outcomePath, "utf8"));
  } catch {
    return null;
  }
}

async function probeLogTail(lines = 40) {
  if (!probeLogPath) {
    return "";
  }
  try {
    const text = await fs.readFile(probeLogPath, "utf8");
    return text.split("\n").slice(-lines).join("\n").trim();
  } catch {
    return "";
  }
}

async function emit(name, value) {
  const file = process.env.GITHUB_OUTPUT;
  if (!file) {
    return;
  }
  const delimiter = `ghadelim_${name}`;
  await fs.appendFile(file, `${name}<<${delimiter}\n${value}\n${delimiter}\n`);
}

async function writeSummary(lines) {
  const file = process.env.GITHUB_STEP_SUMMARY;
  if (!file) {
    console.log(lines.join("\n"));
    return;
  }
  await fs.appendFile(file, `${lines.join("\n")}\n`);
}

const outcome = await readOutcome();
const missingDocument = outcome === null;
const documentSaysFailed = outcome?.verdict === "FAIL";
const failed = documentSaysFailed || probeExitCode !== 0;

let failureClass = "";
if (failed) {
  failureClass = documentSaysFailed && outcome?.failureClass ? outcome.failureClass.toLowerCase() : "unclassified";
}

const verdict = failed ? "fail" : "pass";
const version = outcome?.minecraftVersion ?? "unknown";
const cleanupFailure = outcome?.cleanupFailure ?? "";
const reason = failed
  ? (documentSaysFailed && outcome?.reason) ||
    `probe exited with code ${probeExitCode} without recording a classified outcome${
      missingDocument ? ` (no ${outcomePath})` : ""
    }`
  : "every product assertion passed";

const singleLine = (value) => value.replace(/\s+/g, " ").trim();

await emit("verdict", verdict);
await emit("failure-class", failureClass);
// Job outputs are rendered into later shell lines; keep them single-line.
await emit("reason", singleLine(reason));
await emit("cleanup-failure", singleLine(cleanupFailure));

const headline = !failed
  ? "## Canary verdict: PASS"
  : failureClass === "product"
    ? "## Canary verdict: PRODUCT FAILURE -- the supported Minecraft lane broke"
    : failureClass === "infrastructure"
      ? "## Canary verdict: INFRASTRUCTURE FAILURE -- no verdict obtained (not a version break)"
      : "## Canary verdict: UNCLASSIFIED FAILURE -- treated as a product failure";

const lines = [headline, "", `- Minecraft version under test: \`${version}\``, `- Reason: ${reason}`];
if (outcome?.phase) {
  lines.push(`- Failed phase: \`${outcome.phase}\``);
}
if (outcome?.signature) {
  lines.push(`- Transient signature: \`${outcome.signature}\``);
}
if (typeof outcome?.evidenceCount === "number") {
  lines.push(`- Server evidence events: ${outcome.evidenceCount}`);
}
if (failureClass === "infrastructure") {
  lines.push(
    "",
    "> This run does **not** say the supported Minecraft lane is broken. The probe could",
    "> not reach a verdict because an upstream or runner dependency failed.",
  );
}
if (cleanupFailure) {
  lines.push(
    "",
    "### Cleanup failure (did not affect the verdict)",
    "",
    "Every product assertion had already been decided when this happened, so it did not",
    "change the result above. It is reported because it must not be silently swallowed.",
    "",
    "```",
    cleanupFailure,
    "```",
  );
}
if (outcome?.diagnostics?.length) {
  lines.push("", "### Diagnostics", "", ...outcome.diagnostics.map((path) => `- \`${path}\``));
}
if (failed) {
  const tail = await probeLogTail();
  if (tail) {
    lines.push("", "<details><summary>Probe log tail</summary>", "", "```", tail, "```", "", "</details>");
  }
}
await writeSummary(lines);

if (cleanupFailure) {
  console.log(`::warning title=Canary cleanup failure (verdict unaffected)::${singleLine(cleanupFailure)}`);
}
if (failed) {
  const title =
    failureClass === "infrastructure"
      ? "Canary infrastructure failure (not a version break)"
      : failureClass === "product"
        ? "Canary product failure: supported Minecraft lane broke"
        : "Canary failure could not be classified (treated as product failure)";
  console.log(`::error title=${title}::${singleLine(reason)}`);
}

console.log(`canary verdict=${verdict} class=${failureClass || "none"} version=${version}`);
'
