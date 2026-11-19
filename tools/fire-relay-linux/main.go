// FireControl Linux relay: native executable, no Java/Python or shared C runtime.
package main

import (
	"bufio"
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"crypto/tls"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"os/exec"
	"os/signal"
	"path/filepath"
	"regexp"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"syscall"
	"time"
)

const version = "1.2.0-linux"
const magic = "FCR1"

type endpoint struct {
	Address string `json:"address"`
	Port    int    `json:"port"`
}

type targetConfig struct {
	Address     string `json:"address"`
	Port        int    `json:"port"`
	Fingerprint string `json:"certificate_sha256"`
	SecretFile  string `json:"secret_file"`
}

type sshConfig struct {
	Enabled       bool   `json:"enabled"`
	Host          string `json:"host"`
	Port          int    `json:"port"`
	Username      string `json:"username"`
	PrivateKey    string `json:"private_key_file"`
	KnownHosts    string `json:"known_hosts_file"`
	LocalPort     int    `json:"local_port"`
	RemoteAddress string `json:"remote_address"`
	RemotePort    int    `json:"remote_port"`
}

type destinationConfig struct {
	Node     string         `json:"node_id"`
	Target   targetConfig   `json:"target"`
	SSH      sshConfig      `json:"ssh"`
	Intercom intercomConfig `json:"intercom,omitempty"`
}

type config struct {
	Node           string                       `json:"node_id"`
	Listen         endpoint                     `json:"listen"`
	Target         targetConfig                 `json:"target"`
	Maximum        int                          `json:"max_connections"`
	ConnectSeconds float64                      `json:"connect_timeout_seconds"`
	IdleSeconds    int                          `json:"idle_seconds"`
	RetrySeconds   float64                      `json:"reconnect_seconds"`
	LogDirectory   string                       `json:"log_directory"`
	SSH            sshConfig                    `json:"ssh"`
	ActiveTarget   string                       `json:"active_target,omitempty"`
	Destinations   map[string]destinationConfig `json:"destinations,omitempty"`
	Intercom       intercomConfig               `json:"intercom,omitempty"`
	secret         []byte
	pin            []byte
}

func relative(base, path string) string {
	if filepath.IsAbs(path) {
		return filepath.Clean(path)
	}
	return filepath.Join(base, path)
}

func loadConfig(path string) (*config, error) {
	return loadSelectedConfig(path, "")
}

