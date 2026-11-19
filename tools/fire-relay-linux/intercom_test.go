package main

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"log"
	"net"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

func TestIntercomRejectsUnsafeAndConflictingEndpoints(t *testing.T) {
	dir := t.TempDir()
	os.WriteFile(filepath.Join(dir, "key"), []byte("fixture"), 0600)
	base := config{SSH: sshConfig{Enabled: true, RemotePort: 22000}, Intercom: intercomConfig{Enabled: true, PrivateKey: "key", Cameras: []cameraTunnel{{Name: "shanpan", Address: "10.5.90.150", Port: 37777, ServerPort: 33777}}}}
	if err := base.validateIntercom(dir); err != nil {
		t.Fatal(err)
	}
	for _, address := range []string{"127.0.0.1", "0.0.0.0", "8.8.8.8", "169.254.169.254", "10.5.090.150", "10.5.90.999", "camera.invalid"} {
		cfg := base
		cfg.Intercom.Cameras = append([]cameraTunnel(nil), base.Intercom.Cameras...)
		cfg.Intercom.Cameras[0].Address = address
		if cfg.validateIntercom(dir) == nil {
			t.Fatalf("unsafe target accepted: %s", address)
		}
	}
	for _, port := range []int{80, 22000, 65536} {
		cfg := base
		cfg.Intercom.Cameras = append([]cameraTunnel(nil), base.Intercom.Cameras...)
		cfg.Intercom.Cameras[0].ServerPort = port
		if cfg.validateIntercom(dir) == nil {
			t.Fatalf("unsafe server port accepted: %d", port)
		}
	}
	cfg := base
	cfg.Intercom.Cameras = append(append([]cameraTunnel(nil), base.Intercom.Cameras...), base.Intercom.Cameras[0])
	if cfg.validateIntercom(dir) == nil {
		t.Fatal("duplicate mapping accepted")
	}
	cfg = base
	cfg.SSH.Enabled = false
	if cfg.validateIntercom(dir) == nil {
		t.Fatal("intercom without SSH accepted")
	}
	args := base.intercomArguments()
	joined := strings.Join(args, " ")
	if !strings.Contains(joined, "-R 127.0.0.1:33777:10.5.90.150:37777") || strings.Contains(joined, " -L ") || !strings.Contains(joined, "StrictHostKeyChecking=yes") {
		t.Fatal("invalid isolated reverse tunnel arguments", joined)
	}
}

func TestIntercomUpgradePreservesExistingProfiles(t *testing.T) {
	path, _, _ := profileConfig(t)
	data, _ := os.ReadFile(path)
	var document map[string]any
	json.Unmarshal(data, &document)
	profiles := document["destinations"].(map[string]any)
	for _, value := range profiles {
		delete(value.(map[string]any), "intercom")
	}
	data, _ = json.Marshal(document)
	os.WriteFile(path, data, 0600)
	template := filepath.Join(filepath.Dir(path), "template.json")
	prod := profiles["production"].(map[string]any)
	prod["intercom"] = map[string]any{"enabled": false, "cameras": []any{map[string]any{"name": "shanpan", "device_address": "10.5.90.150", "device_port": 37777, "server_port": 33777}}}
	incoming, _ := json.Marshal(document)
	os.WriteFile(template, incoming, 0600)
	if err := importTargets(path, template); err != nil {
		t.Fatal(err)
	}
	upgraded, err := loadConfig(path)
	if err != nil || upgraded.ActiveTarget != "development" || len(upgraded.Destinations["production"].Intercom.Cameras) != 1 {
		t.Fatal("upgrade lost new feature or current target", err)
	}
	// An explicit on-site disabled feature must survive subsequent imports.
	document["destinations"].(map[string]any)["production"].(map[string]any)["intercom"] = map[string]any{"enabled": false}
	data, _ = json.Marshal(document)
	os.WriteFile(path, data, 0600)
	if err := importTargets(path, template); err != nil {
		t.Fatal(err)
	}
	upgraded, _ = loadConfig(path)
	if len(upgraded.Destinations["production"].Intercom.Cameras) != 0 {
		t.Fatal("explicit on-site feature settings replaced")
	}
}

