package main

import (
	"bytes"
	"context"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"io"
	"log"
	"math/big"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"sync"
	"sync/atomic"
	"syscall"
	"testing"
	"time"
)

type fixture struct {
	listener    net.Listener
	certificate tls.Certificate
	cfg         *config
	mu          sync.Mutex
	closed      bool
	connections map[net.Conn]bool
	sources     map[string]int
	bytes       atomic.Int64
	wg          sync.WaitGroup
}

func newFixture(t *testing.T, address string, cert *tls.Certificate) *fixture {
	t.Helper()
	if cert == nil {
		key, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
		if err != nil {
			t.Fatal(err)
		}
		template := &x509.Certificate{SerialNumber: big.NewInt(123), Subject: pkix.Name{CommonName: "fixture"},
			NotBefore: time.Now().Add(-time.Hour), NotAfter: time.Now().Add(time.Hour), KeyUsage: x509.KeyUsageDigitalSignature,
			ExtKeyUsage: []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth}}
		der, err := x509.CreateCertificate(rand.Reader, template, template, &key.PublicKey, key)
		if err != nil {
			t.Fatal(err)
		}
		private, _ := x509.MarshalECPrivateKey(key)
		created, err := tls.X509KeyPair(pem.EncodeToMemory(&pem.Block{Type: "CERTIFICATE", Bytes: der}),
			pem.EncodeToMemory(&pem.Block{Type: "EC PRIVATE KEY", Bytes: private}))
		if err != nil {
			t.Fatal(err)
		}
		cert = &created
	}
	listener, err := tls.Listen("tcp4", address, &tls.Config{Certificates: []tls.Certificate{*cert}, MinVersion: tls.VersionTLS12})
	if err != nil {
		t.Fatal(err)
	}
	pin := sha256.Sum256(cert.Certificate[0])
	fixture := &fixture{listener: listener, certificate: *cert, connections: make(map[net.Conn]bool), sources: make(map[string]int),
		cfg: &config{Node: "site-01", Target: targetConfig{Address: "127.0.0.1", Port: listener.Addr().(*net.TCPAddr).Port},
			ConnectSeconds: 2, RetrySeconds: 1, IdleSeconds: 5, Maximum: 4, secret: bytes.Repeat([]byte("k"), 64), pin: pin[:]}}
	fixture.wg.Add(1)
	go func() {
		defer fixture.wg.Done()
		for {
			conn, err := listener.Accept()
			if err != nil {
				return
			}
			fixture.mu.Lock()
			if fixture.closed {
				fixture.mu.Unlock()
				conn.Close()
				return
			}
			fixture.connections[conn] = true
			fixture.wg.Add(1)
			fixture.mu.Unlock()
			go fixture.session(conn)
		}
	}()
	t.Cleanup(fixture.Close)
	return fixture
}

func (f *fixture) Close() {
	f.mu.Lock()
	f.closed = true
	f.listener.Close()
	for conn := range f.connections {
		conn.Close()
	}
	f.mu.Unlock()
	f.wg.Wait()
}

func (f *fixture) session(conn net.Conn) {
	defer f.wg.Done()
	defer func() { conn.Close(); f.mu.Lock(); delete(f.connections, conn); f.mu.Unlock() }()
	conn.SetDeadline(time.Now().Add(3 * time.Second))
	challenge := make([]byte, 36)
	copy(challenge, magic)
	rand.Read(challenge[4:])
	if writeAll(conn, challenge) != nil {
		return
	}
	var header bytes.Buffer
	kind := make([]byte, 1)
	if _, err := io.ReadFull(conn, kind); err != nil {
		return
	}
	header.Write(kind)
	readText := func(limit int) (string, error) {
		var length uint16
		if err := binary.Read(conn, binary.BigEndian, &length); err != nil {
			return "", err
		}
		if int(length) > limit {
			return "", fmt.Errorf("oversized header")
		}
		data := make([]byte, length)
		if _, err := io.ReadFull(conn, data); err != nil {
			return "", err
		}
		binary.Write(&header, binary.BigEndian, length)
		header.Write(data)
		return string(data), nil
	}
	node, err := readText(64)
	if err != nil {
		return
	}
	ip, err := readText(15)
	if err != nil {
		return
	}
	var port uint16
	if binary.Read(conn, binary.BigEndian, &port) != nil {
		return
	}
	binary.Write(&header, binary.BigEndian, port)
	id, err := readText(36)
	if err != nil || len(id) != 36 {
		return
	}
	supplied := make([]byte, 32)
	if _, err := io.ReadFull(conn, supplied); err != nil {
		return
	}
	mac := hmac.New(sha256.New, f.cfg.secret)
	mac.Write(challenge)
	mac.Write(header.Bytes())
	if !hmac.Equal(mac.Sum(nil), supplied) || node != f.cfg.Node {
		return
	}
	if writeAll(conn, []byte{1}) != nil {
		return
	}
	if kind[0] == 0 && ip == "" && port == 0 {
		return
	}
	if kind[0] != 1 || net.ParseIP(ip).To4() == nil || port == 0 {
		return
	}
	f.mu.Lock()
	f.sources[ip]++
	f.mu.Unlock()
	conn.SetDeadline(time.Time{})
	buffer := make([]byte, 8192)
	for {
		n, err := conn.Read(buffer)
		if n > 0 {
			f.bytes.Add(int64(n))
			if writeAll(conn, buffer[:n]) != nil {
				return
			}
		}
		if err != nil {
			return
		}
	}
}