func loadSelectedConfig(path, selected string) (*config, error) {
	path, err := filepath.Abs(path)
	if err != nil {
		return nil, err
	}
	content, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("读取同目录配置失败：%w", err)
	}
	content = bytes.TrimPrefix(content, []byte{0xef, 0xbb, 0xbf})
	cfg := &config{Node: "site-01", Maximum: 64, ConnectSeconds: 5, IdleSeconds: 600,
		RetrySeconds: 5, LogDirectory: "logs", SSH: sshConfig{Port: 18881, LocalPort: 22100, RemoteAddress: "127.0.0.1", RemotePort: 22000}}
	decoder := json.NewDecoder(bytes.NewReader(content))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(cfg); err != nil {
		return nil, fmt.Errorf("配置格式错误：%w", err)
	}
	if err := decoder.Decode(new(any)); err != io.EOF {
		return nil, errors.New("配置文件只能包含一个JSON对象")
	}
	if selected != "" {
		cfg.ActiveTarget = selected
	}
	if len(cfg.Destinations) > 0 {
		if len(cfg.Destinations) > 8 {
			return nil, errors.New("最多配置8个转发目标")
		}
		for name := range cfg.Destinations {
			if !regexp.MustCompile(`^[A-Za-z0-9_.-]{1,32}$`).MatchString(name) {
				return nil, errors.New("目标名称格式无效")
			}
		}
		destination, found := cfg.Destinations[cfg.ActiveTarget]
		if !found {
			return nil, fmt.Errorf("目标 %q 不存在，请核对 active_target", cfg.ActiveTarget)
		}
		cfg.Node, cfg.Target, cfg.SSH = destination.Node, destination.Target, destination.SSH
		cfg.Intercom = destination.Intercom
		if cfg.Node == "" {
			cfg.Node = "site-01"
		}
		if cfg.SSH.Port == 0 {
			cfg.SSH.Port = 18881
		}
		if cfg.SSH.LocalPort == 0 {
			cfg.SSH.LocalPort = 22100
		}
		if cfg.SSH.RemotePort == 0 {
			cfg.SSH.RemotePort = 22000
		}
		if cfg.SSH.RemoteAddress == "" {
			cfg.SSH.RemoteAddress = "127.0.0.1"
		}
	} else if selected != "" || cfg.ActiveTarget != "" {
		return nil, errors.New("当前为旧版单目标配置，没有 destinations 可供切换")
	}
	if !regexp.MustCompile(`^[A-Za-z0-9_.-]{1,64}$`).MatchString(cfg.Node) {
		return nil, errors.New("node_id 格式无效")
	}
	if net.ParseIP(cfg.Listen.Address).To4() == nil {
		return nil, errors.New("listen.address 必须为本机IPv4地址")
	}
	validPort := func(v int) bool { return v >= 1 && v <= 65535 }
	if !validPort(cfg.Listen.Port) || !validPort(cfg.Target.Port) || strings.TrimSpace(cfg.Target.Address) == "" {
		return nil, errors.New("监听或目标地址/端口无效")
	}
	if cfg.Maximum < 1 || cfg.Maximum > 256 || !(cfg.ConnectSeconds >= 1 && cfg.ConnectSeconds <= 30) ||
		cfg.IdleSeconds < 5 || cfg.IdleSeconds > 86400 || !(cfg.RetrySeconds >= 1 && cfg.RetrySeconds <= 300) {
		return nil, errors.New("连接数或超时参数超出允许范围")
	}
	cfg.Target.Fingerprint = strings.ToLower(strings.ReplaceAll(cfg.Target.Fingerprint, ":", ""))
	cfg.pin, err = hex.DecodeString(cfg.Target.Fingerprint)
	if err != nil || len(cfg.pin) != sha256.Size {
		return nil, errors.New("接收端证书SHA256指纹必须为64个十六进制字符")
	}
	base := filepath.Dir(path)
	if cfg.Target.SecretFile == "" {
		return nil, errors.New("缺少 target.secret_file")
	}
	cfg.Target.SecretFile = relative(base, cfg.Target.SecretFile)
	cfg.secret, err = os.ReadFile(cfg.Target.SecretFile)
	if err != nil {
		return nil, fmt.Errorf("读取节点密钥失败：%w", err)
	}
	cfg.secret = bytes.TrimSpace(cfg.secret)
	if len(cfg.secret) < 32 || len(cfg.secret) > 1024 {
		return nil, errors.New("节点密钥必须为32到1024字节")
	}
	cfg.LogDirectory = relative(base, cfg.LogDirectory)
	if cfg.SSH.Enabled {
		if !regexp.MustCompile(`^[A-Za-z0-9_][A-Za-z0-9_.-]*$`).MatchString(cfg.SSH.Username) ||
			!regexp.MustCompile(`^[A-Za-z0-9_][A-Za-z0-9_.-]*$`).MatchString(cfg.SSH.Host) ||
			!validPort(cfg.SSH.Port) || !validPort(cfg.SSH.LocalPort) || !validPort(cfg.SSH.RemotePort) ||
			net.ParseIP(cfg.SSH.RemoteAddress).To4() == nil {
			return nil, errors.New("SSH地址、账号或端口参数无效")
		}
		for _, item := range []*string{&cfg.SSH.PrivateKey, &cfg.SSH.KnownHosts} {
			if *item == "" {
				return nil, errors.New("SSH密钥及known_hosts路径不能为空")
			}
			*item = relative(base, *item)
			if info, err := os.Stat(*item); err != nil || !info.Mode().IsRegular() {
				return nil, errors.New("找不到SSH密钥或known_hosts文件")
			}
		}
	}
	if err := cfg.validateIntercom(base); err != nil {
		return nil, err
	}
	return cfg, nil
}

func defaultConfig() string {
	executable, err := os.Executable()
	if err != nil {
		return "config.json"
	}
	executable, err = filepath.EvalSymlinks(executable)
	if err != nil {
		return "config.json"
	}
	return filepath.Join(filepath.Dir(executable), "config.json")
}

func field(buffer *bytes.Buffer, text string) {
	binary.Write(buffer, binary.BigEndian, uint16(len(text)))
	buffer.WriteString(text)
}

func randomUUID() (string, error) {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	b[6] = (b[6] & 15) | 64
	b[8] = (b[8] & 63) | 128
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[:4], b[4:6], b[6:8], b[8:10], b[10:]), nil
}

