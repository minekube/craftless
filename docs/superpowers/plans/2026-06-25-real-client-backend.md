# Real Client Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a HeadlessMC/HMC-Specifics backend that can launch and control a real offline-mode Fabric Minecraft Java client through the existing Craftwright engine interface.

**Architecture:** Keep `engine.Engine` as the boundary. Add `internal/hmc` for HeadlessMC config rendering, launch command construction, process supervision, bridge command I/O, log parsing, and artifact collection. Wire the backend into CLI dependencies through explicit config/env selection while preserving the in-memory backend for fast tests.

**Tech Stack:** Go 1.22, Cobra, standard `os/exec` process handling, YAML config, JSON/JSONL artifacts, HeadlessMC 2.9.x external CLI, HMC-Specifics 2.4.x bridge commands, fake process scripts for normal CI.

---

## Files And Responsibilities

- `internal/config/config.go`: add backend/cache knobs while preserving default config compatibility.
- `internal/hmc/options.go`: typed HMC backend options and validation.
- `internal/hmc/config.go`: render isolated `HeadlessMC/config.properties`.
- `internal/hmc/command.go`: build HeadlessMC launch and prepare commands.
- `internal/hmc/parser.go`: parse HeadlessMC/HMC-Specifics output into normalized events.
- `internal/hmc/process.go`: small process runner abstraction for real `exec.Cmd` and fake tests.
- `internal/hmc/engine.go`: implement `engine.Engine` with client lifecycle and bridge commands.
- `internal/hmc/artifacts.go`: write launch metadata, logs, events, and copy crash/log directories.
- `internal/cli/cache.go`: route `mcw cache prepare` to real backend preparation when selected.
- `internal/cli/root.go`: select memory or HMC engine from config/env.
- `docs/real-client-backend.md`: user-facing install, supported matrix, CI, troubleshooting, and opt-in smoke instructions.

---

## Task 1: Backend Config Surface

**Files:**
- Modify: `internal/config/config.go`
- Modify: `internal/config/config_test.go`
- Modify: `internal/project/project.go`

- [ ] **Step 1: Add failing config tests**

Add tests to `internal/config/config_test.go`:

```go
func TestLoadBackendConfig(t *testing.T) {
	path := writeConfig(t, `version: 1
backend:
  type: hmc
  hmc:
    launcher: /opt/headlessmc/headlessmc-launcher-wrapper.jar
    java: /usr/bin/java
    minecraft: .craftwright/cache/minecraft
    headless: lwjgl
    autoDownloadJava: false
    jline: false
defaults:
  minecraft: "1.21.6"
  loader: fabric
  offline: true
paths:
  artifacts: .craftwright/artifacts
  cache: .craftwright/cache
`)
	cfg, err := Load(path)
	if err != nil {
		t.Fatal(err)
	}
	if cfg.Backend.Type != "hmc" {
		t.Fatalf("backend type = %q", cfg.Backend.Type)
	}
	if cfg.Backend.HMC.Launcher != "/opt/headlessmc/headlessmc-launcher-wrapper.jar" {
		t.Fatalf("launcher = %q", cfg.Backend.HMC.Launcher)
	}
	if cfg.Backend.HMC.Headless != "lwjgl" || cfg.Backend.HMC.JLine {
		t.Fatalf("hmc = %#v", cfg.Backend.HMC)
	}
}

func TestDefaultBackendIsMemory(t *testing.T) {
	cfg := Default()
	if cfg.Backend.Type != "memory" {
		t.Fatalf("backend type = %q", cfg.Backend.Type)
	}
	if cfg.Backend.HMC.Headless != "lwjgl" {
		t.Fatalf("headless = %q", cfg.Backend.HMC.Headless)
	}
}
```

- [ ] **Step 2: Verify red**

Run:

```bash
go test ./internal/config -run 'TestLoadBackendConfig|TestDefaultBackendIsMemory' -count=1
```

Expected: fail because `Config` has no `Backend` field.

- [ ] **Step 3: Add backend config types**

Add to `internal/config/config.go`:

