package cli

import "github.com/spf13/cobra"

func newScenarioCommand(deps Dependencies, opts *GlobalOptions) *cobra.Command {
	return &cobra.Command{
		Use:   "scenario",
		Short: "Run client automation scenarios",
	}
}
