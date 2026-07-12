package main

import (
	"os"

	"github.com/kiseki1011/TISSUE/tui/internal/cli"
)

func main() {
	os.Exit(cli.Execute())
}
