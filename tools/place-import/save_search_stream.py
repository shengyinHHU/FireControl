import sys,json
from pathlib import Path
out=Path('outputs/regulatory-geocoded/public-search').resolve()
for line in sys.stdin:
 if line.strip()=='__END__':break
 try:
  x=json.loads(line);p=out/x['file']
  if p.parent!=out:raise ValueError('bad path')
  p.write_text(x['text'],encoding='utf-8');print('SAVED '+x['file'],flush=True)
 except Exception as e:print(type(e).__name__,flush=True)
