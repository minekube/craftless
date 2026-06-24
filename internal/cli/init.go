package cli

import (
	"fmt"

	"github.com/minekube/craftwright/internal/project"
	"github.com/spf13/cobra"
)

func newInitCommand(deps Dependencies, opts *GlobalOptions) *cobra.Command {
	var dir string
	var dryRun bool
	var force bool

	cmd := &cobra.Command{
		Use:   "init",
		Short: "Initialize Craftwright configuration",
		Args:  cobra.NoArgs,
		RunE: func(cmd *cobra.Command, args []string) error {
			root := dir
			if root == "" {
				root = opts.WorkDir
			}
			if root == "" {
				root = "."
			}
			if dryRun {
				if opts.JSON {
					return WriteJSON(cmd.OutOrStdout(), map[string]any{"ok": true, "dir": root, "dryRun": true})
				}
				_, err := fmt.Fprintf(cmd.OutOrStdout(), "Would initialize Craftwright project at %s\n", root)
				return err
			}
			if err := project.Init(project.Layout{Root: root}, force); err != nil {
				return usageError("%v", err)
			}
			if opts.JSON {
				return WriteJSON(cmd.OutOrStdout(), map[string]any{"ok": true, "dir": root})
			}
			_, err := fmt.Fprintf(cmd.OutOrStdout(), "Initialized Craftwright project at %s\n", root)
			return err
		},
	}
	cmd.Flags().StringVar(&dir, "dir", "", "project directory")
	cmd.Flags().BoolVar(&dryRun, "dry-run", false, "show what would be initialized")
	cmd.Flags().BoolVar(&force, "force", false, "overwrite existing configuration")
	return cmd
}
