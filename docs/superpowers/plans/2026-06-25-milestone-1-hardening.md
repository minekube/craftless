# Milestone 1 Hardening Implementation Plan

> **Status:** Legacy Go hardening plan. This plan may be mined for tests and
> contract expectations, but it is superseded by the JVM-first rewrite direction
> in `docs/superpowers/specs/2026-06-25-jvm-first-rewrite-design.md`.
> Do not continue it as the main implementation path unless explicitly working
> on a temporary Go compatibility layer.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tighten the Milestone 1 foundation so config, output modes, scenarios, and daemon protocol behavior are safe to build the real Minecraft backend on top of.

**Architecture:** Keep the existing package boundaries. CLI commands continue to depend on `engine.Engine`; scenario validation stays in `internal/scenario`; daemon protocol behavior stays in `internal/daemon`. Each task adds tests first, implements only the reviewed gap, and commits independently.

**Tech Stack:** Go 1.22, Cobra, `gopkg.in/yaml.v3`, standard `testing`, JSON-RPC 2.0 over newline-delimited stdio.

---

## Files And Responsibilities

- `internal/cli/client.go`: resolve `client launch` defaults from config before calling `engine.Launch`.
- `internal/cli/client_config_test.go`: black-box CLI tests for config-backed launch defaults and flag override precedence.
- `internal/cli/output.go`: reject unsupported `--jsonl` until streaming commands implement JSON Lines.
- `internal/cli/output_test.go`: global output mode tests for JSONL policy.
- `internal/scenario/scenario.go`: strict scenario YAML loading, validation, and cleanup on run failure.
- `internal/scenario/scenario_test.go`: validation and cleanup regression tests.
- `internal/daemon/server.go`: return JSON-RPC parse errors and continue reading subsequent requests.
- `internal/daemon/server_test.go`: daemon parse-error resilience tests.
- `README.md`: note reserved flags and hardening status if behavior changes user-visible CLI semantics.

---

## Task 1: Config-Backed Client Launch Defaults

**Files:**
- Modify: `internal/cli/client.go`
- Create: `internal/cli/client_config_test.go`

- [ ] **Step 1: Write failing tests for config-backed launch defaults**

Create `internal/cli/client_config_test.go`:

```go
package cli_test

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"

	"github.com/minekube/craftwright/internal/cli"
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
	stdout, stderr, code := execute("--json", "--work-dir", dir, "client", "launch", "alice")
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
}

func TestClientLaunchFlagsOverrideConfigDefaults(t *testing.T) {
	dir := t.TempDir()
	writeClientConfig(t, dir, `version: 1
defaults:
  minecraft: "1.20.4"
  loader: vanilla
  offline: false
`)
	stdout, stderr, code := execute("--json", "--work-dir", dir, "client", "launch", "alice", "--mc", "1.21.6", "--loader", "fabric", "--offline")
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

func writeClientConfig(t *testing.T, dir string, content string) {
	t.Helper()
	if err := os.WriteFile(filepath.Join(dir, "craftwright.yaml"), []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}

var _ = cli.Execute
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
go test ./internal/cli -run 'TestClientLaunch.*Config' -count=1
```

Expected: fail because `client launch` still requires `--mc` and does not load config defaults.

- [ ] **Step 3: Implement config resolution in `client launch`**

Modify `internal/cli/client.go`:

```go
import (
	"context"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"time"

	"github.com/minekube/craftwright/internal/config"
	"github.com/minekube/craftwright/internal/engine"
	"github.com/spf13/cobra"
)
```

Add helpers:

```go
func loadCLIConfig(opts *GlobalOptions) (config.Config, error) {
	if opts.Config != "" {
		return config.Load(opts.Config)
	}
	root := opts.WorkDir
	if root == "" {
		root = "."
	}
	path := filepath.Join(root, "craftwright.yaml")
	cfg, err := config.Load(path)
	if err == nil {
		return cfg, nil
	}
	if errors.Is(err, os.ErrNotExist) {
		return config.Default(), nil
	}
	return config.Config{}, err
}

func flagChanged(cmd *cobra.Command, name string) bool {
	flag := cmd.Flags().Lookup(name)
	return flag != nil && flag.Changed
}
```

