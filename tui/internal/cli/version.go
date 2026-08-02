package cli

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/kiseki1011/TISSUE/tui/internal/version"
)

func newVersionCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "version",
		Short: "Print the client version",
		Args:  cobra.NoArgs,
		Run: func(_ *cobra.Command, _ []string) {
			fmt.Println("tissue", version.Version)
		},
	}
}