```go
type Config struct {
	Version  int      `yaml:"version"`
	Defaults Defaults `yaml:"defaults"`
	Paths    Paths    `yaml:"paths"`
	Backend  Backend  `yaml:"backend"`
}

type Backend struct {
	Type string    `yaml:"type"`
	HMC  HMCConfig `yaml:"hmc"`
}

type HMCConfig struct {
	Launcher         string `yaml:"launcher"`
	Java             string `yaml:"java"`
	Minecraft        string `yaml:"minecraft"`
	Headless         string `yaml:"headless"`
	AutoDownloadJava bool   `yaml:"autoDownloadJava"`
	JLine            bool   `yaml:"jline"`
}
```

Update `Default()` to include:

```go
Backend: Backend{
	Type: "memory",
	HMC: HMCConfig{
		Minecraft:        ".craftwright/cache/minecraft",
		Headless:         "lwjgl",
		AutoDownloadJava: true,
		JLine:            false,
	},
},
```

Update `internal/project/project.go` `defaultConfig` to include:

```yaml
backend:
  type: memory
  hmc:
    launcher: ""
    java: ""
    minecraft: .craftwright/cache/minecraft
    headless: lwjgl
    autoDownloadJava: true
    jline: false
```

- [ ] **Step 4: Verify green**

Run:

```bash
go test ./internal/config ./internal/project -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add internal/config/config.go internal/config/config_test.go internal/project/project.go
git commit -m "feat: add backend config surface"
```

---

## Task 2: HMC Options, Config Rendering, And Command Builder

**Files:**
- Create: `internal/hmc/options.go`
- Create: `internal/hmc/config.go`
- Create: `internal/hmc/command.go`
- Create: `internal/hmc/config_test.go`
- Create: `internal/hmc/command_test.go`

- [ ] **Step 1: Add failing renderer and command tests**

Create `internal/hmc/config_test.go`:

```go
package hmc

import (
	"strings"
	"testing"
)

func TestRenderConfigProperties(t *testing.T) {
	opts := Options{
		Java:             "/usr/bin/java",
		GameDir:          "/tmp/game",
		MinecraftDir:     "/tmp/mc",
		Offline:          true,
		Username:         "alice",
		Headless:         "lwjgl",
		AutoDownloadJava: false,
		JLine:            false,
	}
	props := RenderConfigProperties(opts)
	for _, want := range []string{
		"hmc.gamedir=/tmp/game",
		"hmc.mcdir=/tmp/mc",
		"hmc.offline=true",
		"hmc.offline.username=alice",
		"hmc.java.versions=/usr/bin/java",
		"hmc.auto.download.java=false",
		"hmc.jline.enabled=false",
		"hmc.rethrow.launch.exceptions=true",
		"hmc.exit.on.failed.command=true",
	} {
		if !strings.Contains(props, want+"\n") {
			t.Fatalf("properties missing %q in:\n%s", want, props)
		}
	}
}

func TestOptionsValidateRequiresLauncherForRealProcess(t *testing.T) {
	err := Options{Java: "java", MinecraftVersion: "1.21.6", Loader: "fabric"}.Validate()
	if err == nil || !strings.Contains(err.Error(), "launcher is required") {
		t.Fatalf("err = %v", err)
	}
}
```

Create `internal/hmc/command_test.go`:

```go
package hmc

import (
	"reflect"
	"testing"
)

func TestBuildLaunchCommand(t *testing.T) {
	opts := Options{
		Java:             "/usr/bin/java",
		Launcher:         "/opt/hmc/headlessmc-launcher-wrapper.jar",
		MinecraftVersion: "1.21.6",
		Loader:           "fabric",
		Offline:          true,
		Headless:         "lwjgl",
		Server:           "127.0.0.1:25565",
	}
	cmd := BuildLaunchCommand(opts)
	want := []string{
		"/usr/bin/java",
		"-jar", "/opt/hmc/headlessmc-launcher-wrapper.jar",
		"--command",
		"launch fabric:1.21.6 -offline -specifics -lwjgl --jvm \"-Djava.awt.headless=true\" --game-args \"--quickPlayMultiplayer 127.0.0.1:25565\"",
	}
	if !reflect.DeepEqual(cmd, want) {
		t.Fatalf("cmd = %#v", cmd)
	}
}
```

- [ ] **Step 2: Verify red**

Run:

```bash
go test ./internal/hmc -run 'TestRenderConfigProperties|TestBuildLaunchCommand|TestOptionsValidate' -count=1
```

Expected: fail because `internal/hmc` does not exist.