In `newClientLaunchCommand`, initialize defaults from config before constructing `engine.LaunchRequest`:

```go
cfg, err := loadCLIConfig(opts)
if err != nil {
	return err
}
if !flagChanged(cmd, "mc") {
	mc = cfg.Defaults.Minecraft
}
if !flagChanged(cmd, "loader") {
	loader = cfg.Defaults.Loader
}
if !flagChanged(cmd, "offline") {
	offline = cfg.Defaults.Offline
}
if !flagChanged(cmd, "timeout") {
	timeout = cfg.Defaults.Timeout
}
if !flagChanged(cmd, "artifacts") {
	artifacts = cfg.Paths.Artifacts
}
if mc == "" {
	return usageError("--mc is required")
}
if loader == "" {
	return usageError("--loader is required")
}
```

- [ ] **Step 4: Run focused tests**

Run:

```bash
go test ./internal/cli -run 'TestClientLaunch.*Config|TestClientLaunchJSON|TestClientListAndStatusPlain' -count=1
```

Expected: pass.

- [ ] **Step 5: Run all tests and commit**

Run:

```bash
go test ./... -count=1
go vet ./...
git add internal/cli/client.go internal/cli/client_config_test.go
git commit -m "feat: apply config defaults to client launch"
```

---

## Task 2: Explicit JSONL Policy

**Files:**
- Modify: `internal/cli/output.go`
- Modify: `internal/cli/output_test.go`

- [ ] **Step 1: Write failing JSONL policy test**

Append to `internal/cli/output_test.go`:

```go
func TestJSONLIsReservedUntilStreamingCommandsImplementIt(t *testing.T) {
	_, stderr, code := execute("--jsonl", "client", "list")
	if code != 2 {
		t.Fatalf("code = %d stderr = %s", code, stderr)
	}
	if !strings.Contains(stderr, "--jsonl is reserved") {
		t.Fatalf("stderr = %q", stderr)
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
go test ./internal/cli -run TestJSONLIsReserved -count=1
```

Expected: fail because `--jsonl` is accepted and ignored.

- [ ] **Step 3: Reject JSONL globally for now**

Modify `GlobalOptions.Validate` in `internal/cli/output.go`:

```go
if opts.JSONL {
	return usageError("--jsonl is reserved until streaming JSON Lines output is implemented")
}
```

Keep the existing conflict check for `--json` and `--plain`.

- [ ] **Step 4: Run tests and commit**

Run:

```bash
go test ./internal/cli -run 'TestJSONLIsReserved|TestOutputModeRejectsConflictingMachineModes' -count=1
go test ./... -count=1
git add internal/cli/output.go internal/cli/output_test.go
git commit -m "fix: reject reserved jsonl output mode"
```

---

## Task 3: Strict Scenario Validation And Cleanup

**Files:**
- Modify: `internal/scenario/scenario.go`
- Modify: `internal/scenario/scenario_test.go`

- [ ] **Step 1: Write failing validation tests**

Append to `internal/scenario/scenario_test.go`:

```go
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
```

- [ ] **Step 2: Write failing cleanup test**

Append to `internal/scenario/scenario_test.go`:

```go
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
```

Add `errors` to the test imports.

- [ ] **Step 3: Run scenario tests to verify failure**

Run:

```bash
go test ./internal/scenario -count=1
```

Expected: fail on unknown fields, unsupported version, multiple actions, undefined clients, and missing cleanup.

- [ ] **Step 4: Implement strict YAML loading**

Replace `yaml.Unmarshal(data, &file)` in `loadFile` with:

```go
decoder := yaml.NewDecoder(bytes.NewReader(data))
decoder.KnownFields(true)
if err := decoder.Decode(&file); err != nil {
	return File{}, err
}
```

