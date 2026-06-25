# Real Client Backend Implementation Plan

> **Status:** Legacy Go/HeadlessMC wrapper plan. This plan is retained as
> research material and a possible short-lived evidence spike, but it is
> superseded by the JVM-first rewrite direction in
> `docs/superpowers/specs/2026-06-25-jvm-first-rewrite-design.md`.
> Do not implement this as the final Craftwright real-client architecture.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first HeadlessMC/HMC-Specifics real-client backend for long-lived `mcw scenario run` and `mcw daemon --stdio` control, with fake-process tests by default and an opt-in real smoke path.

**Architecture:** Keep `engine.Engine` as the public boundary. Add an `internal/headlessmc` package for cache metadata, command construction, process supervision, output parsing, event buffering, and the real engine implementation. Wire backend selection through config for long-lived commands only, and avoid pretending one-shot CLI invocations can persist real clients before a background daemon transport exists.

**Tech Stack:** Go, Cobra, JSON-RPC over stdio, `os/exec`, `net/http`, YAML config, HeadlessMC 2.9.0, HMC-Specifics 2.4.0.

---

## File Structure

- Modify `internal/config/config.go`: add backend config and defaults.
- Modify `internal/project/project.go`: write backend defaults and richer cache metadata.
- Modify `internal/project/project_test.go`: cover metadata and config defaults.
- Create `internal/headlessmc/cache.go`: resolve pinned upstream URLs/checksums and prepare local metadata.
- Create `internal/headlessmc/cache_test.go`: deterministic cache tests without network.
- Create `internal/headlessmc/commands.go`: build HeadlessMC and HMC-Specifics command strings.
- Create `internal/headlessmc/commands_test.go`: command construction tests.
- Create `internal/headlessmc/parser.go`: parse stdout/stderr lines into Craftwright events.
- Create `internal/headlessmc/parser_test.go`: chat/connect/log parser tests.
- Create `internal/headlessmc/events.go`: bounded event/log buffer with wait cursors.
- Create `internal/headlessmc/events_test.go`: wait cursor and timeout tests.
- Create `internal/headlessmc/process.go`: process runner abstraction and OS runner.
- Create `internal/headlessmc/engine.go`: `engine.Engine` implementation.
- Create `internal/headlessmc/engine_test.go`: fake-process launch/connect/chat/wait/logs/stop tests.
- Modify `internal/daemon/server.go`: add connect/chat/wait/logs/stop JSON-RPC methods.
- Modify `internal/daemon/server_test.go`: daemon method tests.
- Modify `internal/cli/root.go`: add lazy engine factory support for long-lived commands.
- Modify `internal/cli/scenario.go` and `internal/cli/daemon.go`: request backend engine at command execution time.
- Modify `cmd/mcw/main.go`: use the lazy engine factory instead of hard-coded memory.
- Create `internal/headlessmc/real_smoke_test.go`: opt-in real-client smoke test guarded by `CRAFTWRIGHT_REAL_CLIENT=1`.
- Modify `README.md`: document backend status, process ownership, and smoke command.

---

### Task 1: Backend Config

**Files:**
- Modify: `internal/config/config.go`
- Modify: `internal/config/config_test.go`
- Modify: `internal/project/project.go`
- Modify: `internal/project/project_test.go`

- [x] **Step 1: Write failing config tests**

Add tests to `internal/config/config_test.go`:

```go
func TestDefaultIncludesHeadlessMCBackend(t *testing.T) {
	cfg := config.Default()
	if cfg.Backend.Type != "memory" {
		t.Fatalf("backend type = %q, want memory", cfg.Backend.Type)
	}
	if cfg.Backend.HeadlessMCVersion != "2.9.0" {
		t.Fatalf("headlessmc version = %q", cfg.Backend.HeadlessMCVersion)
	}
	if cfg.Backend.SpecificsVersion != "2.4.0" {
		t.Fatalf("specifics version = %q", cfg.Backend.SpecificsVersion)
	}
	if cfg.Backend.Java != "java" {
		t.Fatalf("java = %q, want java", cfg.Backend.Java)
	}
	if len(cfg.Backend.JVMArgs) != 2 {
		t.Fatalf("jvm args = %#v", cfg.Backend.JVMArgs)
	}
}

func TestLoadBackendConfig(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "craftwright.yaml")
	if err := os.WriteFile(path, []byte(`version: 1
defaults:
  minecraft: "1.21.6"
  loader: fabric
  offline: true
  timeout: 2m
backend:
  type: headlessmc
  headlessmcVersion: "2.9.0"
  specificsVersion: "2.4.0"
  java: "/usr/bin/java"
  jvmArgs:
    - "-Djava.awt.headless=true"
    - "-Xmx1G"
paths:
  artifacts: .craftwright/artifacts
  cache: .craftwright/cache
`), 0o644); err != nil {
		t.Fatal(err)
	}

	cfg, err := config.Load(path)
	if err != nil {
		t.Fatal(err)
	}
	if cfg.Backend.Type != "headlessmc" || cfg.Backend.Java != "/usr/bin/java" {
		t.Fatalf("backend = %#v", cfg.Backend)
	}
	if got := strings.Join(cfg.Backend.JVMArgs, " "); got != "-Djava.awt.headless=true -Xmx1G" {
		t.Fatalf("jvm args = %q", got)
	}
}
```

