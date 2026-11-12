"""Check local SMS properties through the production Spring loader, without sending SMS."""
from pathlib import Path
import os,re,subprocess
root=Path(__file__).resolve().parents[2]
flags=getattr(subprocess,'CREATE_NO_WINDOW',0)
build=subprocess.run([os.environ.get('MAVEN_CMD',r'D:\env\apache-maven-3.9.9\bin\mvn.cmd'),'-o','-X','-pl','ruoyi-admin','-am','-DskipTests','package'],cwd=root/'RuoYi-Vue-master',stdout=subprocess.PIPE,stderr=subprocess.PIPE,creationflags=flags,timeout=120)
if build.returncode:raise RuntimeError((build.stdout+build.stderr).decode('utf-8',errors='replace')[-4000:])
cp=os.pathsep.join(re.findall(r'compilePath = \[([^\r\n]+)\]',build.stdout.decode('utf-8',errors='replace'))[-1].split(', '))
check=subprocess.run(['java','-Dfile.encoding=UTF-8','--class-path',cp,str(root/'tools/sms/SmsConfigCheck.java'),str(root/'RuoYi-Vue-master/config/fire-sms-local.properties')],stdout=subprocess.PIPE,stderr=subprocess.PIPE,creationflags=flags,timeout=30)
if check.returncode:raise RuntimeError((check.stdout+check.stderr).decode('utf-8',errors='replace')[-3000:])
print(check.stdout.decode('utf-8').strip())