func connectTarget(ctx context.Context, cfg *config, source string, port int) (net.Conn, error) {
	address := net.JoinHostPort(cfg.Target.Address, strconv.Itoa(cfg.Target.Port))
	if cfg.SSH.Enabled {
		address = net.JoinHostPort("127.0.0.1", strconv.Itoa(cfg.SSH.LocalPort))
	}
	timeout := time.Duration(cfg.ConnectSeconds * float64(time.Second))
	connectCtx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()
	dialer := tls.Dialer{NetDialer: &net.Dialer{Timeout: timeout, KeepAlive: 30 * time.Second}, Config: &tls.Config{
		MinVersion: tls.VersionTLS12,
		// A mandatory, constant-time certificate pin replaces CA/hostname validation.
		InsecureSkipVerify: true,
		VerifyConnection: func(state tls.ConnectionState) error {
			if len(state.PeerCertificates) == 0 {
				return errors.New("接收端未提供证书")
			}
			actual := sha256.Sum256(state.PeerCertificates[0].Raw)
			if !hmac.Equal(actual[:], cfg.pin) {
				return errors.New("接收端证书指纹不匹配，拒绝发送数据")
			}
			return nil
		},
	}}
	conn, err := dialer.DialContext(connectCtx, "tcp4", address)
	if err != nil {
		return nil, err
	}
	success := false
	defer func() {
		if !success {
			conn.Close()
		}
	}()
	conn.SetDeadline(time.Now().Add(timeout))
	challenge := make([]byte, 36)
	if _, err = io.ReadFull(conn, challenge); err != nil {
		return nil, err
	}
	if string(challenge[:4]) != magic {
		return nil, errors.New("目标不是兼容的FireControl转发接收端")
	}
	id, err := randomUUID()
	if err != nil {
		return nil, err
	}
	var header bytes.Buffer
	if source == "" {
		header.WriteByte(0)
	} else {
		header.WriteByte(1)
	}
	field(&header, cfg.Node)
	field(&header, source)
	binary.Write(&header, binary.BigEndian, uint16(port))
	field(&header, id)
	signature := hmac.New(sha256.New, cfg.secret)
	signature.Write(challenge)
	signature.Write(header.Bytes())
	packet := append(header.Bytes(), signature.Sum(nil)...)
	if err = writeAll(conn, packet); err != nil {
		return nil, err
	}
	response := make([]byte, 1)
	if _, err = io.ReadFull(conn, response); err != nil {
		return nil, errors.New("接收端拒绝连接，请核对节点密钥、node_id及允许的设备网段")
	}
	if response[0] != 1 {
		return nil, errors.New("接收端拒绝节点身份或设备来源")
	}
	conn.SetDeadline(time.Time{})
	success = true
	return conn, nil
}

func writeAll(destination io.Writer, data []byte) error {
	for len(data) > 0 {
		n, err := destination.Write(data)
		if err != nil {
			return err
		}
		if n == 0 {
			return io.ErrNoProgress
		}
		data = data[n:]
	}
	return nil
}

type rollingLog struct {
	mu   sync.Mutex
	path string
	file *os.File
	size int64
}

func openLog(directory string) (*rollingLog, error) {
	if err := os.MkdirAll(directory, 0700); err != nil {
		return nil, err
	}
	writer := &rollingLog{path: filepath.Join(directory, "relay.log")}
	return writer, writer.open()
}

func (w *rollingLog) open() error {
	f, err := os.OpenFile(w.path, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0600)
	if err != nil {
		return err
	}
	info, err := f.Stat()
	if err != nil {
		f.Close()
		return err
	}
	w.file = f
	w.size = info.Size()
	return nil
}

func (w *rollingLog) Write(data []byte) (int, error) {
	w.mu.Lock()
	defer w.mu.Unlock()
	if w.size+int64(len(data)) > 5*1024*1024 {
		if err := w.file.Close(); err != nil {
			return 0, err
		}
		if err := os.Remove(w.path + ".5"); err != nil && !os.IsNotExist(err) {
			w.open()
			return 0, err
		}
		for n := 4; n >= 1; n-- {
			if err := os.Rename(fmt.Sprintf("%s.%d", w.path, n), fmt.Sprintf("%s.%d", w.path, n+1)); err != nil && !os.IsNotExist(err) {
				w.open()
				return 0, err
			}
		}
		if err := os.Rename(w.path, w.path+".1"); err != nil {
			w.open()
			return 0, err
		}
		if err := w.open(); err != nil {
			return 0, err
		}
	}
	n, err := w.file.Write(data)
	w.size += int64(n)
	return n, err
}