func TestCameraProbeDoesNotClaimAudioSuccess(t *testing.T) {
	listener, err := net.Listen("tcp4", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	go func() {
		c, e := listener.Accept()
		if e == nil {
			c.Close()
		}
	}()
	cfg := config{ConnectSeconds: 1, Intercom: intercomConfig{Enabled: true, Cameras: []cameraTunnel{{Name: "fixture", Address: "127.0.0.1", Port: listener.Addr().(*net.TCPAddr).Port}}}}
	var output bytes.Buffer
	if err := probeCamerasTCP(context.Background(), &cfg, log.New(&output, "", 0)); err != nil || !strings.Contains(output.String(), "CAMERA_TCP_OK") {
		t.Fatal(err, output.String())
	}
	listener.Close()
	if probeCamerasTCP(context.Background(), &cfg, log.New(io.Discard, "", 0)) == nil {
		t.Fatal("closed camera port accepted")
	}
}

type sshTestSpec struct {
	SSH         sshConfig `json:"ssh"`
	ServerPort  int       `json:"server_port"`
	ControlPort int       `json:"control_port"`
}

func sshSpec(t *testing.T) sshTestSpec {
	t.Helper()
	name := os.Getenv("FIRE_RELAY_SSH_FIXTURE")
	if name == "" {
		t.Skip("real SSH fixture not requested")
	}
	data, err := os.ReadFile(name)
	if err != nil {
		t.Fatal(err)
	}
	var spec sshTestSpec
	if err = json.Unmarshal(data, &spec); err != nil {
		t.Fatal(err)
	}
	return spec
}

func reportRoundTrip(t *testing.T, address string) {
	t.Helper()
	c, err := net.DialTimeout("tcp4", address, 3*time.Second)
	if err != nil {
		t.Fatal(err)
	}
	defer c.Close()
	c.SetDeadline(time.Now().Add(5 * time.Second))
	body := []byte("fire-report-independent-from-intercom")
	if err := writeAll(c, body); err != nil {
		t.Fatal(err)
	}
	received := make([]byte, len(body))
	if _, err := io.ReadFull(c, received); err != nil || !bytes.Equal(received, body) {
		t.Fatal("fire path interrupted", err)
	}
}

func TestSSHIntercomBidirectionalAndReconnect(t *testing.T) {
	spec := sshSpec(t)
	camera, err := net.Listen("tcp4", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	var cameraWG sync.WaitGroup
	acceptDone := make(chan struct{})
	defer func() { camera.Close(); <-acceptDone; cameraWG.Wait() }()
	go func() {
		defer close(acceptDone)
		for {
			c, e := camera.Accept()
			if e != nil {
				return
			}
			cameraWG.Add(1)
			go func() {
				defer cameraWG.Done()
				defer c.Close()
				c.SetDeadline(time.Now().Add(10 * time.Second))
				writeAll(c, []byte("camera-ready"))
				io.Copy(c, c)
			}()
		}
	}()
	f := newFixture(t, "127.0.0.1:0", nil)
	cfg := *f.cfg
	cfg.SSH = spec.SSH
	cfg.SSH.Enabled = false
	// Only the test bypasses private-address validation, keeping every packet on loopback.
	cfg.Intercom = intercomConfig{Enabled: true, PrivateKey: spec.SSH.PrivateKey, Cameras: []cameraTunnel{{Name: "fixture", Address: "127.0.0.1", Port: camera.Addr().(*net.TCPAddr).Port, ServerPort: spec.ServerPort}}}
	_, listener, cancel, done := startApp(t, &cfg)
	address := net.JoinHostPort("127.0.0.1", strconv.Itoa(spec.ServerPort))
	connect := func() net.Conn {
		t.Helper()
		until := time.Now().Add(12 * time.Second)
		for time.Now().Before(until) {
			c, e := net.DialTimeout("tcp4", address, 200*time.Millisecond)
			if e == nil {
				c.SetDeadline(time.Now().Add(2 * time.Second))
				head := make([]byte, 12)
				if _, e = io.ReadFull(c, head); e == nil && string(head) == "camera-ready" {
					return c
				}
				c.Close()
			}
			time.Sleep(50 * time.Millisecond)
		}
		t.Fatal("reverse camera tunnel not ready")
		return nil
	}
	c := connect()
	payload := bytes.Repeat([]byte{0, 1, 127, 255, 32, 0, 64, 9}, 8192)
	var transfer sync.WaitGroup
	for i := 0; i < 4; i++ {
		connection := c
		if i > 0 {
			connection = connect()
		}
		transfer.Add(1)
		go func() {
			defer transfer.Done()
			defer connection.Close()
			connection.SetDeadline(time.Now().Add(8 * time.Second))
			sent := make(chan error, 1)
			go func() { sent <- writeAll(connection, payload) }()
			received := make([]byte, len(payload))
			_, err := io.ReadFull(connection, received)
			if err != nil || !bytes.Equal(payload, received) {
				t.Errorf("bidirectional camera data mismatch: %v", err)
			}
			if err := <-sent; err != nil {
				t.Error(err)
			}
		}()
	}
	reportRoundTrip(t, listener.Addr().String())
	transfer.Wait()
	control, err := net.Dial("tcp4", net.JoinHostPort("127.0.0.1", strconv.Itoa(spec.ControlPort)))
	if err != nil {
		t.Fatal(err)
	}
	control.SetDeadline(time.Now().Add(3 * time.Second))
	writeAll(control, []byte("DROP\n"))
	var ack [1]byte
	if _, err := io.ReadFull(control, ack[:]); err != nil {
		t.Fatal(err)
	}
	control.Close()
	reportRoundTrip(t, listener.Addr().String())
	reconnected := connect()
	reconnected.Close()
	cancel()
	select {
	case <-done:
	case <-time.After(5 * time.Second):
		t.Fatal("owned SSH process did not stop")
	}
	camera.Close()
}

type failureCounter struct{ count atomic.Int32 }

func (w *failureCounter) Write(p []byte) (int, error) {
	if strings.Contains(string(p), "通道已退出") {
		w.count.Add(1)
	}
	return len(p), nil
}
func TestSSHIntercomDeniedPortDoesNotStopReports(t *testing.T) {
	spec := sshSpec(t)
	f := newFixture(t, "127.0.0.1:0", nil)
	cfg := *f.cfg
	cfg.SSH = spec.SSH
	cfg.SSH.Enabled = false
	portListener, _ := net.Listen("tcp4", "127.0.0.1:0")
	deniedPort := portListener.Addr().(*net.TCPAddr).Port
	portListener.Close()
	cfg.Intercom = intercomConfig{Enabled: true, PrivateKey: spec.SSH.PrivateKey, Cameras: []cameraTunnel{{Name: "denied", Address: "127.0.0.1", Port: 1, ServerPort: deniedPort}}}
	writer := &failureCounter{}
	listener, err := net.Listen("tcp4", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithCancel(context.Background())
	app := newApp(&cfg, log.New(writer, "", 0))
	done := make(chan struct{})
	go func() { defer close(done); app.serve(ctx, listener) }()
	defer func() { cancel(); <-done }()
	until := time.Now().Add(8 * time.Second)
	for writer.count.Load() == 0 && time.Now().Before(until) {
		time.Sleep(20 * time.Millisecond)
	}
	if writer.count.Load() == 0 {
		t.Fatal("SSH did not reject the forbidden camera listener")
	}
	reportRoundTrip(t, listener.Addr().String())
	reportRoundTrip(t, listener.Addr().String())
}
