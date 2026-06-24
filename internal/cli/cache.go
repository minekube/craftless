package cli

import "github.com/spf13/cobra"

func newCacheCommand(deps Dependencies, opts *GlobalOptions) *cobra.Command {
	return &cobra.Command{
		Use:   "cache",
		Short: "Manage cached Minecraft assets",
	}
}
