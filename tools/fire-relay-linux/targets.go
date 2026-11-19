package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"time"
)

// Update only the selector: never rewrite credentials or resolved absolute paths.
// Validation takes place before this function, so an unusable target is not saved.
func saveActiveTarget(path, target string) error {
	path, err := filepath.Abs(path)
	if err != nil {
		return err
	}
	original, err := os.ReadFile(path)
	if err != nil {
		return err
	}
	var document map[string]json.RawMessage
	if err := json.Unmarshal(bytes.TrimPrefix(original, []byte{0xef, 0xbb, 0xbf}), &document); err != nil {
		return err
	}
	document["active_target"], err = json.Marshal(target)
	if err != nil {
		return err
	}
	return writeConfigDocument(path, original, document)
}

func importTargets(path, templatePath string) error {
	if _, err := loadConfig(templatePath); err != nil {
		return err
	}
	original, err := os.ReadFile(path)
	if err != nil {
		return err
	}
	template, err := os.ReadFile(templatePath)
	if err != nil {
		return err
	}
	var document, source map[string]json.RawMessage
	if err := json.Unmarshal(bytes.TrimPrefix(original, []byte{0xef, 0xbb, 0xbf}), &document); err != nil {
		return err
	}
	if err := json.Unmarshal(bytes.TrimPrefix(template, []byte{0xef, 0xbb, 0xbf}), &source); err != nil {
		return err
	}
	var incoming, current map[string]json.RawMessage
	if err := json.Unmarshal(source["destinations"], &incoming); err != nil {
		return err
	}
	if len(incoming) == 0 {
		return fmt.Errorf("模板没有 destinations")
	}
	if len(document["destinations"]) > 0 {
		if err := json.Unmarshal(document["destinations"], &current); err != nil {
			return err
		}
	}
	if len(current) == 0 {
		profile := make(map[string]json.RawMessage)
		for _, name := range []string{"node_id", "target", "ssh", "intercom"} {
			if value, found := document[name]; found {
				profile[name] = value
				delete(document, name)
			}
		}
		value, err := json.Marshal(profile)
		if err != nil {
			return err
		}
		current = map[string]json.RawMessage{"development": value}
		document["active_target"] = json.RawMessage(`"development"`)
	}
	for name, value := range incoming {
		if _, exists := current[name]; !exists {
			current[name] = value
		} else {
			// Add only the new optional feature. Preserve on-site targets, keys,
			// explicit intercom settings and the current active selector.
			var existingProfile, incomingProfile map[string]json.RawMessage
			if err := json.Unmarshal(current[name], &existingProfile); err != nil {
				return err
			}
			if err := json.Unmarshal(value, &incomingProfile); err != nil {
				return err
			}
			if _, configured := existingProfile["intercom"]; !configured {
				if feature, found := incomingProfile["intercom"]; found {
					existingProfile["intercom"] = feature
					current[name], err = json.Marshal(existingProfile)
					if err != nil {
						return err
					}
				}
			}
		}
	}
	document["destinations"], err = json.Marshal(current)
	if err != nil {
		return err
	}
	return writeConfigDocument(path, original, document)
}

func writeConfigDocument(path string, original []byte, document map[string]json.RawMessage) error {
	content, err := json.MarshalIndent(document, "", "  ")
	if err != nil {
		return err
	}
	content = append(content, '\n')
	info, err := os.Stat(path)
	if err != nil {
		return err
	}
	backup := path + ".before-target-switch"
	if err := os.WriteFile(backup, original, 0600); err != nil {
		return err
	}
	temporary, err := os.CreateTemp(filepath.Dir(path), ".relay-config-*")
	if err != nil {
		return err
	}
	defer os.Remove(temporary.Name())
	if err := temporary.Chmod(info.Mode().Perm()); err != nil {
		temporary.Close()
		return err
	}
	if err := preserveConfigOwner(temporary, info); err != nil {
		temporary.Close()
		return err
	}
	if _, err := temporary.Write(content); err != nil {
		temporary.Close()
		return err
	}
	if err := temporary.Sync(); err != nil {
		temporary.Close()
		return err
	}
	if err := temporary.Close(); err != nil {
		return err
	}
	if _, err := loadConfig(temporary.Name()); err != nil {
		return err
	}
	return os.Rename(temporary.Name(), path)
}

func probeTarget(parent context.Context, cfg *config, logger *log.Logger) error {
	if !cfg.SSH.Enabled {
		conn, err := connectTarget(parent, cfg, "", 0)
		if err == nil {
			conn.Close()
		}
		return err
	}
	// Stop only the tunnel process started by this probe. An existing service
	// tunnel may also answer; the mandatory certificate pin authenticates its peer.
	ctx, cancel := context.WithTimeout(parent, 25*time.Second)
	done := make(chan struct{})
	go func() { defer close(done); newApp(cfg, logger).sshTunnel(ctx) }()
	defer func() { cancel(); <-done }()
	var last error
	for ctx.Err() == nil {
		conn, err := connectTarget(ctx, cfg, "", 0)
		if err == nil {
			conn.Close()
			return nil
		}
		last = err
		if !pause(ctx, .3) {
			break
		}
	}
	return fmt.Errorf("SSH通道或服务器接收端尚不可用：%v（%v）", last, ctx.Err())
}
