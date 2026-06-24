package cli_test

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"
)

func TestScenarioRunJSON(t *testing.T) {
	path := writeScenarioFile(t)

	stdout, stderr, code := execute("--json", "scenario", "run", path)
	if code != 0 {
		t.Fatalf("code = %d, want 0; stderr = %s stdout = %s", code, stderr, stdout)
	}
	assertScenarioJSON(t, stdout)
}

func TestScenarioValidateJSON(t *testing.T) {
	path := writeScenarioFile(t)

	stdout, stderr, code := execute("--json", "scenario", "validate", path)
	if code != 0 {
		t.Fatalf("code = %d, want 0; stderr = %s stdout = %s", code, stderr, stdout)
	}
	assertScenarioJSON(t, stdout)
}

func assertScenarioJSON(t *testing.T, stdout string) {
	t.Helper()
	var payload struct {
		OK    bool `json:"ok"`
		Steps int  `json:"steps"`
	}
	if err := json.Unmarshal([]byte(stdout), &payload); err != nil {
		t.Fatalf("stdout is not JSON: %v; stdout = %q", err, stdout)
	}
	if !payload.OK {
		t.Fatalf("ok = false, want true; payload = %#v", payload)
	}
	if payload.Steps != 4 {
		t.Fatalf("steps = %d, want 4", payload.Steps)
	}
}

func writeScenarioFile(t *testing.T) string {
	t.Helper()
	dir := t.TempDir()
	path := filepath.Join(dir, "scenario.yaml")
	if err := os.WriteFile(path, []byte(`version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
steps:
  - launch: alice
  - connect:
      client: alice
      server: "localhost:25565"
  - chat:
      client: alice
      message: "Welcome alice"
  - wait:
      client: alice
      chat: "/Welcome/"
      timeout: 30s
`), 0o600); err != nil {
		t.Fatal(err)
	}
	return path
}
