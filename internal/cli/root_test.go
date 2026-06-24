package cli_test

import (
	"bytes"
	"testing"

	"github.com/minekube/craftwright/internal/cli"
	"github.com/minekube/craftwright/internal/engine"
)

func execute(args ...string) (stdout string, stderr string, code int) {
	var out bytes.Buffer
	var err bytes.Buffer
	root := cli.NewRoot(cli.Dependencies{
		Engine:  engine.NewMemory(),
		Stdout:  &out,
		Stderr:  &err,
		Version: "test",
	})
	root.SetArgs(args)
	code = cli.Execute(root)
	return out.String(), err.String(), code
}

func TestRootHelpShowsCommonCommands(t *testing.T) {
	stdout, stderr, code := execute("--help")
	if code != 0 {
		t.Fatalf("code = %d, want 0; stderr = %s", code, stderr)
	}
	if !bytes.Contains([]byte(stdout), []byte("mcw automates real Minecraft Java clients")) {
		t.Fatalf("help missing one-liner:\n%s", stdout)
	}
	if !bytes.Contains([]byte(stdout), []byte("client")) {
		t.Fatalf("help missing client command:\n%s", stdout)
	}
	if !bytes.Contains([]byte(stdout), []byte("scenario")) {
		t.Fatalf("help missing scenario command:\n%s", stdout)
	}
}

func TestVersionPrintsToStdout(t *testing.T) {
	stdout, stderr, code := execute("--version")
	if code != 0 {
		t.Fatalf("code = %d, want 0; stderr = %s", code, stderr)
	}
	if stdout != "mcw test\n" {
		t.Fatalf("stdout = %q", stdout)
	}
	if stderr != "" {
		t.Fatalf("stderr = %q, want empty", stderr)
	}
}

func TestUnknownCommandReturnsUsageError(t *testing.T) {
	_, stderr, code := execute("missing")
	if code != 2 {
		t.Fatalf("code = %d, want 2; stderr = %s", code, stderr)
	}
	if !bytes.Contains([]byte(stderr), []byte("unknown command")) {
		t.Fatalf("stderr missing unknown command message:\n%s", stderr)
	}
}
