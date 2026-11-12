"""Python 3 standard-library client. No vendor secrets; no automatic network retry."""
import argparse
import getpass
import json
import os
from pathlib import Path
import sys
import urllib.error
import urllib.request
import uuid

class SmsError(Exception):
    def __init__(self, status, body, retry_after=None):
        self.status, self.body, self.retry_after = status, body, retry_after
        super().__init__(f'HTTP {status}: {body}; Retry-After={retry_after}')

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

class SmsClient:
    def __init__(self, base_url, username, password, timeout=45):
        self.base_url = base_url.rstrip('/')
        if not self.base_url.startswith(('https://', 'http://')):
            raise ValueError('SMS_BASE_URL must be an HTTP(S) API root')
        self.username, self.password, self.timeout = username, password, timeout
        self.token = None
        self.http = urllib.request.build_opener(NoRedirect())

    def _request(self, method, path, payload=None, auth=True):
        headers = {'Accept': 'application/json'}
        if auth:
            headers['Authorization'] = 'Bearer ' + self.token
        data = None if payload is None else json.dumps(payload, ensure_ascii=False).encode('utf-8')
        if data is not None:
            headers['Content-Type'] = 'application/json; charset=UTF-8'
        request = urllib.request.Request(self.base_url + path, data=data, headers=headers, method=method)
        try:
            with self.http.open(request, timeout=self.timeout) as response:
                body = json.loads(response.read().decode('utf-8'))
                if body.get('code') != 200:
                    raise SmsError(response.status, body)
                return body
        except urllib.error.HTTPError as e:
            raw = e.read().decode('utf-8', errors='replace')
            try: body = json.loads(raw)
            except ValueError: body = raw[:500]
            raise SmsError(e.code, body, e.headers.get('Retry-After')) from None

    def login(self):
        result = self._request('POST', '/fire/sms/open/token', {'username': self.username, 'password': self.password}, False)
        self.token = result['token']
        return result

    def _authenticated(self, method, path, payload=None):
        if not self.token:
            self.login()
        try:
            return self._request(method, path, payload)
        except SmsError as e:
            if e.status != 401:
                raise
            self.login()
            # Same requestId and original payload; one refresh only.
            return self._request(method, path, payload)

    def send(self, payload):
        return self._authenticated('POST', '/fire/sms/open/send', payload)

    def query(self, request_id):
        if str(uuid.UUID(request_id)) != request_id:
            raise ValueError('requestId must be a lowercase UUID')
        return self._authenticated('GET', '/fire/sms/open/requests/' + request_id)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    subs = parser.add_subparsers(dest='command', required=True)
    prepare = subs.add_parser('prepare', help='Write one new request file; does not send SMS')
    prepare.add_argument('file', type=Path)
    prepare.add_argument('--mobile', action='append', required=True, help='Repeat this option for multiple recipients')
    prepare.add_argument('--content', required=True)
    prepare.add_argument('--biz-id', default='')
    for command in ('send', 'query'):
        sub = subs.add_parser(command)
        sub.add_argument('file', type=Path)
    args = parser.parse_args()
    if args.command == 'prepare':
        value = dict(requestId=str(uuid.uuid4()), mobiles=args.mobile, content=args.content, bizId=args.biz_id)
        with args.file.open('x', encoding='utf-8') as f:
            json.dump(value, f, ensure_ascii=False, indent=2)
        print('Prepared (not sent):', args.file)
        return
    payload = json.loads(args.file.read_text('utf-8-sig'))
    request_id = payload['requestId']
    if str(uuid.UUID(request_id)) != request_id:
        raise ValueError('requestId must be a lowercase UUID; keep the original file')
    password = os.environ.get('SMS_PASSWORD') or getpass.getpass('SMS password: ')
    client = SmsClient(os.environ['SMS_BASE_URL'], os.environ['SMS_USERNAME'], password)
    try:
        result = client.send(payload) if args.command == 'send' else client.query(request_id)
        print(json.dumps(result, ensure_ascii=False, indent=2))
    except (OSError, ValueError, SmsError) as e:
        print(str(e), file=sys.stderr)
        print('Keep requestId=' + request_id + '; query this ID before considering any resend.', file=sys.stderr)
        raise SystemExit(1)

if __name__ == '__main__':
    main()