Add a project init assertion in `internal/project/project_test.go`:

```go
func TestInitWritesBackendDefaults(t *testing.T) {
	dir := t.TempDir()
	if err := project.Init(project.Layout{Root: dir}, false); err != nil {
		t.Fatal(err)
	}
	data, err := os.ReadFile(filepath.Join(dir, "craftwright.yaml"))
	if err != nil {
		t.Fatal(err)
	}
	for _, want := range []string{
		"backend:",
		"type: memory",
		"headlessmcVersion: \"2.9.0\"",
		"specificsVersion: \"2.4.0\"",
	} {
		if !bytes.Contains(data, []byte(want)) {
			t.Fatalf("config missing %q:\n%s", want, data)
		}
	}
}
```

- [x] **Step 2: Run tests to verify failure**

Run:

```sh
go test ./internal/config ./internal/project -count=1
```

Expected: fail with missing `Config.Backend`.

- [x] **Step 3: Add config structs and defaults**

In `internal/config/config.go`, add:

```go
type Config struct {
	Version  int      `yaml:"version"`
	Defaults Defaults `yaml:"defaults"`
	Backend  Backend  `yaml:"backend"`
	Paths    Paths    `yaml:"paths"`
}

type Backend struct {
	Type               string   `yaml:"type"`
	HeadlessMCVersion  string   `yaml:"headlessmcVersion"`
	SpecificsVersion   string   `yaml:"specificsVersion"`
	Java               string   `yaml:"java"`
	JVMArgs            []string `yaml:"jvmArgs"`
}
```

Update `Default()`:

```go
Backend: Backend{
	Type:              "memory",
	HeadlessMCVersion: "2.9.0",
	SpecificsVersion:  "2.4.0",
	Java:              "java",
	JVMArgs: []string{
		"-Djava.awt.headless=true",
		"-Xmx2G",
	},
},
```

Update `defaultConfig` in `internal/project/project.go`:

```yaml
backend:
  type: memory
  headlessmcVersion: "2.9.0"
  specificsVersion: "2.4.0"
  java: java
  jvmArgs:
    - "-Djava.awt.headless=true"
    - "-Xmx2G"
```

- [x] **Step 4: Run tests**

Run:

```sh
go test ./internal/config ./internal/project -count=1
```

Expected: pass.

- [x] **Step 5: Commit**

```sh
git add internal/config/config.go internal/config/config_test.go internal/project/project.go internal/project/project_test.go
git commit -m "feat: add backend config defaults"
```

---

### Task 2: HeadlessMC Cache Metadata

**Files:**
- Create: `internal/headlessmc/cache.go`
- Create: `internal/headlessmc/cache_test.go`
- Modify: `internal/project/project.go`
- Modify: `internal/project/project_test.go`

- [ ] **Step 1: Write failing cache tests**

Create `internal/headlessmc/cache_test.go`:

```go
package headlessmc_test

import (
	"context"
	"os"
	"path/filepath"
	"testing"

	"github.com/minekube/craftwright/internal/config"
	"github.com/minekube/craftwright/internal/headlessmc"
)

func TestPrepareWritesPinnedMetadata(t *testing.T) {
	dir := t.TempDir()
	cfg := config.Default()
	cfg.Backend.Type = "headlessmc"

	record, err := headlessmc.Prepare(context.Background(), headlessmc.PrepareRequest{
		Root:    dir,
		Profile: "default",
		MC:      "1.21.6",
		Loader:  "fabric",
		Backend: cfg.Backend,
	})
	if err != nil {
		t.Fatal(err)
	}
	if record.HeadlessMCVersion != "2.9.0" || record.SpecificsVersion != "2.4.0" {
		t.Fatalf("record = %#v", record)
	}
	if record.LauncherURL == "" || record.SpecificsURL == "" {
		t.Fatalf("missing URLs: %#v", record)
	}
	if _, err := os.Stat(filepath.Join(dir, ".craftwright/cache/default/headlessmc-1.21.6-fabric.json")); err != nil {
		t.Fatalf("metadata missing: %v", err)
	}
}

func TestPrepareRejectsUnsupportedLoader(t *testing.T) {
	_, err := headlessmc.Prepare(context.Background(), headlessmc.PrepareRequest{
		Root:    t.TempDir(),
		Profile: "default",
		MC:      "1.21.6",
		Loader:  "vanilla",
		Backend: config.Default().Backend,
	})
	if err == nil {
		t.Fatal("expected error")
	}
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```sh
go test ./internal/headlessmc -count=1
```

Expected: fail because package does not exist.

- [ ] **Step 3: Implement metadata resolver**

Create `internal/headlessmc/cache.go`:

```go
package headlessmc

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/minekube/craftwright/internal/config"
)

type PrepareRequest struct {
	Root    string
	Profile string
	MC      string
	Loader  string
	Backend config.Backend
}