Add imports:

```go
import (
	"bytes"
	"context"
	"fmt"
	"io"
	"os"
	"time"
)
```

Reject multiple documents:

```go
var extra yaml.Node
if err := decoder.Decode(&extra); err != io.EOF {
	if err != nil {
		return File{}, err
	}
	return File{}, fmt.Errorf("multiple YAML documents are not supported")
}
```

- [ ] **Step 5: Implement validation helpers**

Change `Step` to pointer fields:

```go
type Step struct {
	Launch  string       `yaml:"launch"`
	Connect *ConnectStep `yaml:"connect"`
	Chat    *ChatStep    `yaml:"chat"`
	Wait    *WaitStep    `yaml:"wait"`
}
```

Update `validate(file File)` to reject:

```go
if file.Version != 1 {
	return fmt.Errorf("version %d is not supported", file.Version)
}
for i, step := range file.Steps {
	if err := validateStep(file, i+1, step); err != nil {
		return err
	}
}
```

Add these helpers:

```go
func validateStep(file File, number int, step Step) error {
	actions := 0
	if step.Launch != "" {
		actions++
	}
	if step.Connect != nil {
		actions++
	}
	if step.Chat != nil {
		actions++
	}
	if step.Wait != nil {
		actions++
	}
	if actions != 1 {
		return fmt.Errorf("step %d must contain exactly one action", number)
	}
	switch {
	case step.Launch != "":
		return validateClientReference(file, number, "launch", step.Launch)
	case step.Connect != nil:
		if err := validateClientReference(file, number, "connect.client", step.Connect.Client); err != nil {
			return err
		}
		if step.Connect.Server == "" {
			return fmt.Errorf("step %d connect.server is required", number)
		}
	case step.Chat != nil:
		if err := validateClientReference(file, number, "chat.client", step.Chat.Client); err != nil {
			return err
		}
		if step.Chat.Message == "" {
			return fmt.Errorf("step %d chat.message is required", number)
		}
	case step.Wait != nil:
		if err := validateClientReference(file, number, "wait.client", step.Wait.Client); err != nil {
			return err
		}
		if step.Wait.Chat == "" {
			return fmt.Errorf("step %d wait.chat is required", number)
		}
		if _, err := waitTimeout(*step.Wait); err != nil {
			return fmt.Errorf("step %d wait: %w", number, err)
		}
	}
	return nil
}

func validateClientReference(file File, number int, field string, name string) error {
	if name == "" {
		return fmt.Errorf("step %d %s is required", number, field)
	}
	if _, ok := file.Clients[name]; !ok {
		return fmt.Errorf("step %d %s references undefined client %s", number, field, name)
	}
	return nil
}
```

- [ ] **Step 6: Add cleanup in `RunFile`**

Track launched clients and route every execution failure through cleanup:

```go
var launched []string
for i, step := range file.Steps {
	number := i + 1
	switch {
	case step.Launch != "":
		client := file.Clients[step.Launch]
		if _, err := eng.Launch(ctx, engine.LaunchRequest{
			Name:             step.Launch,
			MinecraftVersion: client.MC,
			Loader:           client.Loader,
			Offline:          client.Offline,
		}); err != nil {
			stopLaunched(ctx, eng, launched)
			return Result{}, fmt.Errorf("step %d launch: %w", number, err)
		}
		launched = append(launched, step.Launch)
	case step.Connect != nil:
		if err := eng.Connect(ctx, step.Connect.Client, step.Connect.Server); err != nil {
			stopLaunched(ctx, eng, launched)
			return Result{}, fmt.Errorf("step %d connect: %w", number, err)
		}
	case step.Chat != nil:
		if err := eng.Chat(ctx, step.Chat.Client, step.Chat.Message); err != nil {
			stopLaunched(ctx, eng, launched)
			return Result{}, fmt.Errorf("step %d chat: %w", number, err)
		}
	case step.Wait != nil:
		timeout, err := waitTimeout(*step.Wait)
		if err != nil {
			stopLaunched(ctx, eng, launched)
			return Result{}, fmt.Errorf("step %d wait: %w", number, err)
		}
		if _, err := eng.Wait(ctx, engine.WaitRequest{Client: step.Wait.Client, ChatPattern: step.Wait.Chat, Timeout: timeout}); err != nil {
			stopLaunched(ctx, eng, launched)
			return Result{}, fmt.Errorf("step %d wait: %w", number, err)
		}
	default:
		return Result{}, fmt.Errorf("step %d: empty step", number)
	}
	steps++
}
```

