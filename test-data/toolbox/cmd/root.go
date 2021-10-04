/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package cmd

import (
	"fmt"
	"os"

	"github.com/spf13/cobra"
)

const (
	appVersion = "0.0.1"
)

var rootCmd = &cobra.Command{
	Use:   "toolbox",
	Short: "Imaging Ingestion Toolbox",
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Println("Run with --help to find all commands")
	},
	Version: appVersion,
}

// Execute executes the root command.
func Execute() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
