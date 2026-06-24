package cli

import (
	"fmt"
	"io"

	"github.com/minekube/craftwright/internal/engine"
	"github.com/spf13/cobra"
)

type Dependencies struct {
	Engine  engine.Engine
	Stdout  io.Writer
	Stderr  io.Writer
	Version string
}

type GlobalOptions struct {
	JSON    bool
	JSONL   bool
	Plain   bool
	Quiet   bool
	Verbose int
	Debug   bool
	NoInput bool
	NoColor bool
	Config  string
	WorkDir string
}

func NewRoot(deps Dependencies) *cobra.Command {
	opts := GlobalOptions{}
	root := &cobra.Command{
		Use:           "mcw",
		Short:         "mcw automates real Minecraft Java clients for tests, agents, and CI.",
		Version:       deps.Version,
		SilenceUsage:  true,
		SilenceErrors: true,
	}

	root.SetOut(deps.Stdout)
	root.SetErr(deps.Stderr)
	root.SetVersionTemplate("{{.Name}} {{.Version}}\n")

	flags := root.PersistentFlags()
	flags.BoolVar(&opts.JSON, "json", false, "write JSON output")
	flags.BoolVar(&opts.JSONL, "jsonl", false, "write JSON Lines output")
	flags.BoolVar(&opts.Plain, "plain", false, "write plain text output")
	flags.BoolVarP(&opts.Quiet, "quiet", "q", false, "suppress non-essential output")
	flags.CountVarP(&opts.Verbose, "verbose", "v", "increase diagnostic output")
	flags.BoolVar(&opts.Debug, "debug", false, "write debug output")
	flags.BoolVar(&opts.NoInput, "no-input", false, "disable interactive prompts")
	flags.BoolVar(&opts.NoColor, "no-color", false, "disable colored output")
	flags.StringVar(&opts.Config, "config", "", "path to config file")
	flags.StringVar(&opts.WorkDir, "work-dir", "", "path to working directory")

	root.AddCommand(
		newInitCommand(deps, &opts),
		newCacheCommand(deps, &opts),
		newClientCommand(deps, &opts),
		newScenarioCommand(deps, &opts),
		newDaemonCommand(deps, &opts),
	)

	return root
}

func Execute(root *cobra.Command) int {
	if err := root.Execute(); err != nil {
		_, _ = fmt.Fprintf(root.ErrOrStderr(), "error: %v\n", err)
		return exitCode(err)
	}
	return 0
}

func exitCode(err error) int {
	return 2
}