func TestAuthenticationAndOriginalIdentity(t *testing.T) {
	f := newFixture(t, "127.0.0.1:0", nil)
	for _, ip := range []string{"10.5.89.201", "10.5.90.49"} {
		conn, err := connectTarget(context.Background(), f.cfg, ip, 35001)
		if err != nil {
			t.Fatal(err)
		}
		data := []byte("@@original-device-data##")
		conn.SetDeadline(time.Now().Add(3 * time.Second))
		if writeAll(conn, data) != nil {
			t.Fatal("write")
		}
		received := make([]byte, len(data))
		if _, err := io.ReadFull(conn, received); err != nil || !bytes.Equal(data, received) {
			t.Fatal("round-trip mismatch", err)
		}
		conn.Close()
	}
	f.mu.Lock()
	first, second := f.sources["10.5.89.201"], f.sources["10.5.90.49"]
	f.mu.Unlock()
	if first != 1 || second != 1 {
		t.Fatal("source identities collapsed")
	}
	before := f.bytes.Load()
	for _, change := range []func(*config){func(c *config) { c.secret = []byte(strings.Repeat("x", 64)) }, func(c *config) { c.pin = make([]byte, 32) }, func(c *config) { c.Node = "wrong-node" }} {
		bad := *f.cfg
		change(&bad)
		conn, err := connectTarget(context.Background(), &bad, "10.5.90.49", 35002)
		if err == nil {
			conn.Close()
			t.Fatal("invalid identity/certificate accepted")
		}
	}
	probe, err := connectTarget(context.Background(), f.cfg, "", 0)
	if err != nil {
		t.Fatal(err)
	}
	probe.Close()
	if f.bytes.Load() != before {
		t.Fatal("authentication or health probe emitted business payload")
	}
}

