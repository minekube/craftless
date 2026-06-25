package cli_test

import (
	"context"
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/minekube/craftwright/internal/engine"
)

func TestClientLaunchUsesConfigDefaults(t *testing.T) {
	dir := t.TempDir()
	writeClientConfig(t, dir, `version: 1
defaults:
  minecraft: "1.20.4"
  loader: vanilla
  offline: false
  timeout: 45s
paths:
  artifacts: test-results/mcw
`)
	recorder := &recordingEngine{}

	stdout, stderr, code := executeWithEngine(recorder, "--json", "--work-dir", dir, "client", "launch", "alice")
	if code != 0 {
		t.Fatalf("code = %d stderr = %s stdout = %s", code, stderr, stdout)
	}
	var payload struct {
		OK     bool          `json:"ok"`
		Client engine.Client `json:"client"`
	}
	if err := json.Unmarshal([]byte(stdout), &payload); err != nil {
		t.Fatalf("stdout is not JSON: %v; stdout = %q", err, stdout)
	}
	if !payload.OK {
		t.Fatalf("payload = %#v", payload)
	}
	if payload.Client.MinecraftVersion != "1.20.4" || payload.Client.Loader != "vanilla" || payload.Client.Offline {
		t.Fatalf("client = %#v", payload.Client)
	}
	if recorder.launch.Timeout != 45*time.Second || recorder.launch.ArtifactsDir != "test-results/mcw" {
		t.Fatalf("launch request = %#v", recorder.launch)
	}
}

func TestClientLaunchFlagsOverrideConfigDefaults(t *testing.T) {
	dir := t.TempDir()
	writeClientConfig(t, dir, `version: 1
defaults:
  minecraft: "1.20.4"
  loader: vanilla
  offline: false
  timeout: 45s
paths:
  artifacts: test-results/mcw
`)
	recorder := &recordingEngine{}

	stdout, stderr, code := executeWithEngine(recorder, "--json", "--work-dir", dir, "client", "launch", "alice", "--mc", "1.21.6", "--loader", "fabric", "--offline", "--timeout", "10s", "--artifacts", "override-artifacts")
	if code != 0 {
		t.Fatalf("code = %d stderr = %s stdout = %s", code, stderr, stdout)
	}
	var payload struct {
		Client engine.Client `json:"client"`
	}
	if err := json.Unmarshal([]byte(stdout), &payload); err != nil {
		t.Fatalf("stdout is not JSON: %v; stdout = %q", err, stdout)
	}
	if payload.Client.MinecraftVersion != "1.21.6" || payload.Client.Loader != "fabric" || !payload.Client.Offline {
		t.Fatalf("client = %#v", payload.Client)
	}
	if recorder.launch.Timeout != 10*time.Second || recorder.launch.ArtifactsDir != "override-artifacts" {
		t.Fatalf("launch request = %#v", recorder.launch)
	}
}

func TestClientLaunchExplicitConfigPath(t *testing.T) {
	dir := t.TempDir()
	configPath := filepath.Join(dir, "custom.yaml")
	if err := os.WriteFile(configPath, []byte(`version: 1
defaults:
  minecraft: "1.19.4"
  loader: quilt
  offline: true
`), 0o644); err != nil {
		t.Fatal(err)
	}

	stdout, stderr, code := execute("--json", "--config", configPath, "client", "launch", "alice")
	if code != 0 {
		t.Fatalf("code = %d stderr = %s stdout = %s", code, stderr, stdout)
	}
	var payload struct {
		Client engine.Client `json:"client"`
	}
	if err := json.Unmarshal([]byte(stdout), &payload); err != nil {
		t.Fatalf("stdout is not JSON: %v; stdout = %q", err, stdout)
	}
	if payload.Client.MinecraftVersion != "1.19.4" || payload.Client.Loader != "quilt" || !payload.Client.Offline {
		t.Fatalf("client = %#v", payload.Client)
	}
}

func TestClientLaunchRejectsEmptyConfigLoader(t *testing.T) {
	dir := t.TempDir()
	writeClientConfig(t, dir, `version: 1
defaults:
  minecraft: "1.21.6"
  loader: ""
`)

	_, stderr, code := execute("--work-dir", dir, "client", "launch", "alice")
	if code != 2 {
		t.Fatalf("code = %d stderr = %s", code, stderr)
	}
	if !strings.Contains(stderr, "--loader is required") {
		t.Fatalf("stderr = %q", stderr)
	}
}

func writeClientConfig(t *testing.T, dir string, content string) {
	t.Helper()
	if err := os.WriteFile(filepath.Join(dir, "craftwright.yaml"), []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}

type recordingEngine struct {
	launch engine.LaunchRequest
}

func (r *recordingEngine) Launch(ctx context.Context, req engine.LaunchRequest) (engine.Client, error) {
	r.launch = req
	return engine.Client{
		Name:             req.Name,
		State:            engine.StateRunning,
		MinecraftVersion: req.MinecraftVersion,
		Loader:           req.Loader,
		Offline:          req.Offline,
		Server:           req.Server,
	}, nil
}

func (r *recordingEngine) List(ctx context.Context) ([]engine.Client, error) {
	return nil, nil
}

func (r *recordingEngine) Status(ctx context.Context, name string) (engine.Client, error) {
	return engine.Client{}, nil
}

func (r *recordingEngine) Connect(ctx context.Context, name string, server string) error {
	return nil
}

func (r *recordingEngine) Chat(ctx context.Context, name string, message string) error {
	return nil
}

func (r *recordingEngine) Wait(ctx context.Context, req engine.WaitRequest) (engine.Event, error) {
	return engine.Event{}, nil
}

func (r *recordingEngine) Logs(ctx context.Context, name string) ([]string, error) {
	return nil, nil
}

func (r *recordingEngine) Stop(ctx context.Context, name string, force bool) error {
	return nil
}
