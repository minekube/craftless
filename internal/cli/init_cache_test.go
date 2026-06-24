package cli_test

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestInitCreatesProjectLayout(t *testing.T) {
	dir := t.TempDir()
	stdout, stderr, code := execute("init", "--dir", dir)
	if code != 0 {
		t.Fatalf("code = %d stderr = %s stdout = %s", code, stderr, stdout)
	}
	if !strings.Contains(stdout, "Initialized Craftwright project") {
		t.Fatalf("stdout = %q", stdout)
	}
	if _, err := os.Stat(filepath.Join(dir, "craftwright.yaml")); err != nil {
		t.Fatal(err)
	}
}

func TestInitJSONCreatesProjectLayout(t *testing.T) {
	dir := t.TempDir()
	stdout, stderr, code := execute("--json", "init", "--dir", dir)
	if code != 0 {
		t.Fatalf("code = %d stderr = %s stdout = %s", code, stderr, stdout)
	}
	var payload struct {
		OK  bool   `json:"ok"`
		Dir string `json:"dir"`
	}
	if err := json.Unmarshal([]byte(stdout), &payload); err != nil {
		t.Fatalf("stdout is not JSON: %v; stdout = %q", err, stdout)
	}
	if !payload.OK || payload.Dir != dir {
		t.Fatalf("payload = %#v", payload)
	}
	if _, err := os.Stat(filepath.Join(dir, "craftwright.yaml")); err != nil {
		t.Fatal(err)
	}
}

func TestInitJSONDryRunDoesNotWriteFiles(t *testing.T) {
	dir := t.TempDir()
	stdout, stderr, code := execute("--json", "init", "--dry-run", "--dir", dir)
	if code != 0 {
		t.Fatalf("code = %d stderr = %s stdout = %s", code, stderr, stdout)
	}
	var payload struct {
		OK     bool   `json:"ok"`
		Dir    string `json:"dir"`
		DryRun bool   `json:"dryRun"`
	}
	if err := json.Unmarshal([]byte(stdout), &payload); err != nil {
		t.Fatalf("stdout is not JSON: %v; stdout = %q", err, stdout)
	}
	if !payload.OK || payload.Dir != dir || !payload.DryRun {
		t.Fatalf("payload = %#v", payload)
	}
	if _, err := os.Stat(filepath.Join(dir, "craftwright.yaml")); !os.IsNotExist(err) {
		t.Fatalf("dry run wrote config: %v", err)
	}
}

func TestCachePrepareJSON(t *testing.T) {
	dir := t.TempDir()
	_, _, code := execute("init", "--dir", dir)
	if code != 0 {
		t.Fatalf("init code = %d", code)
	}
	stdout, stderr, code := execute("--json", "--work-dir", dir, "cache", "prepare", "--mc", "1.21.6", "--loader", "fabric")
	if code != 0 {
		t.Fatalf("code = %d stderr = %s stdout = %s", code, stderr, stdout)
	}
	var payload struct {
		OK    bool `json:"ok"`
		Cache struct {
			Minecraft string `json:"minecraft"`
		} `json:"cache"`
	}
	if err := json.Unmarshal([]byte(stdout), &payload); err != nil {
		t.Fatalf("stdout is not JSON: %v; stdout = %q", err, stdout)
	}
	if !payload.OK || payload.Cache.Minecraft != "1.21.6" {
		t.Fatalf("payload = %#v", payload)
	}
}

func TestCachePrepareJSONMissingMinecraftWritesErrorEnvelope(t *testing.T) {
	stdout, stderr, code := execute("--json", "cache", "prepare")
	if code != 2 {
		t.Fatalf("code = %d stderr = %s stdout = %s", code, stderr, stdout)
	}
	if !strings.Contains(stderr, "--mc is required") {
		t.Fatalf("stderr = %q", stderr)
	}
	var payload struct {
		OK    bool `json:"ok"`
		Error struct {
			Message string `json:"message"`
		} `json:"error"`
	}
	if err := json.Unmarshal([]byte(stdout), &payload); err != nil {
		t.Fatalf("stdout is not JSON: %v; stdout = %q", err, stdout)
	}
	if payload.OK || !strings.Contains(payload.Error.Message, "--mc is required") {
		t.Fatalf("payload = %#v", payload)
	}
}

func TestCachePrepareJSONInvalidProfileWritesErrorEnvelope(t *testing.T) {
	stdout, stderr, code := execute("--json", "cache", "prepare", "--mc", "1.21.6", "--profile", ".")
	if code != 8 {
		t.Fatalf("code = %d stderr = %s stdout = %s", code, stderr, stdout)
	}
	if !strings.Contains(stderr, "profile") {
		t.Fatalf("stderr = %q", stderr)
	}
	var payload struct {
		OK    bool `json:"ok"`
		Error struct {
			Message string `json:"message"`
		} `json:"error"`
	}
	if err := json.Unmarshal([]byte(stdout), &payload); err != nil {
		t.Fatalf("stdout is not JSON: %v; stdout = %q", err, stdout)
	}
	if payload.OK || payload.Error.Message == "" {
		t.Fatalf("payload = %#v", payload)
	}
}

func TestInitInvalidExistingConfigReturnsUsageCode(t *testing.T) {
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "craftwright.yaml"), []byte("version: nope\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	_, stderr, code := execute("init", "--dir", dir)
	if code != 2 {
		t.Fatalf("code = %d stderr = %s", code, stderr)
	}
}

func TestInitRejectsPositionalArgs(t *testing.T) {
	dir := t.TempDir()
	_, stderr, code := execute("init", "--dir", dir, "junk")
	if code != 2 {
		t.Fatalf("code = %d stderr = %s", code, stderr)
	}
}

func TestCachePrepareRejectsPositionalArgs(t *testing.T) {
	dir := t.TempDir()
	_, _, code := execute("init", "--dir", dir)
	if code != 0 {
		t.Fatalf("init code = %d", code)
	}
	_, stderr, code := execute("--work-dir", dir, "cache", "prepare", "junk", "--mc", "1.21.6")
	if code != 2 {
		t.Fatalf("code = %d stderr = %s", code, stderr)
	}
}

func TestCachePrepareRejectsUnsafeIdentifier(t *testing.T) {
	dir := t.TempDir()
	_, _, code := execute("init", "--dir", dir)
	if code != 0 {
		t.Fatalf("init code = %d", code)
	}
	_, stderr, code := execute("--work-dir", dir, "cache", "prepare", "--mc", "1.21.6", "--loader", "fabric", "--profile", "../../../escape")
	if code != 8 {
		t.Fatalf("code = %d stderr = %s", code, stderr)
	}
	if _, err := os.Stat(filepath.Join(filepath.Dir(dir), "escape")); !os.IsNotExist(err) {
		t.Fatalf("escape path was written: %v", err)
	}
}