func (w *rollingLog) Close() error { w.mu.Lock(); defer w.mu.Unlock(); return w.file.Close() }

type relayApp struct {
	cfg        *config
	logger     *log.Logger
	mu         sync.Mutex
	sockets    map[net.Conn]struct{}
	wg         sync.WaitGroup
	slots      chan struct{}
	active     atomic.Int64
	uploaded   atomic.Int64
	downloaded atomic.Int64
}

func newApp(cfg *config, logger *log.Logger) *relayApp {
	return &relayApp{cfg: cfg, logger: logger, sockets: make(map[net.Conn]struct{}), slots: make(chan struct{}, cfg.Maximum)}
}

func (app *relayApp) track(conn net.Conn, add bool) {
	app.mu.Lock()
	defer app.mu.Unlock()
	if add {
		app.sockets[conn] = struct{}{}
	} else {
		delete(app.sockets, conn)
	}
}

func pause(ctx context.Context, seconds float64) bool {
	timer := time.NewTimer(time.Duration(seconds * float64(time.Second)))
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return false
	case <-timer.C:
		return true
	}
}

func (app *relayApp) health(ctx context.Context) {
	previous := -1
	for ctx.Err() == nil {
		conn, err := connectTarget(ctx, app.cfg, "", 0)
		state := 0
		if err == nil {
			state = 1
			conn.Close()
		}
		if state != previous && ctx.Err() == nil {
			if state == 1 {
				app.logger.Print("接收端证书与节点身份认证成功")
			} else {
				app.logger.Printf("接收端暂不可用：%v；继续探测，设备连接失败时等待设备重连", err)
			}
			previous = state
		}
		if !pause(ctx, app.cfg.RetrySeconds) {
			return
		}
	}
}

func (app *relayApp) sshTunnel(ctx context.Context) {
	cfg := app.cfg.SSH
	args := sshArguments(cfg, cfg.PrivateKey)
	args = append(args, "-L", fmt.Sprintf("127.0.0.1:%d:%s:%d", cfg.LocalPort, cfg.RemoteAddress, cfg.RemotePort), cfg.Username+"@"+cfg.Host)
	app.runSSH(ctx, "用传", args)
}

func sshArguments(cfg sshConfig, key string) []string {
	return []string{"-F", os.DevNull, "-N", "-T", "-p", strconv.Itoa(cfg.Port), "-i", key,
		"-o", "BatchMode=yes", "-o", "IdentitiesOnly=yes", "-o", "StrictHostKeyChecking=yes",
		"-o", `UserKnownHostsFile="` + cfg.KnownHosts + `"`, "-o", "GlobalKnownHostsFile=" + os.DevNull,
		"-o", "ExitOnForwardFailure=yes", "-o", "ServerAliveInterval=15", "-o", "ServerAliveCountMax=3", "-o", "ConnectTimeout=5"}
}

func (app *relayApp) runSSH(ctx context.Context, purpose string, args []string) {
	for ctx.Err() == nil {
		command := exec.CommandContext(ctx, "ssh", args...)
		errors, err := command.StderrPipe()
		if err == nil {
			err = command.Start()
		}
		if err == nil {
			app.logger.Printf("%s SSH进程已启动，目标 %s:%d；通道状态请结合探测结果确认", purpose, app.cfg.SSH.Host, app.cfg.SSH.Port)
			scanner := bufio.NewScanner(errors)
			scanner.Buffer(make([]byte, 1024), 8192)
			for scanner.Scan() {
				text := scanner.Text()
				if len(text) > 500 {
					text = text[:500]
				}
				app.logger.Printf("%s SSH: %s", purpose, text)
			}
			err = command.Wait()
		}
		if ctx.Err() == nil {
			app.logger.Printf("%s SSH通道已退出：%v", purpose, err)
		}
		if !pause(ctx, app.cfg.RetrySeconds) {
			return
		}
	}
}

type copyResult struct {
	direction int
	err       error
}

