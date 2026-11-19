"""Local SSH direct-tcpip fixture for tests; never contacts the real server."""
import logging
import os
from pathlib import Path
import socket
import threading
import time
import subprocess

import paramiko

logging.getLogger("paramiko.transport").setLevel(logging.CRITICAL)


class LocalSSH:
    def __init__(self, directory: Path, receiver_port: int, reverse_port: int | None = None):
        self.directory = directory
        directory.mkdir()
        self.receiver_port = receiver_port
        self.client = paramiko.RSAKey.generate(2048)
        self.host = paramiko.RSAKey.generate(2048)
        self.key_path = directory / "client.key"
        self.client.write_private_key_file(str(self.key_path))
        if os.name == "nt":
            # The Codex host grants its helper group access to new files; real
            # Windows OpenSSH correctly refuses those ACLs for a private key.
            # Restrict only this disposable test key to the current identity.
            environment = dict(os.environ, FIRE_RELAY_TEST_KEY=str(self.key_path))
            secured = subprocess.run(["powershell.exe", "-NoProfile", "-Command",
                "$sid=[System.Security.Principal.WindowsIdentity]::GetCurrent().User; "
                "$acl=New-Object System.Security.AccessControl.FileSecurity; "
				"$acl.SetAccessRuleProtection($true,$false); "
                "$acl.SetOwner($sid); "
                "$rule=New-Object System.Security.AccessControl.FileSystemAccessRule($sid,'FullControl','Allow'); "
                "$acl.AddAccessRule($rule); [System.IO.File]::SetAccessControl($env:FIRE_RELAY_TEST_KEY,$acl)"],
                env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            if secured.returncode:
                raise RuntimeError(secured.stdout.decode(errors="replace"))
        self.listener = socket.socket()
        self.listener.bind(("127.0.0.1", 0))
        self.listener.listen()
        self.listener.settimeout(.2)
        self.port = self.listener.getsockname()[1]
        self.known_hosts = directory / "known_hosts"
        self.known_hosts.write_text(f"[127.0.0.1]:{self.port} {self.host.get_name()} {self.host.get_base64()}\n", encoding="ascii")
        self.closed = threading.Event()
        self.lock = threading.Lock()
        self.transports = []
        self.sockets = []
        self.channels_opened = 0
        self.reverse_port = reverse_port
        self.remote_listeners = []
        self.control_listener = None
        if reverse_port:
            self.control_listener = socket.socket()
            self.control_listener.bind(("127.0.0.1", 0))
            self.control_listener.listen()
            self.control_listener.settimeout(.2)
            self.control_port = self.control_listener.getsockname()[1]
            threading.Thread(target=self.control, daemon=True).start()
        self.thread = threading.Thread(target=self.accept, daemon=True)
        self.thread.start()

    def accept(self):
        while not self.closed.is_set():
            try:
                connection, _ = self.listener.accept()
            except socket.timeout:
                continue
            except OSError:
                return
            threading.Thread(target=self.session, args=(connection,), daemon=True).start()

    def session(self, connection):
        fixture = self

        class Server(paramiko.ServerInterface):
            def get_allowed_auths(self, username):
                return "publickey"

            def check_auth_publickey(self, username, key):
                if username == "fixture" and key == fixture.client:
                    return paramiko.AUTH_SUCCESSFUL
                return paramiko.AUTH_FAILED

            def check_channel_direct_tcpip_request(self, channel, origin, destination):
                if destination == ("127.0.0.1", fixture.receiver_port):
                    return paramiko.OPEN_SUCCEEDED
                return paramiko.OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED

            def check_port_forward_request(self, address, port):
                if address != "127.0.0.1" or port != fixture.reverse_port:
                    return False
                listener = socket.socket()
                try:
                    listener.bind((address, port))
                    listener.listen()
                    listener.settimeout(.2)
                except OSError:
                    listener.close()
                    return False
                with fixture.lock:
                    fixture.remote_listeners.append((transport, listener))
                threading.Thread(target=fixture.accept_remote, args=(transport, listener), daemon=True).start()
                return port

        transport = paramiko.Transport(connection)
        with self.lock:
            self.transports.append(transport)
        try:
            transport.add_server_key(self.host)
            transport.start_server(server=Server())
            while transport.is_active() and not self.closed.is_set():
                channel = transport.accept(.2)
                if channel is not None:
                    with self.lock:
                        self.channels_opened += 1
                    threading.Thread(target=self.forward, args=(channel,), daemon=True).start()
        except (OSError, EOFError, paramiko.SSHException):
            pass
        finally:
            transport.close()

    def accept_remote(self, transport, listener):
        try:
            while transport.is_active() and not self.closed.is_set():
                try:
                    stream, origin = listener.accept()
                except socket.timeout:
                    continue
                except OSError:
                    return
                with self.lock:
                    self.sockets.append(stream)
                def opened(stream=stream, origin=origin):
                    channel = None
                    try:
                        channel = transport.open_forwarded_tcpip_channel(origin, ("127.0.0.1", self.reverse_port))
                        self.bridge(channel, stream)
                    except (OSError, EOFError, paramiko.SSHException):
                        stream.close()
                        if channel:
                            channel.close()
                threading.Thread(target=opened, daemon=True).start()
        finally:
            listener.close()

    def bridge(self, channel, stream):
        def pump(source, destination):
            try:
                while not self.closed.is_set():
                    data = source.recv(8192)
                    if not data:
                        break
                    destination.sendall(data)
            except (OSError, EOFError):
                pass
            finally:
                channel.close()
                stream.close()
        thread = threading.Thread(target=pump, args=(channel, stream), daemon=True)
        thread.start()
        pump(stream, channel)
        thread.join(timeout=3)

    def drop_remote(self):
        with self.lock:
            listeners = list(self.remote_listeners)
        for transport, listener in listeners:
            listener.close()
            transport.close()

    def control(self):
        while not self.closed.is_set():
            try:
                stream, _ = self.control_listener.accept()
            except socket.timeout:
                continue
            except OSError:
                return
            with stream:
                stream.settimeout(2)
                if stream.recv(32).startswith(b"DROP"):
                    self.drop_remote()
                    stream.sendall(b"1")

    def forward(self, channel):
        upstream = None
        try:
            upstream = socket.create_connection(("127.0.0.1", self.receiver_port), timeout=2)
            upstream.settimeout(None)
            with self.lock:
                self.sockets.append(upstream)

            def pump(source, destination):
                try:
                    while not self.closed.is_set():
                        value = source.recv(8192)
                        if not value:
                            break
                        destination.sendall(value)
                except (OSError, EOFError):
                    pass
                finally:
                    channel.close()
                    upstream.close()

            upload = threading.Thread(target=pump, args=(channel, upstream), daemon=True)
            upload.start()
            pump(upstream, channel)
            upload.join(timeout=3)
        except OSError:
            channel.close()
            if upstream:
                upstream.close()

    def close(self):
        self.closed.set()
        self.listener.close()
        self.drop_remote()
        if self.control_listener:
            self.control_listener.close()
        with self.lock:
            for transport in self.transports:
                transport.close()
            for stream in self.sockets:
                stream.close()
        self.thread.join(timeout=3)


def verify_switching(fixture_module, executable, forwarder, config, environment, receiver_port, work, sql):
    import json

    check, run = fixture_module.check, fixture_module.run
    ssh = LocalSSH(work / "ssh-fixture", receiver_port)
    app = None
    output = (work / "ssh-relay.log").open("wb")
    try:
        development = {key: config[key] for key in ("node_id", "target", "ssh")}
        production = json.loads(json.dumps(development))
        production["ssh"] = {"enabled": True, "host": "127.0.0.1", "port": ssh.port, "username": "fixture",
                             "private_key_file": str(ssh.key_path), "known_hosts_file": str(ssh.known_hosts),
                             "local_port": fixture_module.port(), "remote_address": "127.0.0.1", "remote_port": receiver_port}
        switched = {key: value for key, value in config.items() if key not in ("node_id", "target", "ssh")}
        switched.update({"active_target": "development", "destinations": {"development": development, "production": production}})
        path = forwarder / "config.json"
        path.write_text(json.dumps(switched), encoding="utf-8")
        environment = dict(environment)
        environment["PATH"] = str(Path(os.environ["SystemRoot"]) / "System32/OpenSSH") + os.pathsep + environment["PATH"]
        before = sql("USE dachang;SELECT COUNT(*) FROM fire_protocol_message;")
        probe = run([executable, "--target", "production", "--probe"], cwd=work, env=environment, timeout=35)
        (work / "ssh-probe.log").write_bytes(probe.stdout)
        check(probe.returncode == 0 and b"PROBE_OK target=production" in probe.stdout,
              "production profile passes real OpenSSH forwarding and Java TLS node authentication")
        check(sql("USE dachang;SELECT COUNT(*) FROM fire_protocol_message;") == before,
              "production SSH probe does not write business messages")
        check(json.loads(path.read_text())["active_target"] == "development",
              "production probe leaves selected development target unchanged")
        app = subprocess.Popen([str(executable), "--target", "production", "--headless"], cwd=work,
                               env=environment, stdout=output, stderr=output, creationflags=fixture_module.FLAGS)
        device = None
        for _ in range(150):
            if ssh.channels_opened >= 2:
                try:
                    device = socket.create_connection(("127.0.0.1", config["listen"]["port"]), timeout=.1)
                    break
                except OSError:
                    pass
            if app.poll() is not None:
                break
            time.sleep(.05)
        check(device is not None, "production profile starts listener while SSH tunnel is active")
        with device:
            device.settimeout(8)
            device.sendall(fixture_module.frame(706, True))
            fixture_module.ack(device, 706)
        check(sql("USE dachang;SELECT COUNT(*) FROM fire_protocol_message WHERE sequence_no=706 AND direction='RX' AND parse_status<>'RAW_CHUNK';") == "1",
              "production profile forwards GB26875 through OpenSSH into actual Java database ingestion")
        check(ssh.channels_opened >= 3, "health and device connections both traverse the SSH forwarding server")
    finally:
        if app and app.poll() is None:
            app.terminate()
            app.wait(timeout=5)
        ssh.close()
        output.close()