- [ ] **Step 3: Implement minimal package**

Create `internal/hmc/options.go`:

```go
package hmc

import "fmt"

type Options struct {
	Launcher         string
	Java             string
	GameDir          string
	MinecraftDir     string
	MinecraftVersion string
	Loader           string
	Offline          bool
	Username         string
	Headless         string
	Server           string
	AutoDownloadJava bool
	JLine            bool
}

func (o Options) Validate() error {
	if o.Launcher == "" {
		return fmt.Errorf("launcher is required")
	}
	if o.Java == "" {
		return fmt.Errorf("java is required")
	}
	if o.MinecraftVersion == "" {
		return fmt.Errorf("minecraft version is required")
	}
	if o.Loader == "" {
		return fmt.Errorf("loader is required")
	}
	if o.Headless != "" && o.Headless != "lwjgl" && o.Headless != "xvfb" {
		return fmt.Errorf("unsupported headless mode %q", o.Headless)
	}
	return nil
}
```

Create `internal/hmc/config.go`:

```go
package hmc

import (
	"fmt"
	"strings"
)

func RenderConfigProperties(o Options) string {
	var b strings.Builder
	writeProp(&b, "hmc.gamedir", o.GameDir)
	writeProp(&b, "hmc.mcdir", o.MinecraftDir)
	writeProp(&b, "hmc.offline", fmt.Sprintf("%t", o.Offline))
	if o.Username != "" {
		writeProp(&b, "hmc.offline.username", o.Username)
	}
	if o.Java != "" {
		writeProp(&b, "hmc.java.versions", o.Java)
	}
	writeProp(&b, "hmc.auto.download.java", fmt.Sprintf("%t", o.AutoDownloadJava))
	writeProp(&b, "hmc.jline.enabled", fmt.Sprintf("%t", o.JLine))
	writeProp(&b, "hmc.rethrow.launch.exceptions", "true")
	writeProp(&b, "hmc.exit.on.failed.command", "true")
	writeProp(&b, "hmc.crash.report.watcher", "true")
	return b.String()
}

func writeProp(b *strings.Builder, key string, value string) {
	if value == "" {
		return
	}
	b.WriteString(key)
	b.WriteByte('=')
	b.WriteString(value)
	b.WriteByte('\n')
}
```

Create `internal/hmc/command.go`:

```go
package hmc

import "fmt"

func BuildLaunchCommand(o Options) []string {
	java := o.Java
	if java == "" {
		java = "java"
	}
	loaderVersion := o.MinecraftVersion
	if o.Loader != "" && o.Loader != "vanilla" {
		loaderVersion = o.Loader + ":" + o.MinecraftVersion
	}
	launch := "launch " + loaderVersion
	if o.Offline {
		launch += " -offline"
	}
	if o.Loader != "" && o.Loader != "vanilla" {
		launch += " -specifics"
	}
	if o.Headless == "" || o.Headless == "lwjgl" {
		launch += " -lwjgl"
	}
	launch += " --jvm \"-Djava.awt.headless=true\""
	if o.Server != "" {
		launch += fmt.Sprintf(" --game-args \"--quickPlayMultiplayer %s\"", o.Server)
	}
	return []string{java, "-jar", o.Launcher, "--command", launch}
}
```

- [ ] **Step 4: Verify green**

Run:

```bash
go test ./internal/hmc -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add internal/hmc
git commit -m "feat: add hmc command builder"
```

---

## Task 3: Log Parser And Event Normalization

**Files:**
- Create: `internal/hmc/parser.go`
- Create: `internal/hmc/parser_test.go`

- [ ] **Step 1: Add failing parser tests**

Create `internal/hmc/parser_test.go`:

```go
package hmc

import (
	"testing"

	"github.com/minekube/craftwright/internal/engine"
)

func TestParseBridgeReady(t *testing.T) {
	event, ok := ParseLine("alice", "Loading HMC-Specifics!")
	if !ok {
		t.Fatal("expected event")
	}
	if event.Type != engine.EventState || event.State != engine.StateRunning {
		t.Fatalf("event = %#v", event)
	}
}

func TestParseChatLine(t *testing.T) {
	event, ok := ParseLine("alice", "[CHAT] Welcome alice")
	if !ok {
		t.Fatal("expected event")
	}
	if event.Type != engine.EventChat || event.Message != "Welcome alice" {
		t.Fatalf("event = %#v", event)
	}
}

func TestParseRenderLine(t *testing.T) {
	event, ok := ParseLine("alice", "{x=4.0, y=8.0, text=Play Multiplayer}")
	if !ok {
		t.Fatal("expected event")
	}
	if event.Type != EventRenderedText || event.Message != "Play Multiplayer" {
		t.Fatalf("event = %#v", event)
	}
}
```