func (app *relayApp) session(ctx context.Context, device net.Conn) {
	defer app.wg.Done()
	defer func() { device.Close(); app.track(device, false); <-app.slots }()
	ip, portText, err := net.SplitHostPort(device.RemoteAddr().String())
	if err != nil {
		return
	}
	port, err := strconv.Atoi(portText)
	if err != nil {
		return
	}
	upstream, err := connectTarget(ctx, app.cfg, ip, port)
	if err != nil {
		if ctx.Err() == nil {
			app.logger.Printf("设备 %s 暂未接入：%v；等待设备重连", ip, err)
		}
		return
	}
	app.track(upstream, true)
	defer func() { upstream.Close(); app.track(upstream, false) }()
	active := app.active.Add(1)
	defer app.active.Add(-1)
	app.logger.Printf("设备 %s 已接入，当前连接=%d", ip, active)
	var last atomic.Int64
	last.Store(time.Now().UnixNano())
	var upload, download atomic.Int64
	done := make(chan copyResult, 2)
	copyStream := func(source, destination net.Conn, direction int) {
		buffer := make([]byte, 8192)
		for ctx.Err() == nil {
			source.SetReadDeadline(time.Now().Add(time.Second))
			n, readErr := source.Read(buffer)
			if n > 0 {
				destination.SetWriteDeadline(time.Now().Add(5 * time.Second))
				writeErr := writeAll(destination, buffer[:n])
				if writeErr != nil {
					done <- copyResult{direction, writeErr}
					return
				}
				if direction == 0 {
					upload.Add(int64(n))
					app.uploaded.Add(int64(n))
				} else {
					download.Add(int64(n))
					app.downloaded.Add(int64(n))
				}
				last.Store(time.Now().UnixNano())
			}
			if readErr != nil {
				if timeout, ok := readErr.(net.Error); ok && timeout.Timeout() {
					if time.Since(time.Unix(0, last.Load())) < time.Duration(app.cfg.IdleSeconds)*time.Second {
						continue
					}
				}
				done <- copyResult{direction, readErr}
				return
			}
		}
		done <- copyResult{direction, ctx.Err()}
	}
	go copyStream(device, upstream, 0)
	go copyStream(upstream, device, 1)
	var first copyResult
	consumed := 0
	select {
	case first = <-done:
		consumed++
	case <-ctx.Done():
	}
	if first.direction == 0 && errors.Is(first.err, io.EOF) && ctx.Err() == nil {
		// Drain any confirmations after an orderly device-side half-close.
		timer := time.NewTimer(time.Duration(app.cfg.ConnectSeconds * float64(time.Second)))
		select {
		case <-done:
			consumed++
		case <-timer.C:
		case <-ctx.Done():
		}
		timer.Stop()
	}
	device.Close()
	upstream.Close()
	for consumed < 2 {
		<-done
		consumed++
	}
	app.logger.Printf("设备 %s 会话结束：已转发上行=%d字节，下行=%d字节", ip, upload.Load(), download.Load())
}

func (app *relayApp) serve(ctx context.Context, listener net.Listener) {
	ctx, cancel := context.WithCancel(ctx)
	defer cancel()
	go func() {
		<-ctx.Done()
		listener.Close()
		app.mu.Lock()
		defer app.mu.Unlock()
		for conn := range app.sockets {
			conn.Close()
		}
	}()
	app.wg.Add(1)
	go func() { defer app.wg.Done(); app.health(ctx) }()
	if app.cfg.SSH.Enabled {
		app.wg.Add(1)
		go func() { defer app.wg.Done(); app.sshTunnel(ctx) }()
	}
	if app.cfg.Intercom.Enabled {
		app.wg.Add(1)
		go func() { defer app.wg.Done(); app.intercomTunnel(ctx) }()
	}
	app.logger.Printf("FireRelay Linux 已启动：%s → %s:%d，节点=%s", listener.Addr(), app.cfg.Target.Address, app.cfg.Target.Port, app.cfg.Node)
	for ctx.Err() == nil {
		conn, err := listener.Accept()
		if err != nil {
			if ctx.Err() == nil {
				app.logger.Printf("监听异常：%v", err)
			}
			break
		}
		select {
		case app.slots <- struct{}{}:
			app.track(conn, true)
			app.wg.Add(1)
			go app.session(ctx, conn)
		default:
			app.logger.Printf("设备连接数已满，拒绝 %s", conn.RemoteAddr())
			conn.Close()
		}
	}
	cancel()
	app.wg.Wait()
}

