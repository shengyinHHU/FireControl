const {chromium}=require(process.env.PLAYWRIGHT_PATH || 'playwright');const {spawn,execFile}=require('node:child_process');const path=require('node:path');const fs=require('node:fs');
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port','5178','--strictPort'],{windowsHide:true,stdio:'ignore'});
(async()=>{let browser;try{
 for(let i=0;i<80;i++){try{if((await fetch('http://127.0.0.1:5178')).ok)break}catch{}await new Promise(r=>setTimeout(r,100))}
 let preview;
 browser=await chromium.launch({executablePath:process.env.RTC_BROWSER==='chrome'?'C:/Program Files/Google/Chrome/Application/chrome.exe':'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,args:process.env.RTC_DIRECT==='1'?['--disable-web-security']:[]});const page=await browser.newPage();
 let gatewayFailure;const network=[];page.on('requestfailed',r=>{const u=new URL(r.url());network.push({host:u.host,path:u.pathname,error:r.failure()?.errorText})});page.on('response',r=>{const u=new URL(r.url());if(u.protocol==='https:')network.push({host:u.host,path:u.pathname,status:r.status()})});page.on('console',m=>{if(m.type()==='error')network.push({browserError:m.text().replace(/https?:[^\s'"]+/g,'[redacted-url]')})});
 if(process.env.RTC_DIRECT==='1')await page.route('**/vendor/official-rtc/srs.sdk.js',r=>r.fulfill({contentType:'application/javascript',body:fs.readFileSync('../docs/videoapi/rtc_player/rtc_player/js/srs.sdk.js','utf8').replace(/console\.log\([^;]+;/g,'')}));
 await page.route('**/rtc-check',r=>r.fulfill({contentType:'text/html',body:'<video autoplay muted controls></video><script src="/vendor/official-rtc/jquery-1.10.2.min.js"></script><script src="/vendor/official-rtc/adapter-7.4.0.min.js"></script><script src="/vendor/official-rtc/srs.sdk.js"></script>'}));
 await page.route('**/rtc-signal',async route=>{
  const dir=path.resolve('../tools/video/runtime');let offer=route.request().postDataJSON().sdp;
  if(process.env.RTC_HEVC_PROBE==='1'){
   const sections=offer.split('m=');offer=sections.map(section=>{
    if(!section.startsWith('video '))return section;
    const lines=section.split('\r\n');lines[0]=lines[0].split(' ').slice(0,3).join(' ')+' 122';
    return lines.filter(line=>!/^a=(rtpmap|fmtp|rtcp-fb):/.test(line)).join('\r\n')+'a=rtpmap:122 H265/90000\r\na=fmtp:122 profile-id=1;tier-flag=0;level-id=120;tx-mode=SRST\r\n';
   }).join('m=');
  }
  fs.writeFileSync(path.join(dir,'rtc-offer.sdp'),offer);if(process.env.RTC_HEVC_PROBE==='1')fs.rmSync(path.join(dir,'rtc-answer.json'),{force:true});
  try {await new Promise((resolve,reject)=>execFile('java',['-Dfile.encoding=UTF-8','-cp',fs.readFileSync(path.join(dir,'rtc-classpath.txt'),'utf8'),'VideoRtcSignalCheck',dir],{windowsHide:true,timeout:25000},(error,stdout,stderr)=>{if(error){error.stderr=stderr;reject(error)}else resolve(stdout)}));
   await route.fulfill({contentType:'application/json',body:fs.readFileSync(path.join(dir,'rtc-answer.json'),'utf8')});
  }catch(error){gatewayFailure=(error.stderr||'').match(/RTC网关协商失败，代码 [^\r\n]+/)?.[0] || 'gateway probe did not produce an accepted answer';await route.fulfill({status:502,json:{code:502}})}
 });
 await page.goto('http://127.0.0.1:5178/rtc-check');
 const runtime=path.resolve('../tools/video/runtime');
 await new Promise((resolve,reject)=>execFile('java',['-Dfile.encoding=UTF-8','-cp',fs.readFileSync(path.join(runtime,'rtc-classpath.txt'),'utf8'),'VideoRtcCheck',path.resolve('..'),...(process.env.RTC_VARIANT?[process.env.RTC_VARIANT]:[])],{windowsHide:true,timeout:60000},(error)=>error?reject(error):resolve()));
 preview=JSON.parse(fs.readFileSync(path.join(runtime,'rtc-preview.json'),'utf8'));
 const report=await page.evaluate(async ({url,direct,codec})=>{
  const sdk=new SrsRtcPlayerAsync(),v=document.querySelector('video');v.srcObject=sdk.stream;if(codec==='h265'||codec==='h264'){const add=sdk.pc.addTransceiver.bind(sdk.pc);sdk.pc.addTransceiver=(kind,init)=>{const t=add(kind,init);if(kind==='video'){const codecs=RTCRtpReceiver.getCapabilities('video').codecs.filter(c=>codec==='h264'?/^video\/H264$/i.test(c.mimeType):/video\/(H265|HEVC)/i.test(c.mimeType));if(!codecs.length)throw Error(codec+' not available');t.setCodecPreferences(codecs)}return t}}if(!direct)sdk.signaling=offer=>fetch('/rtc-signal',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({sdp:offer.sdp})}).then(r=>r.json());let signaling='pending',reason='',responseCode;
  sdk.play(url).then(()=>{signaling='success';v.play().catch(()=>{})}).catch(e=>{signaling='failed';reason=e.message || e.statusText || '';responseCode=e.code || e.status});
  for(let i=0;i<250;i++){if(signaling==='failed')break;await new Promise(r=>setTimeout(r,100))}const stats=[];try{(await sdk.pc.getStats()).forEach(s=>{if(['inbound-rtp','codec','candidate-pair','transport'].includes(s.type))stats.push({type:s.type,kind:s.kind,state:s.state,mimeType:s.mimeType,bytesReceived:s.bytesReceived,packetsReceived:s.packetsReceived,framesDecoded:s.framesDecoded,framesReceived:s.framesReceived,keyFramesDecoded:s.keyFramesDecoded,pliCount:s.pliCount,firCount:s.firCount,packetsLost:s.packetsLost,dtlsState:s.dtlsState})})}catch{}
  const result={offeredVideoCodecs:sdk.pc.localDescription?.sdp.split('\r\n').filter(line=>line.startsWith('a=rtpmap:')),answeredVideoCodecs:sdk.pc.remoteDescription?.sdp.split('\r\n').filter(line=>line.startsWith('a=rtpmap:')),signaling,reason,responseCode,connection:sdk.pc.connectionState,ice:sdk.pc.iceConnectionState,videoWidth:v.videoWidth,readyState:v.readyState,tracks:sdk.stream.getTracks().map(t=>({kind:t.kind,readyState:t.readyState,muted:t.muted})),stats};sdk.close();return result;
 },{url:preview.url,direct:process.env.RTC_DIRECT==='1',codec:process.env.RTC_CODEC});report.gatewayFailure=gatewayFailure;report.hevcGatewayProbe=process.env.RTC_HEVC_PROBE==='1';if(report.hevcGatewayProbe){try{report.gatewayVideoCodecs=JSON.parse(fs.readFileSync('../tools/video/runtime/rtc-answer.json','utf8')).sdp.split('\r\n').filter(line=>line.startsWith('a=rtpmap:'))}catch{}}report.requestedCodec=process.env.RTC_CODEC||'browser-default';report.variant=process.env.RTC_VARIANT||'default';report.freshAddress=true;report.directOriginalSdk=process.env.RTC_DIRECT==='1';report.network=network;fs.writeFileSync('../tools/video/runtime/rtc-diagnostic.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));
}finally{await browser?.close();server.kill()}})().catch(e=>{console.error(e.message);process.exitCode=1});