- [ ] **Step 2: Verify red**

Run:

```bash
go test ./internal/hmc -run TestParse -count=1
```

Expected: fail because parser functions and rendered event type are missing.

- [ ] **Step 3: Add rendered event type and parser**

Add to `internal/engine/engine.go`:

```go
EventRendered EventType = "client.rendered"
```

Create `internal/hmc/parser.go`:

```go
package hmc

import (
	"strings"

	"github.com/minekube/craftwright/internal/engine"
)

const EventRenderedText engine.EventType = engine.EventRendered

func ParseLine(client string, line string) (engine.Event, bool) {
	if strings.Contains(line, "Loading HMC-Specifics!") || strings.Contains(line, "HMC-Specifics initialized!") {
		return engine.Event{Type: engine.EventState, Client: client, State: engine.StateRunning, Message: "bridge-ready"}, true
	}
	if strings.Contains(line, "[CHAT]") {
		_, msg, _ := strings.Cut(line, "[CHAT]")
		return engine.Event{Type: engine.EventChat, Client: client, Message: strings.TrimSpace(msg)}, true
	}
	if strings.HasPrefix(line, "{x=") && strings.Contains(line, "text=") {
		_, msg, _ := strings.Cut(line, "text=")
		msg = strings.TrimSuffix(strings.TrimSpace(msg), "}")
		return engine.Event{Type: engine.EventRendered, Client: client, Message: msg}, true
	}
	return engine.Event{}, false
}
```

- [ ] **Step 4: Verify green**

Run:

```bash
go test ./internal/engine ./internal/hmc -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add internal/engine/engine.go internal/hmc/parser.go internal/hmc/parser_test.go
git commit -m "feat: parse hmc client events"
```

---

## Task 4: Process Runner And Fake HeadlessMC Harness

**Files:**
- Create: `internal/hmc/process.go`
- Create: `internal/hmc/process_test.go`

- [ ] **Step 1: Add failing fake process test**

Create `internal/hmc/process_test.go`:

```go
package hmc

import (
	"context"
	"io"
	"strings"
	"testing"
	"time"
)

func TestFakeRunnerExposesStdoutAndStdin(t *testing.T) {
	runner := NewFakeRunner([]string{"Loading HMC-Specifics!\n"})
	proc, err := runner.Start(context.Background(), []string{"java", "-jar", "hmc.jar"})
	if err != nil {
		t.Fatal(err)
	}
	data, err := io.ReadAll(proc.Stdout())
	if err != nil {
		t.Fatal(err)
	}
	if string(data) != "Loading HMC-Specifics!\n" {
		t.Fatalf("stdout = %q", data)
	}
	if _, err := proc.Stdin().Write([]byte("quit\n")); err != nil {
		t.Fatal(err)
	}
	if got := strings.Join(runner.Commands(), ""); got != "quit\n" {
		t.Fatalf("commands = %q", got)
	}
}

func TestWaitReadyTimesOut(t *testing.T) {
	runner := NewFakeRunner(nil)
	proc, err := runner.Start(context.Background(), []string{"java"})
	if err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Millisecond)
	defer cancel()
	_, err = WaitForBridge(ctx, "alice", proc.Stdout())
	if err == nil {
		t.Fatal("expected timeout")
	}
}
```

- [ ] **Step 2: Verify red**

Run:

```bash
go test ./internal/hmc -run 'TestFakeRunner|TestWaitReady' -count=1
```

Expected: fail because process runner is missing.

- [ ] **Step 3: Implement runner interfaces and readiness wait**

Create `internal/hmc/process.go`:

