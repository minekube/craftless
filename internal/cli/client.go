package cli

import "github.com/spf13/cobra"

func newClientCommand(deps Dependencies, opts *GlobalOptions) *cobra.Command {
	return &cobra.Command{
		Use:   "client",
		Short: "Manage Minecraft Java clients",
	}
}
