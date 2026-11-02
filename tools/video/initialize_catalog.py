"""Explicit initial sync against configured business DB/vendor. Credentials stay in Java memory."""
import subprocess,re,os,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[2]
flags=getattr(subprocess,'CREATE_NO_WINDOW',0)
def run(args,**kw):return subprocess.run([str(x) for x in args],stdout=subprocess.PIPE,stderr=subprocess.PIPE,creationflags=flags,**kw)
b=run([r'D:\env\apache-maven-3.9.9\bin\mvn.cmd','-o','-X','-pl','ruoyi-admin','-am','-DskipTests','compile'],cwd=root/'RuoYi-Vue-master',timeout=90)
if b.returncode:raise RuntimeError('Backend compile failed')
paths=re.findall(r'compilePath = \[([^\r\n]+)\]',b.stdout.decode(errors='replace'));cp=os.pathsep.join(paths[-1].split(', '))
out=Path(tempfile.mkdtemp(prefix='video-catalog-setup-'))
c=run(['javac','-encoding','UTF-8','-cp',cp,'-d',out,root/'tools/video/VideoCatalogInitialSync.java'],timeout=30)
if c.returncode:raise RuntimeError(c.stderr.decode(errors='replace'))
r=subprocess.run(['java','-Dfile.encoding=UTF-8','-cp',str(out)+os.pathsep+cp,'VideoCatalogInitialSync',str(root)],creationflags=flags,cwd=root)
raise SystemExit(r.returncode)