```go
package hmc

import (
	"bufio"
	"bytes"
	"context"
	"io"
	"os/exec"
	"sync"

	"github.com/minekube/craftwright/internal/engine"
)

type Runner interface {
	Start(context.Context, []string) (Process, error)
}

type Process interface {
	Stdin() io.Writer
	Stdout() io.Reader
	Stderr() io.Reader
	Kill() error
	Wait() error
}

type ExecRunner struct{}

func (ExecRunner) Start(ctx context.Context, args []string) (Process, error) {
	cmd := exec.CommandContext(ctx, args[0], args[1:]...)
	stdin, err := cmd.StdinPipe()
	if err != nil {
		return nil, err
	}
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		return nil, err
	}
	stderr, err := cmd.StderrPipe()
	if err != nil {
		return nil, err
	}
	if err := cmd.Start(); err != nil {
		return nil, err
	}
	return &execProcess{cmd: cmd, stdin: stdin, stdout: stdout, stderr: stderr}, nil
}

type execProcess struct {
	cmd    *exec.Cmd
	stdin  io.Writer
	stdout io.Reader
	stderr io.Reader
}

func (p *execProcess) Stdin() io.Writer  { return p.stdin }
func (p *execProcess) Stdout() io.Reader { return p.stdout }
func (p *execProcess) Stderr() io.Reader { return p.stderr }
func (p *execProcess) Kill() error       { return p.cmd.Process.Kill() }
func (p *execProcess) Wait() error       { return p.cmd.Wait() }

type FakeRunner struct {
	stdout   []string
	mu       sync.Mutex
	commands []string
}

func NewFakeRunner(stdout []string) *FakeRunner {
	return &FakeRunner{stdout: stdout}
}

func (r *FakeRunner) Start(context.Context, []string) (Process, error) {
	stdin := &recordingWriter{onWrite: func(s string) {
		r.mu.Lock()
		defer r.mu.Unlock()
		r.commands = append(r.commands, s)
	}}
	return fakeProcess{stdin: stdin, stdout: bytes.NewBufferString(stringsJoin(r.stdout)), stderr: bytes.NewBuffer(nil)}, nil
}

func (r *FakeRunner) Commands() []string {
	r.mu.Lock()
	defer r.mu.Unlock()
	return append([]string(nil), r.commands...)
}

type recordingWriter struct {
	onWrite func(string)
}

func (w *recordingWriter) Write(p []byte) (int, error) {
	w.onWrite(string(p))
	return len(p), nil
}

type fakeProcess struct {
	stdin  io.Writer
	stdout io.Reader
	stderr io.Reader
}

func (p fakeProcess) Stdin() io.Writer  { return p.stdin }
func (p fakeProcess) Stdout() io.Reader { return p.stdout }
func (p fakeProcess) Stderr() io.Reader { return p.stderr }
func (p fakeProcess) Kill() error       { return nil }
func (p fakeProcess) Wait() error       { return nil }

func WaitForBridge(ctx context.Context, client string, stdout io.Reader) (engine.Event, error) {
	lines := make(chan string)
	go func() {
		scanner := bufio.NewScanner(stdout)
		for scanner.Scan() {
			lines <- scanner.Text()
		}
		close(lines)
	}()
	for {
		select {
		case <-ctx.Done():
			return engine.Event{}, ctx.Err()
		case line, ok := <-lines:
			if !ok {
				return engine.Event{}, io.EOF
			}
			if event, parsed := ParseLine(client, line); parsed && event.Message == "bridge-ready" {
				return event, nil
			}
		}
	}
}

func stringsJoin(parts []string) string {
	var b bytes.Buffer
	for _, part := range parts {
		b.WriteString(part)
	}
	return b.String()
}
```

- [ ] **Step 4: Verify green**

Run:

```bash
go test ./internal/hmc -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add internal/hmc/process.go internal/hmc/process_test.go
git commit -m "feat: add hmc process runner"
```

---

## Task 5: HMC Engine Launch, Logs, Status, And Stop

**Files:**
- Create: `internal/hmc/engine.go`
- Create: `internal/hmc/engine_test.go`
- Create: `internal/hmc/artifacts.go`

- [ ] **Step 1: Add failing engine lifecycle test**

Create `internal/hmc/engine_test.go`:

