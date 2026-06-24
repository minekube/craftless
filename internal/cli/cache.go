package cli

import (
	"fmt"

	"github.com/minekube/craftwright/internal/project"
	"github.com/spf13/cobra"
)

func newCacheCommand(deps Dependencies, opts *GlobalOptions) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "cache",
		Short: "Manage cached Minecraft assets",
	}
	cmd.AddCommand(newCachePrepareCommand(opts))
	return cmd
}

func newCachePrepareCommand(opts *GlobalOptions) *cobra.Command {
	var mc string
	var loader string
	var profile string

	cmd := &cobra.Command{
		Use:   "prepare",
		Short: "Prepare Minecraft cache metadata",
		Args:  cobra.NoArgs,
		RunE: func(cmd *cobra.Command, args []string) error {
			if mc == "" {
				return usageError("--mc is required")
			}

			record, err := project.PrepareCache(project.Layout{Root: opts.WorkDir}, project.CacheRequest{
				Minecraft: mc,
				Loader:    loader,
				Profile:   profile,
			})
			if err != nil {
				return appError{Code: 8, Err: fmt.Errorf("prepare cache: %w", err)}
			}
			if opts.JSON {
				return WriteJSON(cmd.OutOrStdout(), map[string]any{"ok": true, "cache": record})
			}
			_, err = fmt.Fprintf(cmd.OutOrStdout(), "Prepared cache metadata for Minecraft %s with %s profile %s\n", record.Minecraft, record.Loader, record.Profile)
			return err
		},
	}
	cmd.Flags().StringVar(&mc, "mc", "", "Minecraft version")
	cmd.Flags().StringVar(&loader, "loader", "fabric", "mod loader")
	cmd.Flags().StringVar(&profile, "profile", "default", "cache profile")
	return cmd
}
