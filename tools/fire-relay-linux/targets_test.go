package main

import (
	"bytes"
	"context"
	"crypto/hmac"
	"encoding/hex"
	"encoding/json"
	"io"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"testing"
)

func profileConfig(t *testing.T) (string, *fixture, *fixture) {
	t.Helper()
	development := newFixture(t, "127.0.0.1:0", nil)
	production := newFixture(t, "127.0.0.1:0", nil)
	production.cfg.secret = bytes.Repeat([]byte("p"), 64)
	dir := t.TempDir()
	os.WriteFile(filepath.Join(dir, "dev.secret"), development.cfg.secret, 0600)
	os.WriteFile(filepath.Join(dir, "prod.secret"), production.cfg.secret, 0600)
	profiles := make(map[string]destinationConfig)
	for name, f := range map[string]*fixture{"development": development, "production": production} {
		target := f.cfg.Target
		target.Fingerprint = hex.EncodeToString(f.cfg.pin)
		target.SecretFile = map[string]string{"development": "dev.secret", "production": "prod.secret"}[name]
		profiles[name] = destinationConfig{Node: "site-01", Target: target}
	}
	cfg := config{Listen: endpoint{Address: "127.0.0.1", Port: 2000}, Maximum: 4, ConnectSeconds: 2,
		IdleSeconds: 5, RetrySeconds: 1, LogDirectory: "logs", ActiveTarget: "development", Destinations: profiles}
	data, _ := json.Marshal(cfg)
	path := filepath.Join(dir, "config.json")
	if err := os.WriteFile(path, data, 0640); err != nil {
		t.Fatal(err)
	}
	return path, development, production
}

func TestTargetSelectionAndIndependentCredentials(t *testing.T) {
	path, dev, prod := profileConfig(t)
	originalInfo, _ := os.Stat(path)
	cfg, err := loadConfig(path)
	if err != nil {
		t.Fatal(err)
	}
	if cfg.Target.Port != dev.cfg.Target.Port || !hmac.Equal(cfg.pin, dev.cfg.pin) {
		t.Fatal("wrong development target")
	}
	cfg, err = loadSelectedConfig(path, "production")
	if err != nil {
		t.Fatal(err)
	}
	if cfg.Target.Port != prod.cfg.Target.Port || !hmac.Equal(cfg.pin, prod.cfg.pin) {
		t.Fatal("wrong production target")
	}
	if err := probeTarget(context.Background(), cfg, log.New(io.Discard, "", 0)); err != nil {
		t.Fatal(err)
	}
	unchanged, _ := loadConfig(path)
	if unchanged.ActiveTarget != "development" {
		t.Fatal("runtime override persisted unexpectedly")
	}
	if err := saveActiveTarget(path, "production"); err != nil {
		t.Fatal(err)
	}
	changed, err := loadConfig(path)
	if err != nil || changed.ActiveTarget != "production" {
		t.Fatal("target not saved", err)
	}
	info, _ := os.Stat(path)
	if info.Mode().Perm() != originalInfo.Mode().Perm() {
		t.Fatal("config permissions changed")
	}
	backup, _ := os.ReadFile(path + ".before-target-switch")
	if !bytes.Contains(backup, []byte(`"development"`)) {
		t.Fatal("configuration backup missing")
	}
}

func TestInvalidOrUnconfiguredTargetPreservesWorkingConfig(t *testing.T) {
	path, _, _ := profileConfig(t)
	before, _ := os.ReadFile(path)
	if err := saveActiveTarget(path, "nonexistent"); err == nil {
		t.Fatal("unknown target saved")
	}
	after, _ := os.ReadFile(path)
	if !bytes.Equal(before, after) {
		t.Fatal("unknown target damaged working configuration")
	}
	var doc config
	json.Unmarshal(before, &doc)
	profile := doc.Destinations["production"]
	profile.Target.SecretFile = "not-installed.secret"
	doc.Destinations["production"] = profile
	data, _ := json.Marshal(doc)
	os.WriteFile(path, data, 0640)
	if _, err := loadConfig(path); err != nil {
		t.Fatal("inactive production blocks development", err)
	}
	if err := saveActiveTarget(path, "production"); err == nil {
		t.Fatal("unconfigured production saved")
	}
	after, _ = os.ReadFile(path)
	if !bytes.Equal(data, after) {
		t.Fatal("failed production switch damaged config")
	}
}

func TestLegacyConfigUpgradePreservesFieldSettings(t *testing.T) {
	template, dev, _ := profileConfig(t)
	dir := filepath.Dir(template)
	legacy := *dev.cfg
	legacy.Node = "custom-site"
	legacy.Target.Fingerprint = hex.EncodeToString(dev.cfg.pin)
	legacy.Target.SecretFile = "dev.secret"
	legacy.Listen = endpoint{Address: "10.5.90.50", Port: 2100}
	legacy.LogDirectory = "field-logs"
	data, _ := json.Marshal(legacy)
	path := filepath.Join(dir, "legacy.json")
	os.WriteFile(path, data, 0640)
	if err := importTargets(path, template); err != nil {
		t.Fatal(err)
	}
	cfg, err := loadConfig(path)
	if err != nil {
		t.Fatal(err)
	}
	if cfg.Node != "custom-site" || cfg.Listen.Port != 2100 || cfg.Target.Port != dev.cfg.Target.Port || cfg.ActiveTarget != "development" ||
		cfg.LogDirectory != filepath.Join(dir, "field-logs") {
		t.Fatal("field configuration overwritten")
	}
	if len(cfg.Destinations) != 2 {
		t.Fatal("production not imported")
	}
	if err := importTargets(path, template); err != nil {
		t.Fatal("repeat upgrade failed", err)
	}
	cfg, _ = loadConfig(path)
	if cfg.Node != "custom-site" {
		t.Fatal("repeat upgrade replaced existing profile")
	}
}

func TestStandaloneProfileOverrideAndSavedSelection(t *testing.T) {
	binary := os.Getenv("FIRE_RELAY_TEST_BIN")
	if binary == "" {
		t.Skip("standalone binary path not provided")
	}
	path, _, _ := profileConfig(t)
	for _, args := range [][]string{
		{"--config", path, "--target", "production", "--probe"},
		{"--config", path, "--select-target", "production"},
		{"--config", path, "--probe"},
	} {
		command := exec.Command(binary, args...)
		command.Env = append(os.Environ(), "PATH=/nonexistent")
		output, err := command.CombinedOutput()
		if err != nil {
			t.Fatal(string(output), err)
		}
	}
	cfg, err := loadConfig(path)
	if err != nil || cfg.ActiveTarget != "production" {
		t.Fatal("standalone selection failed", err)
	}
}
