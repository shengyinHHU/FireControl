"""Offline Java verification; bounded child processes; never reads device credentials."""
import os,re,subprocess,tempfile,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
flags=getattr(subprocess,'CREATE_NO_WINDOW',0)
def run(args,**kwargs):
    p=subprocess.run([str(x) for x in args],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=120,creationflags=flags,**kwargs)
    if p.returncode: raise RuntimeError((p.stdout+p.stderr).decode(errors='replace')[-8000:])
    return p.stdout.decode(errors='replace')
maven=os.environ.get('MAVEN_CMD',r'D:\env\apache-maven-3.9.9\bin\mvn.cmd')
build=run([maven,'-o','-X','-pl','ruoyi-admin','-am','-DskipTests','compile'],cwd=ROOT/'RuoYi-Vue-master')
paths=re.findall(r'compilePath = \[([^\r\n]+)\]',build)
if not paths: raise RuntimeError('No compiler dependency path')
classpath=os.pathsep.join(paths[-1].split(', '))
with tempfile.TemporaryDirectory(prefix='dahua-offline-') as out:
    # Compile feature sources independently: an open IDE can otherwise overwrite target/classes.
    sources=list((ROOT/'RuoYi-Vue-master/ruoyi-system/src/main/java/com/ruoyi/system/fire/dahua').glob('*.java'))
    sources+=list((ROOT/'RuoYi-Vue-master/ruoyi-framework/src/main/java/com/ruoyi/framework/websocket').glob('Dahua*.java'))
    run(['javac','-J-Duser.language=en','-encoding','UTF-8','-cp',classpath,'-d',out,*sources,ROOT/'tools/dahua/DahuaLifecycleTest.java'])
    print(run(['java','-Dfile.encoding=UTF-8','-cp',out+os.pathsep+classpath,'com.ruoyi.framework.websocket.DahuaLifecycleTest']))
    if '--native-smoke' in sys.argv:
        run(['javac','-encoding','UTF-8','-cp',out+os.pathsep+classpath,'-d',out,ROOT/'tools/dahua/DahuaNativeSmokeTest.java'])
        print(run(['java','-Dfile.encoding=UTF-8','-cp',out+os.pathsep+classpath,'DahuaNativeSmokeTest']))