Add:

```go
func stopLaunched(ctx context.Context, eng engine.Engine, launched []string) {
	for i := len(launched) - 1; i >= 0; i-- {
		_ = eng.Stop(ctx, launched[i], true)
	}
}
```

- [ ] **Step 7: Run tests and commit**

Run:

```bash
go test ./internal/scenario -count=1
go test ./... -count=1
go vet ./...
git add internal/scenario/scenario.go internal/scenario/scenario_test.go
git commit -m "fix: harden scenario validation and cleanup"
```

---

## Task 4: JSON-RPC Parse Error Responses

**Files:**
- Modify: `internal/daemon/server.go`
- Modify: `internal/daemon/server_test.go`

- [ ] **Step 1: Write failing parse-error resilience test**

Append to `internal/daemon/server_test.go`:

```go
func TestServeParseErrorReturnsRPCErrorAndContinues(t *testing.T) {
	input := strings.NewReader("{not-json\n" +
		`{"jsonrpc":"2.0","id":2,"method":"client.status","params":{"name":"missing"}}` + "\n")
	var output bytes.Buffer
	if err := daemon.Serve(context.Background(), engine.NewMemory(), input, &output); err != nil {
		t.Fatalf("Serve returned error: %v", err)
	}
	responses := decodeRPCResponses(t, output.String())
	if len(responses) != 2 {
		t.Fatalf("len(responses) = %d output = %q", len(responses), output.String())
	}
	assertRPCError(t, responses[0], "null", -32700)
	assertRPCError(t, responses[1], "2", -32000)
}
```

- [ ] **Step 2: Run daemon tests to verify failure**

Run:

```bash
go test ./internal/daemon -run TestServeParseErrorReturnsRPCErrorAndContinues -count=1
```

Expected: fail because `Serve` returns a JSON parse error instead of encoding a JSON-RPC error and continuing.

- [ ] **Step 3: Implement parse-error response**

In `Serve`, replace the malformed JSON return:

```go
if err := json.Unmarshal(scanner.Bytes(), &req); err != nil {
	resp := response{
		JSONRPC: "2.0",
		ID:      json.RawMessage("null"),
		Error:   newRPCError(-32700, err),
	}
	if err := encoder.Encode(resp); err != nil {
		return err
	}
	continue
}
```

- [ ] **Step 4: Run tests and commit**

Run:

```bash
go test ./internal/daemon -count=1
go test ./... -count=1
go vet ./...
git add internal/daemon/server.go internal/daemon/server_test.go
git commit -m "fix: keep daemon alive after parse errors"
```

---

## Task 5: README Hardening Notes

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README status notes**

Add under `## Status`:

```markdown
Milestone 1 hardening covers config-backed launch defaults, explicit reserved
output modes, strict scenario validation with cleanup, and daemon parse-error
resilience before the real-client backend lands.
```

- [ ] **Step 2: Run verification and commit**

Run:

```bash
go test ./... -count=1
git add README.md
git commit -m "docs: note milestone 1 hardening scope"
```

---

## Plan Self-Review

- Spec coverage: Covers all final review important findings except the historical-event cursor, which is deferred to the real-client backend design because current scenario tests intentionally use retained memory events.
- Placeholder scan: No TBD/TODO/fill-in placeholders are present.
- Type consistency: Uses existing `config.Config`, `engine.Engine`, `scenario.Result`, `daemon.Serve`, and CLI test helpers.