func execute() int {
	flags := flag.NewFlagSet("fire-relay", flag.ContinueOnError)
	path := flags.String("config", defaultConfig(), "配置文件，默认与可执行文件同目录的config.json")
	check := flags.Bool("check-config", false, "检查配置，不监听设备")
	probe := flags.Bool("probe", false, "验证接收端TLS与节点身份")
	probeCameras := flags.Bool("probe-cameras", false, "只探测麒麟到摄像机SDK端口的TCP连接，不代表语音已验证")
	headless := flags.Bool("headless", false, "后台运行，日志只写文件")
	showVersion := flags.Bool("version", false, "显示版本与架构")
	target := flags.String("target", "", "本次运行使用的目标，如 development 或 production")
	selectTarget := flags.String("select-target", "", "验证并保存 active_target；不启动转发")
	importPath := flags.String("import-targets", "", "从模板补充目标配置，保留当前配置及目标；不启动转发")
	if err := flags.Parse(os.Args[1:]); err != nil {
		return 2
	}
	if *showVersion {
		fmt.Printf("FireRelay %s %s/%s\n", version, runtime.GOOS, runtime.GOARCH)
		return 0
	}
	if (*target != "" && *selectTarget != "") || (*importPath != "" && (*target != "" || *selectTarget != "")) {
		fmt.Fprintln(os.Stderr, "--target 与 --select-target 不能同时使用")
		return 2
	}
	selected := *target
	if *selectTarget != "" {
		selected = *selectTarget
	}
	cfg, err := loadSelectedConfig(*path, selected)
	if err != nil {
		fmt.Fprintln(os.Stderr, "配置错误：", err)
		return 1
	}
	if *importPath != "" {
		if err := importTargets(*path, *importPath); err != nil {
			fmt.Fprintln(os.Stderr, "补充目标配置失败：", err)
			return 1
		}
		fmt.Println("TARGETS_IMPORTED 已保留原目标及监听配置，并补充模板中的缺失目标")
		return 0
	}
	if *selectTarget != "" {
		if err := saveActiveTarget(*path, *selectTarget); err != nil {
			fmt.Fprintln(os.Stderr, "保存目标失败：", err)
			return 1
		}
		fmt.Printf("TARGET_SAVED target=%s；重启正在运行的转发程序后生效\n", cfg.ActiveTarget)
		return 0
	}
	if *check {
		fmt.Printf("CONFIG_OK active_target=%s node=%s listen=%s:%d target=%s:%d ssh=%v\n", cfg.ActiveTarget, cfg.Node, cfg.Listen.Address, cfg.Listen.Port, cfg.Target.Address, cfg.Target.Port, cfg.SSH.Enabled)
		return 0
	}
	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()
	if *probeCameras {
		if err := probeCamerasTCP(ctx, cfg, log.New(os.Stdout, "", log.Ldate|log.Ltime)); err != nil {
			fmt.Fprintln(os.Stderr, "CAMERA_TCP_FAILED", err)
			return 1
		}
		return 0
	}
	if *probe {
		logger := log.New(os.Stdout, "", log.Ldate|log.Ltime|log.Lmicroseconds)
		if err := probeTarget(ctx, cfg, logger); err != nil {
			logger.Printf("接收端探测失败 target=%s：%v", cfg.ActiveTarget, err)
			return 1
		}
		logger.Printf("PROBE_OK target=%s 接收端证书和节点身份认证成功", cfg.ActiveTarget)
		return 0
	}
	writer, err := openLog(cfg.LogDirectory)
	if err != nil {
		fmt.Fprintln(os.Stderr, "日志目录不可写：", err)
		return 1
	}
	defer writer.Close()
	var destination io.Writer = writer
	if !*headless {
		destination = io.MultiWriter(os.Stdout, writer)
	}
	logger := log.New(destination, "", log.Ldate|log.Ltime|log.Lmicroseconds)
	listener, err := net.Listen("tcp4", net.JoinHostPort(cfg.Listen.Address, strconv.Itoa(cfg.Listen.Port)))
	if err != nil {
		logger.Printf("监听失败，请核对本机IP与端口占用：%v", err)
		return 1
	}
	defer listener.Close()
	newApp(cfg, logger).serve(ctx, listener)
	return 0
}

func main() { os.Exit(execute()) }
