package main

import (
	"os"

	"github.com/minekube/craftwright/internal/cli"
	"github.com/minekube/craftwright/internal/engine"
)

var version = "dev"

func main() {
	root := cli.NewRoot(cli.Dependencies{
		Engine:  engine.NewMemory(),
		Stdout:  os.Stdout,
		Stderr:  os.Stderr,
		Version: version,
	})
	os.Exit(cli.Execute(root))
}
