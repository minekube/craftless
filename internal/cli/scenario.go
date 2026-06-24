package cli

import (
	"context"
	"fmt"

	"github.com/minekube/craftwright/internal/scenario"
	"github.com/spf13/cobra"
)

func newScenarioCommand(deps Dependencies, opts *GlobalOptions) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "scenario",
		Short: "Run client automation scenarios",
		RunE: func(cmd *cobra.Command, args []string) error {
			return cmd.Help()
		},
	}
	cmd.AddCommand(
		newScenarioRunCommand(deps, opts),
		newScenarioValidateCommand(deps, opts),
	)
	return cmd
}

func newScenarioRunCommand(deps Dependencies, opts *GlobalOptions) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "run FILE",
		Short: "Run a scenario file",
		Args:  cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			result, err := scenario.RunFile(context.Background(), deps.Engine, args[0])
			if err != nil {
				return err
			}
			return writeScenarioResult(cmd, opts, "Ran scenario", result)
		},
	}
	return cmd
}

func newScenarioValidateCommand(deps Dependencies, opts *GlobalOptions) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "validate FILE",
		Short: "Validate a scenario file",
		Args:  cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			result, err := scenario.ValidateFile(args[0])
			if err != nil {
				return err
			}
			return writeScenarioResult(cmd, opts, "Scenario valid", result)
		},
	}
	return cmd
}

func writeScenarioResult(cmd *cobra.Command, opts *GlobalOptions, label string, result scenario.Result) error {
	if opts.JSON {
		return WriteJSON(cmd.OutOrStdout(), result)
	}
	if opts.Plain {
		_, err := fmt.Fprintf(cmd.OutOrStdout(), "ok %d\n", result.Steps)
		return err
	}
	_, err := fmt.Fprintf(cmd.OutOrStdout(), "%s: %d steps\n", label, result.Steps)
	return err
}
