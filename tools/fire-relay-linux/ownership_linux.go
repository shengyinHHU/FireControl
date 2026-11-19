package main

import (
	"os"
	"syscall"
)

func preserveConfigOwner(file *os.File, info os.FileInfo) error {
	stat := info.Sys().(*syscall.Stat_t)
	return file.Chown(int(stat.Uid), int(stat.Gid))
}
