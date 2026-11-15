"""Back up and apply the gateway-only schema upgrade. Defaults to read-only preflight."""
import argparse
import datetime as dt
import gzip
import hashlib
import json
import os
from pathlib import Path
import re
import socket
import subprocess
import sys

import pymysql
import yaml

ROOT = Path(__file__).resolve().parents[2]
TABLES = ('fire_gateway', 'fire_host', 'fire_event', 'fire_protocol_message', 'sys_menu', 'sys_role_menu')


def snapshot(cur, table):
    cur.execute('SELECT COUNT(*) AS n FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=%s', (table,))
    if not cur.fetchone()['n']:
        return None
    cur.execute(f'SHOW CREATE TABLE `{table}`')
    ddl = cur.fetchone()['Create Table']
    cur.execute(f'SELECT * FROM `{table}`')
    return {'ddl': ddl, 'rows': cur.fetchall()}


def digest(rows):
    # Only the removed host pointer is excluded; every other historical field must match.
    encoded = [json.dumps({k: v for k, v in row.items() if k != 'host_id'},
                          sort_keys=True, ensure_ascii=False, default=str) for row in rows]
    return hashlib.sha256('\n'.join(sorted(encoded)).encode()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--config', type=Path, default=ROOT / 'RuoYi-Vue-master/ruoyi-admin/target/classes/application-druid.yml')
    args = parser.parse_args()
    master = yaml.safe_load(args.config.read_text('utf-8'))['spring']['datasource']['druid']['master']
    match = re.match(r'jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)', master['url'])
    if not match or match[1] not in ('localhost', '127.0.0.1') or match[3] != 'dachang':
        raise RuntimeError('This local migration tool only targets the configured local dachang database')
    db = pymysql.connect(host=match[1], port=int(match[2] or 3306), user=master['username'],
                         password=str(master['password']), database=match[3], charset='utf8mb4',
                         cursorclass=pymysql.cursors.DictCursor, connect_timeout=5, read_timeout=30)
    try:
        with db.cursor() as cur:
            saved = {table: snapshot(cur, table) for table in TABLES}
        report = {'database': match[3], 'apply': args.apply,
                  'before': {table: len(data['rows']) if data else None for table, data in saved.items()}}
        if not args.apply:
            print(json.dumps(report, ensure_ascii=False))
            return
        with socket.socket() as probe:
            probe.settimeout(1)
            if probe.connect_ex(('127.0.0.1', 2000)) == 0:
                raise RuntimeError('Stop the old local receiver before applying the schema upgrade')
        backup = ROOT / 'outputs/device-diagnostics/backups' / ('gateway-only-' + dt.datetime.now().strftime('%Y%m%d-%H%M%S-%f') + '.json.gz')
        backup.parent.mkdir(parents=True, exist_ok=True)
        with gzip.open(backup, 'wt', encoding='utf-8') as out:
            json.dump(saved, out, ensure_ascii=False, default=str)
        # Close the consistent read before the independent DDL client runs.
        db.commit()
        env = os.environ.copy()
        env['MYSQL_PWD'] = str(master['password'])
        client = Path(r'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe')
        result = subprocess.run([str(client), '--no-defaults', '--protocol=TCP', '--host='+match[1],
                                 '--port='+str(match[2] or 3306), '--user='+master['username'], '--default-character-set=utf8mb4'],
                                input=(ROOT/'SQL/fire_gateway_only_upgrade.sql').read_bytes(), env=env,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=60,
                                creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
        if result.returncode:
            raise RuntimeError('Migration failed; backup: '+str(backup)+'; '+result.stderr.decode(errors='replace'))
        with db.cursor() as cur:
            after = {table: snapshot(cur, table) for table in TABLES}
            cur.execute("SELECT COUNT(*) AS n FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fire_event' AND column_name='host_id'")
            assert cur.fetchone()['n'] == 0, 'Legacy event host pointer still exists'
        assert after['fire_host'] is None, 'Legacy host table still exists'
        for table in ('fire_gateway', 'fire_event', 'fire_protocol_message'):
            assert digest(saved[table]['rows']) == digest(after[table]['rows']), table+' history changed'
        def grants(data):
            menus = {row['menu_id']: row for row in data['sys_menu']['rows']}
            return {(row['role_id'], (menus[row['menu_id']]['perms'] or '').replace('fire:hosts:', 'fire:gateways:'))
                    for row in data['sys_role_menu']['rows'] if row['menu_id'] in menus}
        assert grants(saved) == grants(after), 'Existing role permissions changed'
        assert not any((row['perms'] or '').startswith('fire:hosts:') for row in after['sys_menu']['rows'])
        report.update(completed=True, backup=str(backup), historyPreserved=True, roleGrantsPreserved=True,
                      after={table: len(data['rows']) if data else None for table, data in after.items()})
        output = ROOT / 'outputs/device-diagnostics/gateway-only-migration.json'
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', 'utf-8')
        print(json.dumps(report, ensure_ascii=False))
    finally:
        db.close()


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    main()
