package cli

import "github.com/spf13/cobra"

func newDaemonCommand(deps Dependencies, opts *GlobalOptions) *cobra.Command {
	return &cobra.Command{
		Use:   "daemon",
		Short: "Run the Craftwright daemon",
	}
}