func startApp(t *testing.T, cfg *config) (*relayApp, net.Listener, context.CancelFunc, <-chan struct{}) {
	t.Helper()
	listener, err := net.Listen("tcp4", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithCancel(context.Background())
	app := newApp(cfg, log.New(io.Discard, "", 0))
	done := make(chan struct{})
	go func() { defer close(done); app.serve(ctx, listener) }()
	t.Cleanup(func() {
		cancel()
		select {
		case <-done:
		case <-time.After(8 * time.Second):
			t.Error("relay did not stop")
		}
	})
	return app, listener, cancel, done
}

func TestRelayBackpressureHalfCloseAndCapacity(t *testing.T) {
	f := newFixture(t, "127.0.0.1:0", nil)
	cfg := *f.cfg
	cfg.Maximum = 1
	app, listener, cancel, done := startApp(t, &cfg)
	device, err := net.DialTimeout("tcp4", listener.Addr().String(), 3*time.Second)
	if err != nil {
		t.Fatal(err)
	}
	device.SetDeadline(time.Now().Add(8 * time.Second))
	payload := bytes.Repeat([]byte{0, 64, 35, 2, 3}, 40000)
	writeDone := make(chan error, 1)
	go func() { writeDone <- writeAll(device, payload) }()
	received := make([]byte, len(payload))
	if _, err := io.ReadFull(device, received); err != nil {
		t.Fatal(err)
	}
	if err := <-writeDone; err != nil || !bytes.Equal(payload, received) {
		t.Fatal("bounded forwarding changed bytes", err)
	}
	second, err := net.Dial("tcp4", listener.Addr().String())
	if err != nil {
		t.Fatal(err)
	}
	second.SetDeadline(time.Now().Add(time.Second))
	buffer := make([]byte, 1)
	n, _ := second.Read(buffer)
	second.Close()
	if n != 0 {
		t.Fatal("over-capacity connection received a fabricated response")
	}
	if err := device.(*net.TCPConn).CloseWrite(); err != nil {
		t.Fatal(err)
	}
	device.Close()
	cancel()
	select {
	case <-done:
	case <-time.After(8 * time.Second):
		t.Fatal("shutdown hung")
	}
	if app.uploaded.Load() != int64(len(payload)) || app.downloaded.Load() != int64(len(payload)) {
		t.Fatal("incorrect transfer counters")
	}
}

func TestReceiverRestartAndNoFakeConfirmation(t *testing.T) {
	f := newFixture(t, "127.0.0.1:0", nil)
	cfg := *f.cfg
	_, listener, _, _ := startApp(t, &cfg)
	address := f.listener.Addr().String()
	certificate := f.certificate
	f.Close()
	device, err := net.Dial("tcp4", listener.Addr().String())
	if err != nil {
		t.Fatal(err)
	}
	device.SetDeadline(time.Now().Add(4 * time.Second))
	device.Write([]byte("@@report##"))
	b := make([]byte, 30)
	n, _ := device.Read(b)
	device.Close()
	if n != 0 {
		t.Fatal("upstream outage manufactured confirmation")
	}
	newFixture(t, address, &certificate)
	recovered, err := net.Dial("tcp4", listener.Addr().String())
	if err != nil {
		t.Fatal(err)
	}
	defer recovered.Close()
	recovered.SetDeadline(time.Now().Add(4 * time.Second))
	payload := []byte("@@after-restart##")
	writeAll(recovered, payload)
	data := make([]byte, len(payload))
	if _, err := io.ReadFull(recovered, data); err != nil || !bytes.Equal(data, payload) {
		t.Fatal("restart did not recover", err)
	}
}

func TestConfigAndStandaloneExecutable(t *testing.T) {
	f := newFixture(t, "127.0.0.1:0", nil)
	dir := t.TempDir()
	key := filepath.Join(dir, "relay.secret")
	if err := os.WriteFile(key, f.cfg.secret, 0600); err != nil {
		t.Fatal(err)
	}
	cfg := *f.cfg
	cfg.Listen = endpoint{Address: "127.0.0.1", Port: 22001}
	cfg.Target.SecretFile = "relay.secret"
	cfg.Target.Fingerprint = hex.EncodeToString(cfg.pin)
	cfg.LogDirectory = "logs"
	data, _ := json.Marshal(cfg)
	path := filepath.Join(dir, "config.json")
	os.WriteFile(path, append([]byte{0xef, 0xbb, 0xbf}, data...), 0600)
	loaded, err := loadConfig(path)
	if err != nil {
		t.Fatal(err)
	}
	if loaded.LogDirectory != filepath.Join(dir, "logs") || !hmac.Equal(loaded.secret, f.cfg.secret) {
		t.Fatal("relative files not resolved beside config")
	}
	bad := bytes.Replace(data, []byte(`"node_id"`), []byte(`"unknown_node_field"`), 1)
	os.WriteFile(path, bad, 0600)
	if _, err := loadConfig(path); err == nil {
		t.Fatal("unknown config was silently accepted")
	}
	os.WriteFile(path, data, 0600)
	binaryPath := os.Getenv("FIRE_RELAY_TEST_BIN")
	if binaryPath == "" {
		t.Skip("standalone binary path not provided")
	}
	executable, err := os.ReadFile(binaryPath)
	if err != nil {
		t.Fatal(err)
	}
	local := filepath.Join(dir, "fire-relay")
	os.WriteFile(local, executable, 0755)
	command := exec.Command(local, "--check-config")
	command.Dir = "/"
	command.Env = append(os.Environ(), "PATH=/nonexistent")
	output, err := command.CombinedOutput()
	if err != nil || !bytes.Contains(output, []byte("CONFIG_OK")) {
		t.Fatal("default config lookup failed", string(output), err)
	}
	command = exec.Command(local, "--probe")
	command.Dir = "/"
	command.Env = append(os.Environ(), "PATH=/nonexistent")
	output, err = command.CombinedOutput()
	if err != nil || !bytes.Contains(output, []byte("PROBE_OK")) {
		t.Fatal("standalone TLS probe failed", string(output), err)
	}
	free, err := net.Listen("tcp4", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	cfg.Listen.Port = free.Addr().(*net.TCPAddr).Port
	free.Close()
	data, _ = json.Marshal(cfg)
	os.WriteFile(path, data, 0600)
	command = exec.Command(local, "--headless")
	command.Dir = "/"
	command.Env = append(os.Environ(), "PATH=/nonexistent")
	var logs bytes.Buffer
	command.Stdout = &logs
	command.Stderr = &logs
	if err := command.Start(); err != nil {
		t.Fatal(err)
	}
	defer func() {
		if command.ProcessState == nil {
			command.Process.Kill()
			command.Wait()
		}
	}()
	var device net.Conn
	for i := 0; i < 100; i++ {
		device, err = net.DialTimeout("tcp4", fmt.Sprintf("127.0.0.1:%d", cfg.Listen.Port), 50*time.Millisecond)
		if err == nil {
			break
		}
		time.Sleep(30 * time.Millisecond)
	}
	if device == nil {
		t.Fatal("standalone listener did not start")
	}
	device.SetDeadline(time.Now().Add(4 * time.Second))
	payload := []byte("@@standalone-no-jdk-python##")
	writeAll(device, payload)
	received := make([]byte, len(payload))
	if _, err := io.ReadFull(device, received); err != nil || !bytes.Equal(received, payload) {
		t.Fatal("standalone transfer failed", err)
	}
	device.Close()
	command.Process.Signal(syscall.SIGTERM)
	wait := make(chan error, 1)
	go func() { wait <- command.Wait() }()
	select {
	case err := <-wait:
		if err != nil {
			t.Fatal("SIGTERM exit failed", err)
		}
	case <-time.After(8 * time.Second):
		t.Fatal("standalone SIGTERM shutdown hung")
	}
}
