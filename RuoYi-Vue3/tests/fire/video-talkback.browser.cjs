const {chromium}=require(process.env.PLAYWRIGHT_PATH||'playwright')
const {spawn}=require('node:child_process'),assert=require('node:assert/strict'),fs=require('node:fs')
const base='http://127.0.0.1:5187'
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port','5187','--strictPort'],{windowsHide:true,stdio:'ignore'})
;(async()=>{let browser;const checks=[];try{
 for(let i=0;i<100;i++){try{if((await fetch(base)).ok)break}catch{}await new Promise(r=>setTimeout(r,100))}
 browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true,args:['--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream']})
 const context=await browser.newContext({viewport:{width:1440,height:1000},permissions:['microphone']})
 await context.addCookies([{name:'Admin-Token',value:'fixture-only',url:base}])
 await context.addInitScript(()=>{
  window.talkCheck={frames:[],controls:[],closed:0,tracks:0,stopped:0,contextsClosed:0,deny:false,backlog:false,rejectStart:false}
  const Native=window.WebSocket
  class VoiceSocket {
   static OPEN=1;static CONNECTING=0;static CLOSING=2;static CLOSED=3
   constructor(url,...args){if(!url.includes('voice.example.invalid'))return new Native(url,...args);this.url=url;this.readyState=0;this.bufferedAmount=0;setTimeout(()=>{if(this.readyState===0){this.readyState=1;this.onopen?.({})}},20)}
   send(data){assertReady(this.readyState);if(typeof data==='string'){const message=JSON.parse(data);window.talkCheck.controls.push(message);if(message.method==='10000')setTimeout(()=>this.onmessage?.({data:JSON.stringify({method:'10001',code:window.talkCheck.rejectStart?'9':'0',msg:window.talkCheck.rejectStart?'fixture start rejected':'success'})}),20)}else{const bytes=new Int8Array(data.buffer,data.byteOffset,data.byteLength);window.talkCheck.frames.push({length:bytes.length,nonzero:bytes.some(v=>v!==0)})}if(window.talkCheck.backlog)this.bufferedAmount=9000}
   close(){if(this.readyState===3)return;this.readyState=3;window.talkCheck.closed++;setTimeout(()=>this.onclose?.({code:1000}),0)}
  }
  function assertReady(state){if(state!==1)throw Error('send after close')}
  window.WebSocket=VoiceSocket
  const get=navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices)
  navigator.mediaDevices.getUserMedia=async constraints=>{if(window.talkCheck.deny)throw new DOMException('fixture denied','NotAllowedError');const stream=await get(constraints);for(const track of stream.getTracks()){window.talkCheck.tracks++;const stop=track.stop.bind(track);let stopped=false;track.stop=()=>{if(!stopped){stopped=true;window.talkCheck.stopped++}return stop()}}return stream}
  const Audio=window.AudioContext
  window.AudioContext=class extends Audio{close(){window.talkCheck.contextsClosed++;return super.close()}}
 })
 let ticketCalls=0,delay=0,rejected=false,permission=true
 const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.stack))
 await context.route('**/dev-api/**',async route=>{
  const req=route.request(),p=new URL(req.url()).pathname.replace('/dev-api','');let body={code:200,data:[],rows:[],total:0}
  if(p==='/getInfo')body={code:200,user:{userId:1,userName:'admin',nickName:'管理员',avatar:''},roles:permission?['admin']:['operator'],permissions:permission?['*:*:*']:['fire:video:list','fire:video:preview']}
  else if(p==='/getRouters')body={code:200,data:[]}
  else if(p==='/fire/video/regions')body={code:200,data:[]}
  else if(p==='/fire/video/devices')body={code:200,data:{lastId:-1,list:[{deviceCode:'fixture-talk',deviceName:'对讲测试摄像头'}]}}
  else if(p.endsWith('/preview'))body={code:200,data:{url:'/fire/video/flv/talk-fixture',protocol:'flv',requiresAuth:true}}
  else if(p.startsWith('/fire/video/flv/')){if(req.method()==='DELETE'){await route.fulfill({json:{code:200}});return}await route.fulfill({body:Buffer.from([70,76,86,1,1,0,0,0,9,0,0,0,0]),contentType:'video/x-flv'});return}
  else if(p.endsWith('/talkback')){assert.equal(req.headers().authorization,'Bearer fixture-only');assert.equal(req.method(),'POST');ticketCalls++;if(delay)await new Promise(r=>setTimeout(r,delay));body=rejected?{code:500,msg:'测试设备不支持对讲'}:{code:200,data:{wss:'wss://voice.example.invalid:8935/path?sign=fixture',token:'voice-only',sessionId:'session-'+ticketCalls}}}
  await route.fulfill({json:body})
 })
 const open=async()=>{await page.getByRole('region',{name:'真实视频监控'}).locator('.monitor-device').first().click();const video=page.getByRole('dialog').locator('video');await page.waitForFunction(()=>!!document.querySelector('.live-stage video')?.src,{},{timeout:12000});await video.evaluate(v=>Object.defineProperty(v,'videoWidth',{configurable:true,get:()=>640}));await page.waitForTimeout(600);await video.dispatchEvent('playing');return page.getByRole('dialog')}
 await page.goto(base+'/fire/inspections');await page.getByRole('tab',{name:'视频监控',exact:true}).click();await page.locator('.monitor-device').first().waitFor();assert.equal(ticketCalls,0)
 let dialog=await open();assert.equal(ticketCalls,0)
 await dialog.getByRole('button',{name:'开始对讲',exact:true}).click()
 try{await page.waitForFunction(()=>window.talkCheck.frames.length>=3,{},{timeout:15000})}catch(e){console.log('TALK FIXTURE',await dialog.innerText(),await page.evaluate(()=>window.talkCheck),errors);throw e};await dialog.getByText('正在发送语音',{exact:true}).waitFor()
 await page.screenshot({path:'docs/verification/video-talkback-active.png'});await page.setViewportSize({width:720,height:900});const bounds=await dialog.boundingBox();assert.ok(bounds.x>=0&&bounds.x+bounds.width<=721);await page.screenshot({path:'docs/verification/video-talkback-narrow.png'});await page.setViewportSize({width:1440,height:1000})
 let state=await page.evaluate(()=>window.talkCheck);assert.equal(state.controls[0].method,'10000');assert.equal(state.controls[0].level,'1');assert.ok(state.frames.every(f=>f.length===320));assert.ok(state.frames.some(f=>f.nonzero))
 await dialog.locator('summary').filter({hasText:'对讲诊断'}).click();await dialog.getByText(/开始确认：已确认/).waitFor();await page.waitForFunction(()=>/峰值 [1-9]/.test(document.querySelector('.talk-diagnostics')?.innerText||''),{},{timeout:5000});checks.push('input power meter detects nonzero fake microphone signal; start acknowledgement is shown');
 await dialog.getByRole('button',{name:'结束对讲',exact:true}).click();await page.waitForFunction(()=>window.talkCheck.tracks===window.talkCheck.stopped)
 state=await page.evaluate(()=>window.talkCheck);assert.equal(state.controls.at(-1).method,'10006');assert.ok(state.contextsClosed>=1);checks.push('official Recorder captures fake microphone and emits exact 320-byte A-law frames after 10000; explicit 10006 stop releases tracks and AudioContext')
 await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByText('正在发送语音',{exact:true}).waitFor();await page.keyboard.press('Escape');await dialog.waitFor({state:'hidden'});await page.waitForFunction(()=>window.talkCheck.tracks===window.talkCheck.stopped);checks.push('closing the video dialog stops the gateway and microphone')
 dialog=await open();const before=ticketCalls;await page.evaluate(()=>window.talkCheck.deny=true);await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByRole('alert').filter({hasText:'麦克风权限被拒绝'}).waitFor();assert.equal(ticketCalls,before);await page.evaluate(()=>window.talkCheck.deny=false);checks.push('microphone denial never requests a short-lived vendor ticket')
 delay=1200;await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByText('正在申请对讲',{exact:true}).waitFor();await dialog.getByRole('button',{name:'取消对讲',exact:true}).click();await page.waitForTimeout(1600);state=await page.evaluate(()=>window.talkCheck);assert.equal(state.tracks,state.stopped);assert.equal(state.controls.at(-1).method,'10006');checks.push('cancelled pending request retires its late ticket without recording')
 delay=0;await page.evaluate(()=>window.talkCheck.rejectStart=true);const framesBeforeReject=await page.evaluate(()=>window.talkCheck.frames.length);await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByRole('alert').filter({hasText:'fixture start rejected'}).waitFor();assert.equal(await page.evaluate(()=>window.talkCheck.frames.length),framesBeforeReject);await page.evaluate(()=>window.talkCheck.rejectStart=false);checks.push('gateway start rejection stops capture and no audio is sent before acknowledgement')
 rejected=true;await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByRole('alert').filter({hasText:'测试设备不支持对讲'}).waitFor();await page.waitForFunction(()=>window.talkCheck.tracks===window.talkCheck.stopped);checks.push('vendor unsupported-device error is shown and microphone is released')
 rejected=false;await page.evaluate(()=>window.talkCheck.backlog=true);await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByRole('alert').filter({hasText:'语音发送积压'}).waitFor();await page.waitForFunction(()=>window.talkCheck.tracks===window.talkCheck.stopped);checks.push('socket backpressure stops capture instead of accumulating delayed speech')
 await page.keyboard.press('Escape');permission=false;await page.reload();await page.getByRole('tab',{name:'视频监控',exact:true}).click();await page.locator('.monitor-device').first().waitFor();dialog=await open();assert.equal(await dialog.getByRole('button',{name:'开始对讲',exact:true}).count(),0);checks.push('view/preview permission does not grant talk permission')
 assert.deepEqual(errors,[]);fs.mkdirSync('docs/verification',{recursive:true});fs.writeFileSync('docs/verification/video-talkback.json',JSON.stringify({fixtureGateway:true,fakeMicrophone:true,realDeviceAudioSent:false,checks,errors},null,2));console.log(checks.join('\n'))
 }finally{await browser?.close();server.kill()}
})().catch(e=>{console.error(e);process.exitCode=1})
