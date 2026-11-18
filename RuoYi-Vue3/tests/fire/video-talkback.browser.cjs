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
   constructor(url,...args){if(!String(url).includes('/fire/video/dahua/socket'))return new Native(url,...args);this.url=url;this.readyState=0;this.bufferedAmount=0;setTimeout(()=>{if(this.readyState===0){this.readyState=1;this.onopen?.({})}},20)}
   send(data){assertReady(this.readyState);if(typeof data==='string'){const message=JSON.parse(data);window.talkCheck.controls.push(message);if(message.type==='auth')setTimeout(()=>{this.onmessage?.({data:JSON.stringify(window.talkCheck.rejectStart?{type:'error',message:'fixture start rejected'}:{type:'ready'})});const raw=new ArrayBuffer(652),v=new DataView(raw);v.setInt32(0,1,true);v.setInt32(4,8000,true);v.setInt32(8,16,true);this.onmessage?.({data:raw})},20);if(message.type==='ping')this.onmessage?.({data:'{"type":"pong"}'})}else{const bytes=new Int16Array(data);window.talkCheck.frames.push({length:data.byteLength,nonzero:bytes.some(v=>v!==0)})}if(window.talkCheck.backlog)this.bufferedAmount=9000}
   close(){if(this.readyState===3)return;this.readyState=3;window.talkCheck.closed++;setTimeout(()=>this.onclose?.({code:1000}),0)}
  }
  function assertReady(state){if(state!==1)throw Error('send after close')}
  window.WebSocket=VoiceSocket
  const get=navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices)
  navigator.mediaDevices.getUserMedia=async constraints=>{if(window.talkCheck.deny)throw new DOMException('fixture denied','NotAllowedError');const stream=await get(constraints);for(const track of stream.getTracks()){window.talkCheck.tracks++;const stop=track.stop.bind(track);let stopped=false;track.stop=()=>{if(!stopped){stopped=true;window.talkCheck.stopped++}return stop()}}return stream}
  const Audio=window.AudioContext
  window.AudioContext=class extends Audio{createGain(){const gain=super.createGain();(window.talkGains ||= []).push(gain);return gain}close(){window.talkCheck.contextsClosed++;return super.close()}}
 })
 let ticketCalls=0,delay=0,rejected=false,permission=true,binding=null,cancelled=[]
 const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.stack))
 await context.route('**/dev-api/**',async route=>{
  const req=route.request(),p=new URL(req.url()).pathname.replace('/dev-api','');let body={code:200,data:[],rows:[],total:0}
  if(p==='/getInfo')body={code:200,user:{userId:1,userName:'admin',nickName:'管理员',avatar:''},roles:permission?['admin']:['operator'],permissions:permission?['*:*:*']:['fire:video:list','fire:video:preview']}
  else if(p==='/getRouters')body={code:200,data:[]}
  else if(p==='/fire/video/regions')body={code:200,data:[]}
  else if(p==='/fire/video/devices')body={code:200,data:{lastId:-1,list:[{deviceCode:'fixture-talk',deviceName:'对讲测试摄像头'}]}}
  else if(p.endsWith('/preview'))body={code:200,data:{url:'/fire/video/flv/talk-fixture',protocol:'flv',requiresAuth:true}}
  else if(p.startsWith('/fire/video/flv/')){if(req.method()==='DELETE'){await route.fulfill({json:{code:200}});return}await route.fulfill({body:Buffer.from([70,76,86,1,1,0,0,0,9,0,0,0,0]),contentType:'video/x-flv'});return}
  else if(p.endsWith('/binding')){if(req.method()==='PUT'){binding={...req.postDataJSON(),revision:0};body={code:200,data:binding}}else if(req.method()==='DELETE')binding=null;else body={code:200,data:{endpoint:binding,profiles:['default']}}}
  else if(p.endsWith('/sessions')&&req.method()==='POST'){assert.equal(req.headers().authorization,'Bearer fixture-only');ticketCalls++;if(delay)await new Promise(r=>setTimeout(r,delay));body=rejected?{code:500,msg:'测试设备不支持对讲'}:{code:200,data:{ticket:'ticket-'+ticketCalls,path:'/fire/video/dahua/socket'}}}
  else if(p==='/fire/video/dahua/sessions'&&req.method()==='DELETE')cancelled.push(req.postDataJSON().ticket)
  await route.fulfill({json:body})
 })
 const open=async()=>{await page.getByRole('region',{name:'真实视频监控'}).locator('.monitor-device').first().click();const dialog=page.getByRole('dialog',{name:'对讲测试摄像头',exact:true});await dialog.waitFor();return dialog}
 await page.goto(base+'/fire/video');await page.locator('.monitor-device').first().waitFor();assert.equal(ticketCalls,0)
 fs.mkdirSync('../docs/verification',{recursive:true});let dialog=await open();await dialog.getByRole('button',{name:'连接配置',exact:true}).click()
 let config=page.getByRole('dialog',{name:'摄像机对讲连接',exact:true});await config.getByPlaceholder('例如 10.5.90.150').fill('10.5.90.150');await config.locator('.el-loading-mask').waitFor({state:'hidden'});await config.getByRole('combobox',{name:'摄像机登录配置'}).press('ArrowDown');await page.getByRole('option',{name:'default',exact:true}).click();await page.setViewportSize({width:720,height:900});const bounds=await config.boundingBox();assert.ok(bounds.x>=0 && bounds.x+bounds.width<=721);await page.screenshot({path:'../docs/verification/dahua-binding-dialog.png'});await page.setViewportSize({width:1440,height:1000});await config.getByRole('button',{name:'保存',exact:true}).click();await config.waitFor({state:'hidden'});assert.equal(binding.host,'10.5.90.150');assert.equal(binding.port,37777);assert.equal(binding.password,undefined);checks.push('IP/port/profile binding saved without browser password')
 await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await page.waitForFunction(()=>window.talkCheck.frames.length>=13,{},{timeout:15000});await dialog.getByText('正在双向对讲',{exact:true}).waitFor()
 await dialog.getByRole('button',{name:'关闭声音',exact:true}).click();assert.equal(await page.evaluate(()=>window.talkGains.at(-2).gain.value),0);await dialog.getByRole('button',{name:'开启声音',exact:true}).click();assert.equal(await page.evaluate(()=>window.talkGains.at(-2).gain.value),1);checks.push('shared sound button controls SDK listening during talk');let state=await page.evaluate(()=>window.talkCheck);assert.equal(state.controls[0].type,'auth');assert.ok(state.frames.every(f=>f.length===640));assert.ok(state.frames.some(f=>f.nonzero))
 await dialog.locator('summary').filter({hasText:'对讲诊断'}).click();await dialog.getByText(/已接收 640 字节/).waitFor();fs.mkdirSync('../docs/verification',{recursive:true});await page.screenshot({path:'../docs/verification/dahua-talkback-active.png'});checks.push('real Chrome AudioWorklet sends PCM16 8k 640-byte fake microphone packets and decodes downlink')
 await dialog.getByRole('button',{name:'结束对讲',exact:true}).click();await page.waitForFunction(()=>window.talkCheck.tracks===window.talkCheck.stopped);state=await page.evaluate(()=>window.talkCheck);assert.equal(state.controls.at(-1).type,'stop');assert.ok(state.contextsClosed>=1)
 await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByText('正在双向对讲',{exact:true}).waitFor();await page.keyboard.press('Escape');await dialog.waitFor({state:'hidden'});await page.waitForFunction(()=>window.talkCheck.tracks===window.talkCheck.stopped);checks.push('stop and close dialog release microphone and AudioContext')
 dialog=await open();const before=ticketCalls;await page.evaluate(()=>window.talkCheck.deny=true);await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByRole('alert').filter({hasText:'麦克风权限被拒绝'}).waitFor();assert.equal(ticketCalls,before);await page.evaluate(()=>window.talkCheck.deny=false);checks.push('permission denial creates no ticket')
 delay=1200;await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByText('正在申请对讲',{exact:true}).waitFor();await dialog.getByRole('button',{name:'取消对讲',exact:true}).click();await page.waitForTimeout(1600);assert.ok(cancelled.includes('ticket-'+ticketCalls));checks.push('late ticket cancelled after pending request stopped')
 delay=0;await page.evaluate(()=>window.talkCheck.rejectStart=true);const frameCount=await page.evaluate(()=>window.talkCheck.frames.length);await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByRole('alert').filter({hasText:'fixture start rejected'}).waitFor();assert.equal(await page.evaluate(()=>window.talkCheck.frames.length),frameCount);await page.evaluate(()=>window.talkCheck.rejectStart=false);checks.push('no audio before SDK ready; rejected start releases capture')
 rejected=true;await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByRole('alert').filter({hasText:'测试设备不支持对讲'}).waitFor();await page.waitForFunction(()=>window.talkCheck.tracks===window.talkCheck.stopped)
 rejected=false;await page.evaluate(()=>window.talkCheck.backlog=true);await dialog.getByRole('button',{name:'开始对讲',exact:true}).click();await dialog.getByRole('alert').filter({hasText:'语音发送积压'}).waitFor();await page.waitForFunction(()=>window.talkCheck.tracks===window.talkCheck.stopped);await page.evaluate(()=>window.talkCheck.backlog=false);checks.push('API and backpressure errors release capture')
 await dialog.getByRole('button',{name:'连接配置',exact:true}).click();config=page.getByRole('dialog',{name:'摄像机对讲连接',exact:true});await config.getByRole('button',{name:'解除绑定',exact:true}).click();await page.getByRole('button',{name:'确定',exact:true}).click();await config.waitFor({state:'hidden'});assert.equal(binding,null);checks.push('review and remove binding with confirmation')
 await page.keyboard.press('Escape');permission=false;await page.reload();await page.locator('.monitor-device').first().waitFor();dialog=await open();assert.equal(await dialog.getByRole('button',{name:'开始对讲',exact:true}).count(),0);assert.equal(await dialog.getByRole('button',{name:'连接配置',exact:true}).count(),0);checks.push('preview-only user has no talk/config actions')
 assert.deepEqual(errors,[]);fs.writeFileSync('../docs/verification/dahua-talkback-browser.json',JSON.stringify({fixtureGateway:true,fakeMicrophone:true,realDeviceAudioSent:false,checks,errors},null,2));console.log(checks.join('\n'))
 }finally{await browser?.close();server.kill()}
})().catch(e=>{console.error(e);process.exitCode=1})
