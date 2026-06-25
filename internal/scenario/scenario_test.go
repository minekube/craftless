package scenario_test

import (
	"context"
	"errors"
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

func TestValidateFileRejectsUnknownFieldsAndUnsupportedVersion(t *testing.T) {
	unknown := writeScenario(t, "version: 1\nbogus: true\nsteps: []\n")
	if _, err := scenario.ValidateFile(unknown); err == nil || !strings.Contains(err.Error(), "field bogus not found") {
		t.Fatalf("unknown field err = %v", err)
	}

	unsupported := writeScenario(t, "version: 2\nsteps: []\n")
	if _, err := scenario.ValidateFile(unsupported); err == nil || !strings.Contains(err.Error(), "version 2") {
		t.Fatalf("unsupported version err = %v", err)
	}
}

func TestValidateFileRejectsUndefinedClientsAndMultipleActions(t *testing.T) {
	undefined := writeScenario(t, `version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
steps:
  - launch: bob
`)
	if _, err := scenario.ValidateFile(undefined); err == nil || !strings.Contains(err.Error(), "bob") {
		t.Fatalf("undefined client err = %v", err)
	}

	multiple := writeScenario(t, `version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
steps:
  - launch: alice
    chat:
      client: alice
      message: hello
`)
	if _, err := scenario.ValidateFile(multiple); err == nil || !strings.Contains(err.Error(), "exactly one action") {
		t.Fatalf("multiple action err = %v", err)
	}
}

func TestValidateFileRejectsNullActionInMultipleActions(t *testing.T) {
	path := writeScenario(t, `version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
steps:
  - launch:
    chat:
      client: alice
      message: hi
`)
	if _, err := scenario.ValidateFile(path); err == nil || !strings.Contains(err.Error(), "exactly one action") {
		t.Fatalf("null action err = %v", err)
	}
}

func TestRunFileRejectsMissingLaunchClientFieldsBeforeEngineCalls(t *testing.T) {
	path := writeScenario(t, `version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
  bob:
    mc: "1.21.6"
steps:
  - launch: alice
  - launch: bob
`)
	eng := &recordingEngine{}

	if _, err := scenario.ValidateFile(path); err == nil || !strings.Contains(err.Error(), "loader") {
		t.Fatalf("ValidateFile err = %v, want missing loader", err)
	}
	_, err := scenario.RunFile(context.Background(), eng, path)
	if err == nil {
		t.Fatal("RunFile returned nil error, want missing loader error")
	}
	if !strings.Contains(err.Error(), "loader") {
		t.Fatalf("error = %v, want missing loader", err)
	}
	if eng.calls != 0 {
		t.Fatalf("engine calls = %d, want 0", eng.calls)
	}
}

func TestValidateFileRejectsMissingRequiredActionFields(t *testing.T) {
	emptyLaunch := writeScenario(t, `version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
steps:
  - launch: ""
`)
	if _, err := scenario.ValidateFile(emptyLaunch); err == nil || !strings.Contains(err.Error(), "launch") {
		t.Fatalf("empty launch err = %v", err)
	}

	missingServer := writeScenario(t, `version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
steps:
  - connect:
      client: alice
`)
	if _, err := scenario.ValidateFile(missingServer); err == nil || !strings.Contains(err.Error(), "connect.server") {
		t.Fatalf("missing server err = %v", err)
	}
}

func TestValidateFileRejectsMultipleDocuments(t *testing.T) {
	path := writeScenario(t, `version: 1
steps: []
---
version: 1
steps: []
`)
	if _, err := scenario.ValidateFile(path); err == nil || !strings.Contains(err.Error(), "multiple YAML documents") {
		t.Fatalf("multiple documents err = %v", err)
	}
}

func TestRunFileStopsLaunchedClientsOnFailure(t *testing.T) {
	path := writeScenario(t, `version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
steps:
  - launch: alice
  - connect:
      client: alice
      server: "bad"
`)
	eng := &failingConnectEngine{}

	_, err := scenario.RunFile(context.Background(), eng, path)
	if err == nil {
		t.Fatal("RunFile returned nil error")
	}
	if !eng.stoppedAlice {
		t.Fatal("alice was not stopped after failure")
	}
}

func TestRunFileCleanupUsesFreshContext(t *testing.T) {
	path := writeScenario(t, `version: 1
clients:
  alice:
    mc: "1.21.6"
    loader: fabric
    offline: true
steps:
  - launch: alice
  - connect:
      client: alice
      server: "bad"
`)
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	eng := &cleanupContextEngine{}

	_, err := scenario.RunFile(ctx, eng, path)
	if err == nil {
		t.Fatal("RunFile returned nil error")
	}
	if !eng.stopContextUsable {
		t.Fatal("cleanup Stop received canceled context")
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

type failingConnectEngine struct {
	recordingEngine
	stoppedAlice bool
}

func (e *failingConnectEngine) Launch(context.Context, engine.LaunchRequest) (engine.Client, error) {
	e.calls++
	return engine.Client{Name: "alice"}, nil
}

func (e *failingConnectEngine) Connect(context.Context, string, string) error {
	e.calls++
	return errors.New("connect failed")
}

func (e *failingConnectEngine) Stop(_ context.Context, name string, _ bool) error {
	e.calls++
	if name == "alice" {
		e.stoppedAlice = true
	}
	return nil
}

type cleanupContextEngine struct {
	failingConnectEngine
	stopContextUsable bool
}

func (e *cleanupContextEngine) Stop(ctx context.Context, name string, force bool) error {
	if ctx.Err() == nil {
		e.stopContextUsable = true
	}
	return e.failingConnectEngine.Stop(ctx, name, force)
}
