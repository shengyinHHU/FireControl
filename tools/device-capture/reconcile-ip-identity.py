"""Back up and reconcile gateway identity using recorded source IPs. Never sends network/device commands."""
import argparse
import datetime as dt
import gzip
import hashlib
import json
from pathlib import Path
import re
import sys

import pymysql
import yaml

ROOT = Path(__file__).resolve().parents[2]
TABLES = ('fire_gateway', 'fire_host', 'fire_protocol_message', 'fire_event')


def reconcile(db, apply):
    with db.cursor() as cur:
        cur.execute('SELECT * FROM fire_gateway ORDER BY gateway_id')
        gateways = cur.fetchall()
        by_ip = {}
        for row in gateways:
            if row['allowed_ip']:
                if row['allowed_ip'] in by_ip:
                    raise RuntimeError('Duplicate inventory IP; reconciliation stopped')
                by_ip[row['allowed_ip']] = row
        cur.execute("SELECT e.*,m.peer_ip FROM fire_event e JOIN fire_protocol_message m ON m.message_id=e.message_id ORDER BY e.event_id")
        events = cur.fetchall()
        ids = set()
        planned = []
        for event in events:
            raw = json.loads(event['details_json']).get('raw')
            if not raw:
                raise RuntimeError('Event lacks original object bytes; reconciliation stopped')
            identity = f"{event['peer_ip']}|{event['object_type']}|{raw}"
            event_id = hashlib.sha256(identity.encode()).hexdigest()
            if event_id in ids:
                raise RuntimeError('Historical events collide under IP identity; preserve and review manually')
            ids.add(event_id)
            gateway = by_ip.get(event['peer_ip'])
            gateway_id = gateway['gateway_id'] if gateway else event['gateway_id']
            if event['host_id'] and gateway_id != event['gateway_id']:
                raise RuntimeError('Historical host ownership requires manual review; no records changed')
            planned.append((event_id, gateway_id, event['event_id']))
        cur.execute("""SELECT peer_ip,MAX(message_id) AS messageId FROM fire_protocol_message
            WHERE direction='RX' AND parse_status IN ('PARSED','EMPTY','UNSUPPORTED','MALFORMED')
            AND LEFT(raw_hex,4)='4040' AND SUBSTRING(raw_hex,9,2)='01'
            AND SUBSTRING(raw_hex,53,2)='02' GROUP BY peer_ip""")
        observations = cur.fetchall()
        report = {'apply': apply, 'inventoryIPs': len(by_ip), 'historicalEvents': len(events), 'observedIPs': len(observations)}
        if not apply:
            return report

        stamp = dt.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
        backup = ROOT / 'outputs/device-diagnostics/backups' / f'ip-identity-{stamp}.json.gz'
        backup.parent.mkdir(parents=True, exist_ok=True)
        snapshot = {}
        for table in TABLES:
            cur.execute(f'SHOW CREATE TABLE `{table}`')
            ddl = cur.fetchone()['Create Table']
            cur.execute(f'SELECT * FROM `{table}`')
            snapshot[table] = {'ddl': ddl, 'rows': cur.fetchall()}
        with gzip.open(backup, 'wt', encoding='utf-8') as out:
            json.dump(snapshot, out, ensure_ascii=False, default=str)
        report['backup'] = str(backup)
        db.commit()
        cur.execute("SHOW INDEX FROM fire_gateway WHERE Key_name='uk_fire_gateway_ip'")
        if not cur.fetchall():
            cur.execute('ALTER TABLE fire_gateway ADD UNIQUE KEY uk_fire_gateway_ip(allowed_ip)')
        cur.execute("SHOW INDEX FROM fire_gateway WHERE Key_name='uk_fire_gateway_source'")
        if cur.fetchall():
            cur.execute('ALTER TABLE fire_gateway DROP INDEX uk_fire_gateway_source')
        db.begin()
        cur.execute('''UPDATE fire_protocol_message m JOIN fire_gateway g ON g.allowed_ip=m.peer_ip
            SET m.gateway_id=g.gateway_id WHERE NOT(m.gateway_id <=> g.gateway_id)''')
        report['messagesReassociated'] = cur.rowcount
        for new_id, gateway_id, old_id in planned:
            cur.execute('UPDATE fire_event SET event_id=%s,gateway_id=%s WHERE event_id=%s', (new_id, gateway_id, old_id))
        for obs in observations:
            gateway = by_ip.get(obs['peer_ip'])
            if not gateway or gateway['del_flag'] != '0':
                continue
            cur.execute('SELECT raw_hex,received_at FROM fire_protocol_message WHERE message_id=%s', (obs['messageId'],))
            message = cur.fetchone()
            cur.execute('''UPDATE fire_gateway SET source_address=%s,center_address=%s,last_received_at=%s,
                revision=revision+1,update_by='ip-reconcile',update_time=NOW() WHERE gateway_id=%s''',
                (message['raw_hex'][24:36], message['raw_hex'][36:48], message['received_at'], gateway['gateway_id']))
        # Preserve original status reports and clocks; do not manufacture a fresh receipt.
        for gateway in gateways:
            if not gateway['allowed_ip'] or gateway['del_flag'] != '0':
                continue
            cur.execute("""SELECT parsed_json FROM fire_protocol_message WHERE peer_ip=%s AND direction='RX'
                AND parse_status='PARSED' ORDER BY message_id""", (gateway['allowed_ip'],))
            states = []
            for row in cur.fetchall():
                for obj in json.loads(row['parsed_json'] or '[]'):
                    if obj.get('type') == 21:
                        states.append(obj)
            if states:
                state = max(enumerate(states), key=lambda item: (item[1].get('eventTime') or '', item[0]))[1]
                cur.execute('UPDATE fire_gateway SET status_bits=%s,status_description=%s,status_device_time=%s WHERE gateway_id=%s',
                            (state.get('statusBits'), state.get('name'), state.get('eventTime'), gateway['gateway_id']))
        db.commit()
        report['completed'] = True
        return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    config = ROOT / 'RuoYi-Vue-master/ruoyi-admin/target/classes/application-druid.yml'
    master = yaml.safe_load(config.read_text('utf-8'))['spring']['datasource']['druid']['master']
    match = re.match(r'jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)', master['url'])
    db = pymysql.connect(host=match[1], port=int(match[2] or 3306), user=master['username'],
                         password=str(master['password']), database=match[3], charset='utf8mb4',
                         cursorclass=pymysql.cursors.DictCursor, connect_timeout=5, read_timeout=15)
    try:
        report = reconcile(db, args.apply)
        print(json.dumps(report, ensure_ascii=False))
        if args.apply:
            (ROOT / 'outputs/device-diagnostics/ip-reconciliation.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), 'utf-8')
    finally:
        db.close()


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    main()
