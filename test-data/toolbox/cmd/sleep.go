/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package cmd

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"

	"github.com/spf13/cobra"
)

func init() {
	rootCmd.AddCommand(sleepCmd)
}

var sleepCmd = &cobra.Command{
	Use:   "sleep",
	Short: "Sleep Forever",
	Run: func(cmd *cobra.Command, args []string) {
		sleepForever(cmd, args)
	},
}

func sleepForever(cmd *cobra.Command, args []string) {
	exitSignal := make(chan os.Signal)
	signal.Notify(exitSignal, syscall.SIGINT, syscall.SIGTERM)
	fmt.Println("Sleeping forever until an exit signal ....")
	<-exitSignal
}