```go
package hmc

import (
	"context"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/minekube/craftwright/internal/engine"
)

func TestEngineLaunchWritesConfigAndRecordsClient(t *testing.T) {
	dir := t.TempDir()
	runner := NewFakeRunner([]string{"Loading HMC-Specifics!\n"})
	eng := NewEngine(EngineOptions{
		Runner:       runner,
		Launcher:     "/opt/hmc/headlessmc-launcher-wrapper.jar",
		Java:         "java",
		CacheDir:     filepath.Join(dir, "cache"),
		ArtifactsDir: filepath.Join(dir, "artifacts"),
		Headless:     "lwjgl",
	})
	client, err := eng.Launch(context.Background(), engine.LaunchRequest{
		Name:             "alice",
		MinecraftVersion: "1.21.6",
		Loader:           "fabric",
		Offline:          true,
		Username:         "Alice",
		Timeout:          time.Second,
	})
	if err != nil {
		t.Fatal(err)
	}
	if client.Name != "alice" || client.State != engine.StateRunning || !client.Offline {
		t.Fatalf("client = %#v", client)
	}
	if _, err := os.Stat(filepath.Join(dir, "artifacts", "alice", "hmc-config.properties")); err != nil {
		t.Fatal(err)
	}
	logs, err := eng.Logs(context.Background(), "alice")
	if err != nil {
		t.Fatal(err)
	}
	if len(logs) != 1 || logs[0] != "Loading HMC-Specifics!" {
		t.Fatalf("logs = %#v", logs)
	}
	if err := eng.Stop(context.Background(), "alice", false); err != nil {
		t.Fatal(err)
	}
	if got := runner.Commands(); len(got) != 1 || got[0] != "quit\n" {
		t.Fatalf("commands = %#v", got)
	}
}
```

- [ ] **Step 2: Verify red**

Run:

```bash
go test ./internal/hmc -run TestEngineLaunchWritesConfigAndRecordsClient -count=1
```

Expected: fail because HMC engine does not exist.

- [ ] **Step 3: Implement minimal lifecycle**

Create `internal/hmc/engine.go` with a mutex-protected client map. `Launch`
must create artifact/config directories, render config, start the process, wait
for bridge readiness with `WaitForBridge`, store client/process/logs, and return
`engine.Client`. `Status`, `List`, `Logs`, and `Stop` must operate on that map.
Use exact behavior:

```go
type EngineOptions struct {
	Runner       Runner
	Launcher     string
	Java         string
	CacheDir     string
	ArtifactsDir string
	Headless     string
}
```

Default `Runner` to `ExecRunner{}`, `ArtifactsDir` to `.craftwright/artifacts`,
`CacheDir` to `.craftwright/cache`, and `Headless` to `lwjgl`.

Create `internal/hmc/artifacts.go` with:

```go
func writeFile(path string, data string) error {
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return err
	}
	return os.WriteFile(path, []byte(data), 0o644)
}
```

When storing logs, include the bridge-ready line read during launch.

- [ ] **Step 4: Verify green**

Run:

```bash
go test ./internal/hmc -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add internal/hmc/engine.go internal/hmc/engine_test.go internal/hmc/artifacts.go
git commit -m "feat: launch hmc clients"
```

---

## Task 6: Bridge Commands For Connect, Chat, And Wait

**Files:**
- Modify: `internal/hmc/engine.go`
- Modify: `internal/hmc/engine_test.go`
- Modify: `internal/daemon/server.go`

- [ ] **Step 1: Add failing bridge command tests**

Add to `internal/hmc/engine_test.go`:

```go
func TestEngineBridgeCommands(t *testing.T) {
	runner := NewFakeRunner([]string{"Loading HMC-Specifics!\n", "[CHAT] Welcome alice\n"})
	eng := NewEngine(EngineOptions{Runner: runner, Launcher: "hmc.jar", Java: "java", ArtifactsDir: t.TempDir()})
	if _, err := eng.Launch(context.Background(), engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true, Timeout: time.Second}); err != nil {
		t.Fatal(err)
	}
	if err := eng.Connect(context.Background(), "alice", "localhost:25565"); err != nil {
		t.Fatal(err)
	}
	if err := eng.Chat(context.Background(), "alice", "hello there"); err != nil {
		t.Fatal(err)
	}
	event, err := eng.Wait(context.Background(), engine.WaitRequest{Client: "alice", ChatPattern: "Welcome", Timeout: time.Second})
	if err != nil {
		t.Fatal(err)
	}
	if event.Type != engine.EventChat || event.Message != "Welcome alice" {
		t.Fatalf("event = %#v", event)
	}
	want := []string{"connect localhost 25565\n", "msg hello there\n"}
	got := runner.Commands()
	if len(got) < 2 || got[0] != want[0] || got[1] != want[1] {
		t.Fatalf("commands = %#v", got)
	}
}
```

