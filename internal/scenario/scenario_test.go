package scenario_test

import (
	"context"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/minekube/craftwright/internal/engine"
	"github.com/minekube/craftwright/internal/scenario"
)

func TestRunFileExecutesScenario(t *testing.T) {
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

	result, err := scenario.RunFile(context.Background(), engine.NewMemory(), path)
	if err != nil {
		t.Fatalf("RunFile returned error: %v", err)
	}
	if !result.OK {
		t.Fatalf("OK = false, want true")
	}
	if result.Steps != 4 {
		t.Fatalf("Steps = %d, want 4", result.Steps)
	}
}

func TestValidateFileRejectsInvalidWaitDuration(t *testing.T) {
	path := writeScenario(t, `version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
steps:
  - wait:
      client: alice
      chat: "/Welcome/"
      timeout: nope
`)

	_, err := scenario.ValidateFile(path)
	if err == nil {
		t.Fatal("ValidateFile returned nil error, want invalid duration error")
	}
	if !strings.Contains(err.Error(), "invalid duration") {
		t.Fatalf("error = %v, want invalid duration", err)
	}
}

func TestRunFileRejectsInvalidWaitDurationBeforeEngineCalls(t *testing.T) {
	path := writeScenario(t, `version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
steps:
  - launch: alice
  - wait:
      client: alice
      chat: "/Welcome/"
      timeout: nope
`)
	eng := &recordingEngine{}

	_, err := scenario.RunFile(context.Background(), eng, path)
	if err == nil {
		t.Fatal("RunFile returned nil error, want invalid duration error")
	}
	if !strings.Contains(err.Error(), "invalid duration") {
		t.Fatalf("error = %v, want invalid duration", err)
	}
	if eng.calls != 0 {
		t.Fatalf("engine calls = %d, want 0", eng.calls)
	}
}

func writeScenario(t *testing.T, content string) string {
	t.Helper()
	dir := t.TempDir()
	path := filepath.Join(dir, "scenario.yaml")
	if err := os.WriteFile(path, []byte(content), 0o600); err != nil {
		t.Fatal(err)
	}
	return path
}

type recordingEngine struct {
	calls int
}

func (e *recordingEngine) Launch(context.Context, engine.LaunchRequest) (engine.Client, error) {
	e.calls++
	return engine.Client{}, nil
}

func (e *recordingEngine) List(context.Context) ([]engine.Client, error) {
	e.calls++
	return nil, nil
}

func (e *recordingEngine) Status(context.Context, string) (engine.Client, error) {
	e.calls++
	return engine.Client{}, nil
}

func (e *recordingEngine) Connect(context.Context, string, string) error {
	e.calls++
	return nil
}

func (e *recordingEngine) Chat(context.Context, string, string) error {
	e.calls++
	return nil
}

func (e *recordingEngine) Wait(context.Context, engine.WaitRequest) (engine.Event, error) {
	e.calls++
	return engine.Event{}, nil
}

func (e *recordingEngine) Logs(context.Context, string) ([]string, error) {
	e.calls++
	return nil, nil
}

func (e *recordingEngine) Stop(context.Context, string, bool) error {
	e.calls++
	return nil
}

var _ engine.Engine = (*recordingEngine)(nil)