type CacheRecord struct {
	Minecraft          string    `json:"minecraft"`
	Loader             string    `json:"loader"`
	Profile            string    `json:"profile"`
	HeadlessMCVersion  string    `json:"headlessmcVersion"`
	SpecificsVersion   string    `json:"specificsVersion"`
	LauncherURL         string    `json:"launcherUrl"`
	SpecificsURL        string    `json:"specificsUrl"`
	PreparedAt          time.Time `json:"preparedAt"`
}

func Prepare(ctx context.Context, req PrepareRequest) (CacheRecord, error) {
	if err := ctx.Err(); err != nil {
		return CacheRecord{}, err
	}
	if req.Root == "" {
		req.Root = "."
	}
	if req.Profile == "" {
		req.Profile = "default"
	}
	if req.Loader != "fabric" {
		return CacheRecord{}, fmt.Errorf("headlessmc backend supports fabric first, got %q", req.Loader)
	}
	if req.MC == "" {
		return CacheRecord{}, fmt.Errorf("minecraft version is required")
	}
	if req.Backend.HeadlessMCVersion == "" {
		req.Backend.HeadlessMCVersion = "2.9.0"
	}
	if req.Backend.SpecificsVersion == "" {
		req.Backend.SpecificsVersion = "2.4.0"
	}

	record := CacheRecord{
		Minecraft:         req.MC,
		Loader:            req.Loader,
		Profile:           req.Profile,
		HeadlessMCVersion: req.Backend.HeadlessMCVersion,
		SpecificsVersion:  req.Backend.SpecificsVersion,
		LauncherURL:        launcherURL(req.Backend.HeadlessMCVersion),
		SpecificsURL:       specificsURL(req.MC, req.Backend.SpecificsVersion, req.Loader),
		PreparedAt:         time.Now().UTC(),
	}
	dir := filepath.Join(req.Root, ".craftwright", "cache", req.Profile)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return CacheRecord{}, err
	}
	path := filepath.Join(dir, fmt.Sprintf("headlessmc-%s-%s.json", req.MC, req.Loader))
	data, err := json.MarshalIndent(record, "", "  ")
	if err != nil {
		return CacheRecord{}, err
	}
	data = append(data, '\n')
	if err := os.WriteFile(path, data, 0o644); err != nil {
		return CacheRecord{}, err
	}
	return record, nil
}

func launcherURL(version string) string {
	return fmt.Sprintf("https://github.com/headlesshq/headlessmc/releases/download/%s/headlessmc-launcher-wrapper-%s.jar", version, version)
}

func specificsURL(mc string, version string, loader string) string {
	return fmt.Sprintf("https://github.com/headlesshq/hmc-specifics/releases/download/%s/hmc-specifics-%s-%s-%s-release.jar", version, mc, version, loader)
}
```

- [ ] **Step 4: Wire `mcw cache prepare` to HeadlessMC metadata when requested**

Update `internal/project/project.go` `CacheRecord` to include optional fields:

```go
Backend            string `json:"backend,omitempty"`
HeadlessMCVersion  string `json:"headlessmcVersion,omitempty"`
SpecificsVersion   string `json:"specificsVersion,omitempty"`
LauncherURL         string `json:"launcherUrl,omitempty"`
SpecificsURL        string `json:"specificsUrl,omitempty"`
```

In `internal/cli/cache.go`, load config and call `headlessmc.Prepare` when
`cfg.Backend.Type == "headlessmc"`. Keep the existing metadata path for memory.

- [ ] **Step 5: Run tests**

Run:

```sh
go test ./internal/headlessmc ./internal/project ./internal/cli -count=1
```

Expected: pass.

- [ ] **Step 6: Commit**

```sh
git add internal/headlessmc/cache.go internal/headlessmc/cache_test.go internal/project/project.go internal/project/project_test.go internal/cli/cache.go
git commit -m "feat: prepare headlessmc cache metadata"
```

---

### Task 3: Command Construction

**Files:**
- Create: `internal/headlessmc/commands.go`
- Create: `internal/headlessmc/commands_test.go`

- [ ] **Step 1: Write failing tests**

Create `internal/headlessmc/commands_test.go`:

```go
package headlessmc_test

import (
	"reflect"
	"testing"

	"github.com/minekube/craftwright/internal/config"
	"github.com/minekube/craftwright/internal/engine"
	"github.com/minekube/craftwright/internal/headlessmc"
)

func TestLaunchCommand(t *testing.T) {
	cmd := headlessmc.LaunchCommand(engine.LaunchRequest{
		MinecraftVersion: "1.21.6",
		Loader:           "fabric",
		Offline:          true,
	}, config.Backend{JVMArgs: []string{"-Djava.awt.headless=true", "-Xmx2G"}})
	want := `launch fabric:1.21.6 -offline -specifics -lwjgl --jvm "-Djava.awt.headless=true -Xmx2G"`
	if cmd != want {
		t.Fatalf("command = %q, want %q", cmd, want)
	}
}

func TestSplitServer(t *testing.T) {
	tests := map[string]struct {
		host string
		port int
	}{
		"localhost":       {"localhost", 25565},
		"localhost:25566": {"localhost", 25566},
	}
	for input, want := range tests {
		host, port, err := headlessmc.SplitServer(input)
		if err != nil {
			t.Fatalf("%s: %v", input, err)
		}
		if !reflect.DeepEqual([]any{host, port}, []any{want.host, want.port}) {
			t.Fatalf("%s = %s %d, want %s %d", input, host, port, want.host, want.port)
		}
	}
}