- [ ] **Step 2: Verify red**

Run:

```bash
go test ./internal/hmc -run TestEngineBridgeCommands -count=1
```

Expected: fail because bridge methods are missing or still no-op.

- [ ] **Step 3: Implement bridge commands**

Implement exact command mapping in `internal/hmc/engine.go`:

- `Connect(ctx, "alice", "localhost:25565")` writes `connect localhost 25565\n`.
- `Chat(ctx, "alice", "hello")` writes `msg hello\n`.
- `Wait` scans retained events/logs first, then blocks until timeout for future parser events. For this task, retained events are enough because the fake runner emits the chat line during launch.

If a client does not exist, return `fmt.Errorf("client %s not found", name)`.

- [ ] **Step 4: Verify green**

Run:

```bash
go test ./internal/hmc ./internal/daemon -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add internal/hmc/engine.go internal/hmc/engine_test.go internal/daemon/server.go
git commit -m "feat: control hmc clients"
```

---

## Task 7: CLI Backend Selection And Cache Prepare Routing

**Files:**
- Modify: `internal/cli/root.go`
- Modify: `internal/cli/cache.go`
- Create: `internal/cli/backend_test.go`
- Modify: `internal/cli/init_cache_test.go`

- [ ] **Step 1: Add failing CLI backend tests**

Create `internal/cli/backend_test.go`:

```go
package cli_test

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestBackendConfigRejectsMissingHMCLauncher(t *testing.T) {
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "craftwright.yaml"), []byte(`version: 1
backend:
  type: hmc
defaults:
  minecraft: "1.21.6"
  loader: fabric
  offline: true
paths:
  artifacts: .craftwright/artifacts
  cache: .craftwright/cache
`), 0o644); err != nil {
		t.Fatal(err)
	}
	stdout, stderr, code := execute("--work-dir", dir, "client", "launch", "alice")
	if code != 2 {
		t.Fatalf("code = %d stdout = %s stderr = %s", code, stdout, stderr)
	}
	if !strings.Contains(stderr, "backend.hmc.launcher is required") {
		t.Fatalf("stderr = %q", stderr)
	}
}
```

- [ ] **Step 2: Verify red**

Run:

```bash
go test ./internal/cli -run TestBackendConfigRejectsMissingHMCLauncher -count=1
```

Expected: fail because CLI dependencies always use memory engine.

- [ ] **Step 3: Implement backend selection**

In `internal/cli/root.go`, load config before dependency construction when the
caller did not inject an engine. If `backend.type` is `memory`, keep current
behavior. If `backend.type` is `hmc`, create `hmc.NewEngine` from config and
project paths. If launcher is missing, return usage error
`backend.hmc.launcher is required`.

Preserve tests that inject `deps.Engine`; injected dependencies must win so unit
tests remain fast.

In `internal/cli/cache.go`, for HMC backend, `cache prepare` should validate the
requested matrix and write a deterministic manifest under
`.craftwright/cache/<profile>/<mc>-<loader>.json`. Full release download and
checksum enforcement can be the next implementation slice after the fake-process
backend passes. The manifest must include `"backend":"hmc"`.

- [ ] **Step 4: Verify green**

Run:

```bash
go test ./internal/cli ./internal/config ./internal/project -count=1
```

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add internal/cli/root.go internal/cli/cache.go internal/cli/backend_test.go internal/cli/init_cache_test.go
git commit -m "feat: select hmc backend from config"
```

---

## Task 8: Opt-In Real Client Smoke And Documentation

**Files:**
- Create: `internal/hmc/smoke_test.go`
- Create: `docs/real-client-backend.md`
- Modify: `README.md`

- [ ] **Step 1: Add opt-in smoke test**

Create `internal/hmc/smoke_test.go`:

```go
package hmc

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/minekube/craftwright/internal/engine"
)

