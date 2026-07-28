#!/usr/bin/env bash
set -euo pipefail

test_root="$(mktemp -d)"
trap 'rm -rf "$test_root"' EXIT

run_reporter() {
  local document="$1"
  local output="$test_root/output"
  local summary="$test_root/summary"
  printf '%s\n' "$document" > "$test_root/outcome.json"
  : > "$output"
  : > "$summary"
  CANARY_OUTCOME_PATH="$test_root/outcome.json" \
    CANARY_PROBE_EXIT_CODE=0 \
    GITHUB_OUTPUT="$output" \
    GITHUB_STEP_SUMMARY="$summary" \
    bash scripts/report-canary-outcome.sh > "$test_root/reporter.log"
}

assert_unclassified() {
  rg -q '^verdict<<ghadelim_verdict$' "$test_root/output"
  rg -q '^fail$' "$test_root/output"
  rg -q '^failure-class<<ghadelim_failure-class$' "$test_root/output"
  rg -q '^unclassified$' "$test_root/output"
}

run_reporter '{"verdict":"PASS"}'
rg -q '^pass$' "$test_root/output"

run_reporter '{"minecraftVersion":"26.2"}'
assert_unclassified

run_reporter '{"verdict":"MAYBE"}'
assert_unclassified

run_reporter '{not-json'
assert_unclassified
