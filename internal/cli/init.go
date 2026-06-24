package cli

import "github.com/spf13/cobra"

func newInitCommand(deps Dependencies, opts *GlobalOptions) *cobra.Command {
	return &cobra.Command{
		Use:   "init",
		Short: "Initialize Craftwright configuration",
	}
}
