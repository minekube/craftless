package cli_test

import (
	"encoding/json"
	"strings"
	"testing"

	"github.com/minekube/craftwright/internal/cli"
)

func TestOutputModeRejectsConflictingMachineModes(t *testing.T) {
	cases := [][]string{
		{"--json", "--plain", "client"},
		{"--json", "--jsonl", "client"},
		{"--jsonl", "--plain", "client"},
	}

	for _, args := range cases {
		_, stderr, code := execute(args...)
		if code != 2 {
			t.Fatalf("args %v: code = %d, want 2; stderr = %s", args, code, stderr)
		}
		if !strings.Contains(stderr, "choose only one of --json, --jsonl, or --plain") {
			t.Fatalf("args %v: stderr = %q", args, stderr)
		}
	}
}

func TestFlagParseErrorsReturnUsageCode(t *testing.T) {
	cases := [][]string{
		{"--bogus"},
		{"-z"},
		{"--verbose=bad", "client"},
		{"--config"},
	}

	for _, args := range cases {
		_, stderr, code := execute(args...)
		if code != 2 {
			t.Fatalf("args %v: code = %d, want 2; stderr = %s", args, code, stderr)
		}
		if stderr == "" {
			t.Fatalf("args %v: stderr is empty", args)
		}
	}
}

func TestWriteJSONResult(t *testing.T) {
	var b strings.Builder
	err := cli.WriteJSON(&b, map[string]any{"ok": true, "name": "alice"})
	if err != nil {
		t.Fatal(err)
	}
	var got map[string]any
	if err := json.Unmarshal([]byte(b.String()), &got); err != nil {
		t.Fatalf("invalid json %q: %v", b.String(), err)
	}
	if got["ok"] != true || got["name"] != "alice" {
		t.Fatalf("got %#v", got)
	}
}
