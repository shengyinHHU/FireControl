package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"net"
	"os"
	"regexp"
	"strconv"
	"time"
)

// Independent SSH process: a denied camera listener must not stop fire reports.
type intercomConfig struct {
	Enabled    bool           `json:"enabled"`
	PrivateKey string         `json:"private_key_file"`
	Cameras    []cameraTunnel `json:"cameras"`
}

type cameraTunnel struct {
	Name       string `json:"name"`
	Address    string `json:"device_address"`
	Port       int    `json:"device_port"`
	ServerPort int    `json:"server_port"`
}

func privateIPv4(address string) bool {
	ip := net.ParseIP(address).To4()
	return ip != nil && (ip[0] == 10 || ip[0] == 172 && ip[1] >= 16 && ip[1] <= 31 || ip[0] == 192 && ip[1] == 168)
}

func (cfg *config) validateIntercom(base string) error {
	if !cfg.Intercom.Enabled {
		return nil
	}
	if !cfg.SSH.Enabled {
		return errors.New("启用对讲通道需要当前目标的SSH配置")
	}
	if len(cfg.Intercom.Cameras) < 1 || len(cfg.Intercom.Cameras) > 8 {
		return errors.New("对讲通道需配置1至8台摄像机")
	}
	if cfg.Intercom.PrivateKey == "" {
		return errors.New("缺少对讲专用SSH私钥")
	}
	cfg.Intercom.PrivateKey = relative(base, cfg.Intercom.PrivateKey)
	if info, err := os.Stat(cfg.Intercom.PrivateKey); err != nil || !info.Mode().IsRegular() {
		return errors.New("找不到对讲专用SSH私钥")
	}
	names, ports, devices := map[string]bool{}, map[int]bool{}, map[string]bool{}
	for _, camera := range cfg.Intercom.Cameras {
		if !regexp.MustCompile(`^[A-Za-z0-9_.-]{1,32}$`).MatchString(camera.Name) || names[camera.Name] || !privateIPv4(camera.Address) ||
			camera.Port < 1 || camera.Port > 65535 || camera.ServerPort < 1024 || camera.ServerPort > 65535 || ports[camera.ServerPort] ||
			camera.ServerPort == cfg.SSH.RemotePort {
			return errors.New("摄像机名称、专网IPv4或端口无效/重复；服务器对讲端口不能占用接收端口")
		}
		device := net.JoinHostPort(camera.Address, strconv.Itoa(camera.Port))
		if devices[device] {
			return errors.New("同一摄像机SDK地址不能重复映射")
		}
		names[camera.Name], ports[camera.ServerPort], devices[device] = true, true, true
	}
	return nil
}

func (cfg *config) intercomArguments() []string {
	args := sshArguments(cfg.SSH, cfg.Intercom.PrivateKey)
	for _, camera := range cfg.Intercom.Cameras {
		args = append(args, "-R", fmt.Sprintf("127.0.0.1:%d:%s:%d", camera.ServerPort, camera.Address, camera.Port))
	}
	return append(args, cfg.SSH.Username+"@"+cfg.SSH.Host)
}

func (app *relayApp) intercomTunnel(ctx context.Context) {
	for _, camera := range app.cfg.Intercom.Cameras {
		app.logger.Printf("对讲映射 %s：服务器127.0.0.1:%d → 麒麟 → %s:%d/TCP", camera.Name, camera.ServerPort, camera.Address, camera.Port)
	}
	app.runSSH(ctx, "对讲", app.cfg.intercomArguments())
}

func probeCamerasTCP(ctx context.Context, cfg *config, logger *log.Logger) error {
	if !cfg.Intercom.Enabled {
		return errors.New("当前目标未启用intercom，请选择已配置对讲的目标")
	}
	var failures []error
	for _, camera := range cfg.Intercom.Cameras {
		address := net.JoinHostPort(camera.Address, strconv.Itoa(camera.Port))
		connection, err := (&net.Dialer{Timeout: time.Duration(cfg.ConnectSeconds * float64(time.Second))}).DialContext(ctx, "tcp4", address)
		if err != nil {
			failures = append(failures, fmt.Errorf("%s %s: %w", camera.Name, address, err))
			continue
		}
		connection.Close()
		logger.Printf("CAMERA_TCP_OK %s %s；仅表示麒麟到摄像机端口可达，服务器SDK登录及双向声音需现场测试", camera.Name, address)
	}
	return errors.Join(failures...)
}