func TestRealClientSmoke(t *testing.T) {
	launcher := os.Getenv("MCW_HMC_LAUNCHER")
	java := os.Getenv("MCW_JAVA")
	if launcher == "" || java == "" || os.Getenv("MCW_REAL_CLIENT_SMOKE") != "1" {
		t.Skip("set MCW_REAL_CLIENT_SMOKE=1 MCW_HMC_LAUNCHER and MCW_JAVA to run")
	}
	dir := t.TempDir()
	eng := NewEngine(EngineOptions{
		Launcher:     launcher,
		Java:         java,
		CacheDir:     dir + "/cache",
		ArtifactsDir: dir + "/artifacts",
		Headless:     "lwjgl",
	})
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Minute)
	defer cancel()
	client, err := eng.Launch(ctx, engine.LaunchRequest{
		Name:             "smoke",
		MinecraftVersion: getenv("MCW_MC_VERSION", "1.21.6"),
		Loader:           "fabric",
		Offline:          true,
		Username:         "Smoke",
		Timeout:          5 * time.Minute,
	})
	if err != nil {
		t.Fatal(err)
	}
	if client.State != engine.StateRunning {
		t.Fatalf("client = %#v", client)
	}
	if err := eng.Stop(context.Background(), "smoke", true); err != nil {
		t.Fatal(err)
	}
}

func getenv(key string, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
```

- [ ] **Step 2: Verify normal CI skips smoke**

Run:

```bash
go test ./internal/hmc -run TestRealClientSmoke -count=1
```

Expected: pass with skip message unless smoke env vars are set.

- [ ] **Step 3: Document real-client backend**

Create `docs/real-client-backend.md` with these sections:

```markdown
# Real Client Backend

Craftwright can run a real Minecraft Java client through HeadlessMC and HMC-Specifics.

## Supported First Matrix

- Minecraft: `1.21.6`
- Loader: Fabric
- Mode: offline headless CI
- Backend: HeadlessMC launcher wrapper plus HMC-Specifics

## Install

Download HeadlessMC 2.9.x `headlessmc-launcher-wrapper.jar` from `headlesshq/headlessmc` releases and make Java available.

## Config

```yaml
version: 1
backend:
  type: hmc
  hmc:
    launcher: /absolute/path/headlessmc-launcher-wrapper-2.9.0.jar
    java: /absolute/path/java
    minecraft: .craftwright/cache/minecraft
    headless: lwjgl
    autoDownloadJava: true
    jline: false
defaults:
  minecraft: "1.21.6"
  loader: fabric
  offline: true
```

## Commands

```sh
mcw cache prepare --mc 1.21.6 --loader fabric --no-input
mcw client launch alice --offline
mcw client connect alice localhost:25565
mcw client chat alice "hello"
mcw client wait alice --chat /Welcome/ --timeout 30s
mcw client stop alice
```

## Opt-In Smoke Test

```sh
MCW_REAL_CLIENT_SMOKE=1 \
MCW_HMC_LAUNCHER=/path/headlessmc-launcher-wrapper-2.9.0.jar \
MCW_JAVA=/path/java \
go test ./internal/hmc -run TestRealClientSmoke -count=1 -v
```

## Troubleshooting

Use `mcw client logs NAME`, inspect `.craftwright/artifacts/NAME`, and check `stdout.log`, `stderr.log`, `events.jsonl`, and crash reports.
```

Update `README.md` Status with a short line pointing to `docs/real-client-backend.md`.

- [ ] **Step 4: Verify full suite**

Run:

```bash
go test ./... -count=1
go vet ./...
git diff --check
```

Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add internal/hmc/smoke_test.go docs/real-client-backend.md README.md
git commit -m "docs: describe real client backend"
```

---

## Self-Review Checklist

- Spec coverage: tasks cover backend config, HMC launch/config command construction, parser, fake-process CI harness, engine lifecycle, bridge commands, CLI backend selection, cache routing, opt-in real smoke, and docs.
- TDD: each implementation task starts with failing tests and a red command.
- Scope control: SDK, Playwright, Baritone, online auth, GUI selectors, and browser control are intentionally deferred until a real client works behind the daemon.
- License control: HeadlessMC/HMC-Specifics are MIT; PrismLauncher remains source-reading evidence only; GPL/LGPL/ambiguous prior art is not copied into Craftwright.
- Verification: normal CI stays fake-process only; real-client smoke is opt-in through explicit env vars.