func TestBridgeCommands(t *testing.T) {
	if got := headlessmc.ConnectCommand("127.0.0.1:25566"); got != "connect 127.0.0.1 25566" {
		t.Fatalf("connect = %q", got)
	}
	if got := headlessmc.ChatCommand("hello world"); got != ". hello world" {
		t.Fatalf("chat = %q", got)
	}
	if got := headlessmc.StopCommand(); got != "quit" {
		t.Fatalf("stop = %q", got)
	}
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```sh
go test ./internal/headlessmc -count=1
```

Expected: fail with undefined command helpers.

- [ ] **Step 3: Implement command helpers**

Create `internal/headlessmc/commands.go`:

```go
package headlessmc

import (
	"fmt"
	"net"
	"strconv"
	"strings"

	"github.com/minekube/craftwright/internal/config"
	"github.com/minekube/craftwright/internal/engine"
)

func LaunchCommand(req engine.LaunchRequest, backend config.Backend) string {
	version := req.MinecraftVersion
	if req.Loader != "" && req.Loader != "vanilla" {
		version = req.Loader + ":" + req.MinecraftVersion
	}
	parts := []string{"launch", version}
	if req.Offline {
		parts = append(parts, "-offline")
	}
	parts = append(parts, "-specifics", "-lwjgl")
	if len(backend.JVMArgs) > 0 {
		parts = append(parts, "--jvm", quote(strings.Join(backend.JVMArgs, " ")))
	}
	return strings.Join(parts, " ")
}

func ConnectCommand(server string) string {
	host, port, err := SplitServer(server)
	if err != nil {
		return "connect " + server
	}
	return fmt.Sprintf("connect %s %d", host, port)
}

func ChatCommand(message string) string {
	return ". " + message
}

func StopCommand() string {
	return "quit"
}

func SplitServer(server string) (string, int, error) {
	host, portText, err := net.SplitHostPort(server)
	if err != nil {
		if strings.Contains(err.Error(), "missing port in address") {
			return server, 25565, nil
		}
		return "", 0, err
	}
	port, err := strconv.Atoi(portText)
	if err != nil {
		return "", 0, err
	}
	return host, port, nil
}

func quote(value string) string {
	return `"` + strings.ReplaceAll(value, `"`, `\"`) + `"`
}
```

- [ ] **Step 4: Run tests**

Run:

```sh
go test ./internal/headlessmc -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```sh
git add internal/headlessmc/commands.go internal/headlessmc/commands_test.go
git commit -m "feat: build headlessmc command strings"
```

---

### Task 4: Parser And Event Buffer

**Files:**
- Create: `internal/headlessmc/parser.go`
- Create: `internal/headlessmc/parser_test.go`
- Create: `internal/headlessmc/events.go`
- Create: `internal/headlessmc/events_test.go`
- Modify: `internal/engine/engine.go`

- [ ] **Step 1: Extend event fields with JSON-compatible metadata**

Add to `internal/engine/engine.go`:

```go
const (
	EventChat  EventType = "client.chat"
	EventState EventType = "client.state"
	EventLog   EventType = "client.log"
)

type Event struct {
	Type    EventType `json:"type"`
	Client  string    `json:"client"`
	Message string    `json:"message,omitempty"`
	State   State     `json:"state,omitempty"`
	Level   string    `json:"level,omitempty"`
	Server  string    `json:"server,omitempty"`
}
```

- [ ] **Step 2: Write failing parser tests**

Create `internal/headlessmc/parser_test.go`:

```go
package headlessmc_test

import (
	"testing"

	"github.com/minekube/craftwright/internal/engine"
	"github.com/minekube/craftwright/internal/headlessmc"
)

func TestParseChatLine(t *testing.T) {
	event, ok := headlessmc.ParseLine("alice", `[12:00:00] [Render thread/INFO]: [CHAT] Welcome alice`)
	if !ok {
		t.Fatal("line was not parsed")
	}
	if event.Type != engine.EventChat || event.Message != "Welcome alice" {
		t.Fatalf("event = %#v", event)
	}
}

func TestParseConnectIntentLine(t *testing.T) {
	event, ok := headlessmc.ParseLine("alice", `Connecting to server localhost at port 25565...`)
	if !ok {
		t.Fatal("line was not parsed")
	}
	if event.Type != engine.EventState || event.State != engine.StateConnected || event.Server != "localhost:25565" {
		t.Fatalf("event = %#v", event)
	}
}

func TestParseGenericLogLine(t *testing.T) {
	event, ok := headlessmc.ParseLine("alice", `Downloading 1.21.6...`)
	if !ok {
		t.Fatal("line was not parsed")
	}
	if event.Type != engine.EventLog || event.Message != "Downloading 1.21.6..." {
		t.Fatalf("event = %#v", event)
	}
}
```

- [ ] **Step 3: Write failing event cursor tests**

Create `internal/headlessmc/events_test.go`:

```go
package headlessmc_test

import (
	"context"
	"testing"
	"time"

	"github.com/minekube/craftwright/internal/engine"
	"github.com/minekube/craftwright/internal/headlessmc"
)

func TestEventBufferWaitStartsAfterCursor(t *testing.T) {
	buf := headlessmc.NewEventBuffer(100)
	buf.Append(engine.Event{Type: engine.EventChat, Client: "alice", Message: "old"})
	cursor := buf.Cursor()
	buf.Append(engine.Event{Type: engine.EventChat, Client: "alice", Message: "new"})

	event, err := buf.Wait(context.Background(), cursor, engine.WaitRequest{
		Client:      "alice",
		ChatPattern: "new",
		Timeout:     time.Second,
	})
	if err != nil {
		t.Fatal(err)
	}
	if event.Message != "new" {
		t.Fatalf("event = %#v", event)
	}
}

func TestEventBufferWaitTimeout(t *testing.T) {
	buf := headlessmc.NewEventBuffer(100)
	_, err := buf.Wait(context.Background(), buf.Cursor(), engine.WaitRequest{
		Client:      "alice",
		ChatPattern: "missing",
		Timeout:     10 * time.Millisecond,
	})
	if err == nil {
		t.Fatal("expected timeout")
	}
}
```

- [ ] **Step 4: Run tests to verify failure**

Run:

```sh
go test ./internal/headlessmc ./internal/engine -count=1
```

Expected: fail with undefined parser and buffer.

- [ ] **Step 5: Implement parser and buffer**

Create `internal/headlessmc/parser.go`:

```go
package headlessmc

import (
	"regexp"
	"strings"

	"github.com/minekube/craftwright/internal/engine"
)

var connectLine = regexp.MustCompile(`Connecting to server (.+) at port ([0-9]+)\.\.\.`)

func ParseLine(client string, line string) (engine.Event, bool) {
	line = strings.TrimSpace(line)
	if line == "" {
		return engine.Event{}, false
	}
	if idx := strings.Index(line, "[CHAT]"); idx >= 0 {
		return engine.Event{Type: engine.EventChat, Client: client, Message: strings.TrimSpace(line[idx+len("[CHAT]"):])}, true
	}
	if match := connectLine.FindStringSubmatch(line); match != nil {
		return engine.Event{Type: engine.EventState, Client: client, State: engine.StateConnected, Server: match[1] + ":" + match[2]}, true
	}
	return engine.Event{Type: engine.EventLog, Client: client, Level: "info", Message: line}, true
}
```

Create `internal/headlessmc/events.go` with a mutex, condition variable, cursor
index, bounded append, regex matching, timeout timer, and context cancellation.
The matching behavior should mirror `memoryEngine.Wait` but start scanning at
the provided cursor.

- [ ] **Step 6: Run tests**

Run:

```sh
go test ./internal/headlessmc ./internal/engine -count=1
```

Expected: pass.

- [ ] **Step 7: Commit**

```sh
git add internal/engine/engine.go internal/headlessmc/parser.go internal/headlessmc/parser_test.go internal/headlessmc/events.go internal/headlessmc/events_test.go
git commit -m "feat: parse headlessmc events"
```

---

### Task 5: Process Runner And Fake Process

**Files:**
- Create: `internal/headlessmc/process.go`
- Create: `internal/headlessmc/process_test.go`

- [ ] **Step 1: Write failing process runner tests**

Create `internal/headlessmc/process_test.go`:

```go
package headlessmc_test

import (
	"context"
	"strings"
	"testing"

	"github.com/minekube/craftwright/internal/headlessmc"
)

func TestOSCommandSpec(t *testing.T) {
	spec := headlessmc.CommandSpec{
		Java:        "java",
		LauncherJar: "/cache/headlessmc-launcher-wrapper-2.9.0.jar",
		WorkDir:     "/work",
	}
	if got := strings.Join(spec.Args(), " "); got != `-jar /cache/headlessmc-launcher-wrapper-2.9.0.jar` {
		t.Fatalf("args = %q", got)
	}
}

func TestFakeRunnerCapturesInput(t *testing.T) {
	runner := headlessmc.NewFakeRunner()
	proc, err := runner.Start(context.Background(), headlessmc.CommandSpec{Java: "java"})
	if err != nil {
		t.Fatal(err)
	}
	if err := proc.WriteLine("launch fabric:1.21.6 -offline"); err != nil {
		t.Fatal(err)
	}
	if got := runner.Commands(); len(got) != 1 || got[0] != "launch fabric:1.21.6 -offline" {
		t.Fatalf("commands = %#v", got)
	}
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```sh
go test ./internal/headlessmc -count=1
```

Expected: fail with undefined process types.

- [ ] **Step 3: Implement runner interfaces**

Create `internal/headlessmc/process.go`:

```go
package headlessmc

import (
	"bufio"
	"context"
	"io"
	"os/exec"
	"sync"
)

type CommandSpec struct {
	Java        string
	LauncherJar string
	WorkDir     string
}

func (s CommandSpec) Args() []string {
	return []string{"-jar", s.LauncherJar}
}

type Runner interface {
	Start(context.Context, CommandSpec) (Process, error)
}

type Process interface {
	WriteLine(string) error
	Stdout() <-chan string
	Stderr() <-chan string
	Wait() error
	Kill() error
}

type OSRunner struct{}
```

Implement `OSRunner.Start` with `exec.CommandContext`, stdin pipe, stdout/stderr
scanner goroutines, and process kill support.

Implement `FakeRunner`, `FakeProcess`, `EmitStdout`, `EmitStderr`, `Exit`, and
`Commands()` in the same file or a test-only file if preferred. Keep exported
fake helpers if engine tests need them from `headlessmc_test`.

- [ ] **Step 4: Run tests**

Run:

```sh
go test ./internal/headlessmc -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```sh
git add internal/headlessmc/process.go internal/headlessmc/process_test.go
git commit -m "feat: add headlessmc process runner"
```

---

### Task 6: HeadlessMC Engine With Fake Process

**Files:**
- Create: `internal/headlessmc/engine.go`
- Create: `internal/headlessmc/engine_test.go`

- [ ] **Step 1: Write failing engine tests**

Create `internal/headlessmc/engine_test.go`:

```go
package headlessmc_test

import (
	"context"
	"strings"
	"testing"
	"time"

	"github.com/minekube/craftwright/internal/config"
	"github.com/minekube/craftwright/internal/engine"
	"github.com/minekube/craftwright/internal/headlessmc"
)

func TestEngineLaunchConnectChatWaitLogsStop(t *testing.T) {
	runner := headlessmc.NewFakeRunner()
	eng := headlessmc.New(headlessmc.Options{
		Root:        t.TempDir(),
		Backend:     config.Default().Backend,
		Runner:      runner,
		LauncherJar: "/cache/headlessmc-launcher-wrapper-2.9.0.jar",
	})

	client, err := eng.Launch(context.Background(), engine.LaunchRequest{
		Name:             "alice",
		MinecraftVersion: "1.21.6",
		Loader:           "fabric",
		Offline:          true,
		Timeout:          time.Second,
	})
	if err != nil {
		t.Fatal(err)
	}
	if client.State != engine.StateRunning {
		t.Fatalf("client = %#v", client)
	}
	if !strings.Contains(strings.Join(runner.Commands(), "\n"), "launch fabric:1.21.6 -offline -specifics -lwjgl") {
		t.Fatalf("commands = %#v", runner.Commands())
	}

	if err := eng.Connect(context.Background(), "alice", "localhost:25565"); err != nil {
		t.Fatal(err)
	}
	if err := eng.Chat(context.Background(), "alice", "hello"); err != nil {
		t.Fatal(err)
	}
	runner.EmitStdout("alice", `[12:00:00] [Render thread/INFO]: [CHAT] Welcome alice`)
	event, err := eng.Wait(context.Background(), engine.WaitRequest{Client: "alice", ChatPattern: "Welcome", Timeout: time.Second})
	if err != nil {
		t.Fatal(err)
	}
	if event.Message != "Welcome alice" {
		t.Fatalf("event = %#v", event)
	}
	logs, err := eng.Logs(context.Background(), "alice")
	if err != nil {
		t.Fatal(err)
	}
	if len(logs) == 0 {
		t.Fatal("logs empty")
	}
	if err := eng.Stop(context.Background(), "alice", false); err != nil {
		t.Fatal(err)
	}
	status, err := eng.Status(context.Background(), "alice")
	if err != nil {
		t.Fatal(err)
	}
	if status.State != engine.StateStopped {
		t.Fatalf("status = %#v", status)
	}
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```sh
go test ./internal/headlessmc -count=1
```

Expected: fail with undefined `New` and `Options`.

- [ ] **Step 3: Implement engine**

Create `internal/headlessmc/engine.go` with:

```go
type Options struct {
	Root        string
	Backend     config.Backend
	Runner      Runner
	LauncherJar string
	EventLimit  int
}

func New(opts Options) engine.Engine
```

Implementation requirements:

- validate client name, Minecraft version, and loader like memory engine.
- reject duplicate running names.
- start process with `Runner.Start`.
- write `LaunchCommand(req, opts.Backend)` to stdin.
- append `client.state/running` after process starts.
- start stdout/stderr readers and append parsed events/logs.
- write `connect`, chat, and `quit` commands to process stdin.
- `Wait` must use a cursor captured at wait start.
- `Stop(force=false)` writes `quit`, waits bounded by context or timeout, then
  marks stopped. `force=true` kills after writing `quit`.

- [ ] **Step 4: Run tests**

Run:

```sh
go test ./internal/headlessmc -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```sh
git add internal/headlessmc/engine.go internal/headlessmc/engine_test.go
git commit -m "feat: add headlessmc engine"
```

---

### Task 7: Daemon Control Methods

**Files:**
- Modify: `internal/daemon/server.go`
- Modify: `internal/daemon/server_test.go`

- [ ] **Step 1: Write failing daemon tests**

Add a test to `internal/daemon/server_test.go`:

```go
func TestServeClientControlMethods(t *testing.T) {
	input := strings.NewReader(
		`{"jsonrpc":"2.0","id":1,"method":"client.launch","params":{"name":"alice","minecraftVersion":"1.21.6","loader":"fabric","offline":true}}` + "\n" +
			`{"jsonrpc":"2.0","id":2,"method":"client.connect","params":{"name":"alice","server":"localhost:25565"}}` + "\n" +
			`{"jsonrpc":"2.0","id":3,"method":"client.chat","params":{"name":"alice","message":"Welcome alice"}}` + "\n" +
			`{"jsonrpc":"2.0","id":4,"method":"client.wait","params":{"client":"alice","chatPattern":"Welcome","timeout":1000000000}}` + "\n" +
			`{"jsonrpc":"2.0","id":5,"method":"client.logs","params":{"name":"alice"}}` + "\n" +
			`{"jsonrpc":"2.0","id":6,"method":"client.stop","params":{"name":"alice"}}` + "\n",
	)
	var output bytes.Buffer

	if err := daemon.Serve(context.Background(), engine.NewMemory(), input, &output); err != nil {
		t.Fatalf("Serve returned error: %v", err)
	}
	responses := decodeRPCResponses(t, output.String())
	if len(responses) != 6 {
		t.Fatalf("len(responses) = %d; output = %q", len(responses), output.String())
	}
	for _, response := range responses {
		if response.Error != nil {
			t.Fatalf("response has error: %#v", response)
		}
	}
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```sh
go test ./internal/daemon -count=1
```

Expected: fail with method not found.

- [ ] **Step 3: Implement methods**

Add cases in `handle`:

- `client.connect`: params `{name, server}` -> `eng.Connect`
- `client.chat`: params `{name, message}` -> `eng.Chat`
- `client.wait`: params matching `engine.WaitRequest` -> `eng.Wait`
- `client.logs`: params `{name}` -> `eng.Logs`
- `client.stop`: params `{name, force}` -> `eng.Stop`

Return compact result objects:

```go
resp.Result = map[string]any{"ok": true}
resp.Result = map[string]any{"logs": logs}
resp.Result = event
```

- [ ] **Step 4: Run tests**

Run:

```sh
go test ./internal/daemon -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```sh
git add internal/daemon/server.go internal/daemon/server_test.go
git commit -m "feat: add daemon client control methods"
```

---

### Task 8: Lazy Backend Selection For Long-Lived Commands

**Files:**
- Modify: `internal/cli/root.go`
- Modify: `internal/cli/scenario.go`
- Modify: `internal/cli/daemon.go`
- Modify: `internal/cli/scenario_test.go`
- Modify: `internal/cli/daemon_test.go`
- Modify: `cmd/mcw/main.go`

- [ ] **Step 1: Write failing CLI factory tests**

Add a test in `internal/cli/scenario_test.go` that passes `Dependencies.EngineFactory`
and asserts `scenario run` asks for an engine once. Add a similar test in
`internal/cli/daemon_test.go` for `daemon --stdio`.

Use this shape:

```go
factoryCalls := 0
root := cli.NewRoot(cli.Dependencies{
	EngineFactory: func(cli.EngineRequest) (engine.Engine, error) {
		factoryCalls++
		return engine.NewMemory(), nil
	},
	Stdout: &out,
	Stderr: &errOut,
	Version: "test",
})
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```sh
go test ./internal/cli -count=1
```

Expected: fail because `EngineFactory` does not exist.

- [ ] **Step 3: Add factory types**

In `internal/cli/root.go`:

```go
type EngineRequest struct {
	Config  config.Config
	WorkDir string
}

type EngineFactory func(EngineRequest) (engine.Engine, error)

type Dependencies struct {
	Engine        engine.Engine
	EngineFactory EngineFactory
	Stdin         io.Reader
	Stdout        io.Writer
	Stderr        io.Writer
	Version       string
}
```

Add helper:

```go
func engineForCommand(deps Dependencies, opts *GlobalOptions) (engine.Engine, error) {
	if deps.Engine != nil {
		return deps.Engine, nil
	}
	cfg, err := loadCLIConfig(opts)
	if err != nil {
		return nil, err
	}
	if deps.EngineFactory == nil {
		return engine.NewMemory(), nil
	}
	return deps.EngineFactory(EngineRequest{Config: cfg, WorkDir: opts.WorkDir})
}
```

Update `scenario run` and `daemon --stdio` to call `engineForCommand`.

- [ ] **Step 4: Wire main factory**

In `cmd/mcw/main.go`, construct an engine factory:

```go
EngineFactory: func(req cli.EngineRequest) (engine.Engine, error) {
	if req.Config.Backend.Type == "headlessmc" {
		return headlessmc.New(headlessmc.Options{
			Root:    req.WorkDir,
			Backend: req.Config.Backend,
			Runner:  headlessmc.OSRunner{},
		}), nil
	}
	return engine.NewMemory(), nil
},
```

Import `github.com/minekube/craftwright/internal/headlessmc`.

- [ ] **Step 5: Run tests**

Run:

```sh
go test ./internal/cli ./cmd/mcw -count=1
```

Expected: pass.

- [ ] **Step 6: Commit**

```sh
git add internal/cli/root.go internal/cli/scenario.go internal/cli/daemon.go internal/cli/scenario_test.go internal/cli/daemon_test.go cmd/mcw/main.go
git commit -m "feat: select backend for long-lived commands"
```

---

### Task 9: Scenario Real-Backend Coverage With Fake Process

**Files:**
- Modify: `internal/scenario/scenario_test.go`
- Modify: `internal/headlessmc/engine_test.go`

- [ ] **Step 1: Write a scenario test using the HeadlessMC fake process**

Add a test that runs a scenario through `scenario.Run` with the headlessmc engine
and fake runner:

```go
func TestScenarioRunWithHeadlessMCFakeEngine(t *testing.T) {
	runner := headlessmc.NewFakeRunner()
	eng := headlessmc.New(headlessmc.Options{
		Root:        t.TempDir(),
		Backend:     config.Default().Backend,
		Runner:      runner,
		LauncherJar: "/cache/headlessmc-launcher-wrapper-2.9.0.jar",
	})
	go func() {
		time.Sleep(10 * time.Millisecond)
		runner.EmitStdout("alice", `[12:00:00] [Render thread/INFO]: [CHAT] Welcome alice`)
	}()
	result, err := scenario.Run(context.Background(), eng, scenario.File{
		Version: 1,
		Clients: map[string]scenario.Client{
			"alice": {Minecraft: "1.21.6", Loader: "fabric", Offline: true},
		},
		Steps: []scenario.Step{
			{Launch: "alice"},
			{Connect: &scenario.ConnectStep{Client: "alice", Server: "localhost:25565"}},
			{Wait: &scenario.WaitStep{Client: "alice", Chat: "Welcome", Timeout: time.Second}},
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	if result.Steps != 3 {
		t.Fatalf("steps = %d", result.Steps)
	}
}
```

- [ ] **Step 2: Run tests**

Run:

```sh
go test ./internal/scenario ./internal/headlessmc -count=1
```

Expected: pass after any necessary exported scenario constructors are adjusted.

- [ ] **Step 3: Commit**

```sh
git add internal/scenario/scenario_test.go internal/headlessmc/engine_test.go
git commit -m "test: cover scenarios with headlessmc backend"
```

---

### Task 10: Opt-In Real Smoke Test And Docs

**Files:**
- Create: `internal/headlessmc/real_smoke_test.go`
- Modify: `README.md`

- [ ] **Step 1: Add skipped-by-default real smoke test**

Create `internal/headlessmc/real_smoke_test.go`:

```go
package headlessmc_test

import (
	"os"
	"testing"
)

func TestRealClientSmoke(t *testing.T) {
	if os.Getenv("CRAFTWRIGHT_REAL_CLIENT") != "1" {
		t.Skip("set CRAFTWRIGHT_REAL_CLIENT=1 to run the real Minecraft client smoke test")
	}
	t.Fatal("real client smoke is intentionally enabled only after cache download and local offline server harness are implemented")
}
```

Replace the intentional failure in the same task once a local offline server
harness exists. The final version must:

- prepare HeadlessMC metadata.
- launch the real client in offline Fabric mode.
- connect to a local offline-mode server.
- observe a known chat line.
- stop the client.
- leave artifacts in a temp directory.

- [ ] **Step 2: Document the current real-backend status**

Update `README.md` with:

```md
## Real Client Backend

The HeadlessMC/HMC-Specifics backend is designed for long-lived `mcw` processes:
`mcw scenario run` and `mcw daemon --stdio`. Standalone multi-invocation
`mcw client ...` persistence will use a daemon transport in a later slice.

Default tests use fake process supervision. The real client smoke test is opt-in:

```sh
CRAFTWRIGHT_REAL_CLIENT=1 go test ./internal/headlessmc -run TestRealClientSmoke -count=1
```
```

- [ ] **Step 3: Run tests**

Run:

```sh
go test ./... -count=1
go vet ./...
```

Expected: pass with real smoke skipped unless `CRAFTWRIGHT_REAL_CLIENT=1`.

- [ ] **Step 4: Commit**

```sh
git add internal/headlessmc/real_smoke_test.go README.md
git commit -m "docs: document real client smoke path"
```

---

## Final Verification

- [ ] Run default tests:

```sh
go test ./... -count=1
```

- [ ] Run race tests:

```sh
go test -race ./... -count=1
```

- [ ] Run vet:

```sh
go vet ./...
```

- [ ] Run opt-in real smoke only when Java/network/server prerequisites are ready:

```sh
CRAFTWRIGHT_REAL_CLIENT=1 go test ./internal/headlessmc -run TestRealClientSmoke -count=1 -v
```

- [ ] Push commits:

```sh
git push
```

## Self-Review

- Spec coverage: Covers backend config, cache metadata, command construction,
  process supervision, parser/event cursor, daemon methods, scenario execution,
  opt-in real smoke, and docs. Persistent standalone CLI is explicitly deferred
  because the design requires a daemon owner.
- Placeholder scan: No task uses vague `TBD` or "write tests for the above"
  language. The real smoke test is intentionally a gated finalization point and
  must be completed in Task 10 before that task is committed.
- Type consistency: `config.Backend`, `headlessmc.Options`,
  `headlessmc.PrepareRequest`, and daemon method params are named consistently
  across tasks.
