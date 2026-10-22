from pathlib import Path
import subprocess,re,os
root=Path(__file__).resolve().parents[2]
flags=getattr(subprocess,'CREATE_NO_WINDOW',0)
def run(args,**kw):return subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,creationflags=flags,**kw)
b=run([r'D:\env\apache-maven-3.9.9\bin\mvn.cmd','-o','-X','-pl','ruoyi-admin','-am','-DskipTests','compile'],cwd=root/'RuoYi-Vue-master',timeout=90)
if b.returncode:raise RuntimeError('Maven dependency discovery failed')
paths=re.findall(r'compilePath = \[([^\r\n]+)\]',b.stdout.decode(errors='replace'))
cp=os.pathsep.join(paths[-1].split(', '))
out=root/'tools/video/runtime';out.mkdir(exist_ok=True)
c=run(['javac','-encoding','UTF-8','-cp',cp,'-d',str(out),str(root/'tools/video/VideoLiveCheck.java')],timeout=30)
if c.returncode:raise RuntimeError(c.stderr.decode(errors='replace'))
t=run(['java','-Dfile.encoding=UTF-8','-cp',str(out)+os.pathsep+cp,'VideoLiveCheck',str(root)],timeout=120)
print(t.stdout.decode(errors='replace'))
if t.returncode:raise RuntimeError(t.stderr.decode(errors='replace'))
