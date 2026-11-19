package main

import "os"

func preserveConfigOwner(file *os.File, info os.FileInfo) error { return nil }
