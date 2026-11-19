package main

import (
	"os"
	"syscall"
	"testing"
)

func TestRootSwitchPreservesServiceGroup(t *testing.T) {
	if os.Geteuid() != 0 {
		t.Skip("requires root to emulate service config group")
	}
	path, _, _ := profileConfig(t)
	if err := os.Chown(path, 0, 123); err != nil {
		t.Fatal(err)
	}
	if err := saveActiveTarget(path, "production"); err != nil {
		t.Fatal(err)
	}
	info, _ := os.Stat(path)
	stat := info.Sys().(*syscall.Stat_t)
	if stat.Uid != 0 || stat.Gid != 123 || info.Mode().Perm() != 0640 {
		t.Fatal("service account would lose access after root switched config")
	}
}
