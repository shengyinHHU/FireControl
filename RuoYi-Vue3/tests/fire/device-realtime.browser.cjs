// Real Vue with controlled HTTP/SSE. No business database or real device is accessed.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const assert = require('node:assert/strict'), fs = require('node:fs')
const base='http://127.0.0.1:5206', output='../outputs/device-realtime'
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port','5206','--strictPort'],{windowsHide:true,stdio:'ignore'})
;(async()=>{
 let browser
 const checks=[],errors=[]
 try{
  for(let i=0;i<150;i++){try{if((await fetch(base)).ok)break}catch{}await new Promise(r=>setTimeout(r,100))}
  browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true})
  const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'})
  await context.addCookies([{name:'Admin-Token',value:'fixture-only',url:base}])
  await context.addInitScript(()=>{
   const original=window.fetch.bind(window);window.__streams=0;window.__aborts=0
   window.fetch=(url,options)=>{
    if(!String(url).includes('/fire/devices/stream'))return original(url,options)
    window.__streams++;window.__streamURL=String(url);window.__streamAuth=options.headers.Authorization
    const encoder=new TextEncoder()
    return Promise.resolve(new Response(new ReadableStream({start(controller){
     window.__emitDevice=()=>controller.enqueue(encoder.encode('event: device-change\ndata: refresh\n\n'))
     window.__closeDevice=()=>controller.close()
     options.signal.addEventListener('abort',()=>{window.__aborts++;try{controller.close()}catch{}})
     controller.enqueue(encoder.encode('event: ready\ndata: refresh\n\n'))
    }}),{headers:{'Content-Type':'text/event-stream'}}))
   }
  })
  let gatewayCalls=0,eventCalls=0,messageCalls=0,active=0,maxActive=0,hold=false,fail=false,readonly=false,release
  let gatewayVersion=0,eventVersion=0,messageVersion=0
  const gateway=n=>({id:String(n),deviceCode:String(n),name:'实时装置'+n,placeName:'测试单位',unitName:'测试单位',allowedIp:'10.0.0.'+n,enabled:'0',revision:0,status:gatewayVersion?'在线':'离线',lastSeen:gatewayVersion?'2026-11-24 17:00:01':'2026-11-24 17:00:00'})
  const alarm=()=>({id:'event-1',sourceCode:'device',source:'传输装置',kind:'fire',name:'设备事件版本'+eventVersion,placeName:'测试单位',gatewayId:'1',occurredAt:'2026-11-24 17:00:00'})
  await context.route('**/dev-api/**',async route=>{
   const url=new URL(route.request().url()),p=url.pathname.replace('/dev-api','');let body={code:200,data:[],rows:[],total:0}
   if(p==='/getInfo')body={code:200,user:{userId:readonly?2:1,userName:'fixture',nickName:'测试用户',avatar:''},roles:readonly?['monitor']:['admin'],permissions:readonly?['fire:gateways:list']:['*:*:*']}
   else if(p==='/getRouters')body={code:200,data:[]}
   else if(p==='/fire/gateways/list'){
    gatewayCalls++;active++;maxActive=Math.max(maxActive,active)
    const snapshot=gatewayVersion
    if(hold)await new Promise(r=>release=r)
    active--;const page=Number(url.searchParams.get('pageNum')||1)
    body=fail?{code:500,msg:'实时更新测试失败'}:{code:200,rows:Array.from({length:Math.min(20,45-(page-1)*20)},(_,i)=>({...gateway((page-1)*20+i+1),deviceStatus:'状态版本'+snapshot})),total:45}
   }
   else if(p==='/fire/alarms/list'){eventCalls++;body={code:200,rows:[alarm()],total:1}}
   else if(p==='/fire/protocol/messages'){messageCalls++;body={code:200,rows:[{id:'message-1',direction:'TX',transmissionStatus:messageVersion?'SENT':'QUEUED',receivedAt:'2026-11-24 17:00:00',rawHex:'4040',parseStatus:'REPLY'}],total:1}}
   else if(p.startsWith('/fire/assets/gateways/'))body={code:200,data:gateway(Number(p.split('/').at(-1)))}
   await route.fulfill({json:body}).catch(()=>{})
  })
  const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));page.setDefaultTimeout(12000)
  await page.clock.install()
  await page.goto(base+'/fire/gateways',{waitUntil:'domcontentloaded'})
  await page.getByRole('cell',{name:'实时装置1',exact:true}).waitFor()
  await page.getByText('实时更新已连接',{exact:true}).waitFor();await page.waitForTimeout(500)
  assert.equal(await page.evaluate(()=>window.__streamAuth),'Bearer fixture-only')
  assert.ok((await page.evaluate(()=>window.__streamURL)).endsWith('/fire/devices/stream?resource=gateways'))
  const initialCalls=gatewayCalls;await page.waitForTimeout(5300);assert.equal(gatewayCalls,initialCalls,'idle table has no 5-second polling')
  checks.push('authenticated SSE connects; no requests or loading flash during the former 5-second interval')
  await page.evaluate(()=>window.__firstDeviceRow=document.querySelector('.el-table__body tbody tr'))
  hold=true;gatewayVersion=1
  for(let i=0;i<20;i++)await page.evaluate(()=>window.__emitDevice())
  while(gatewayCalls===initialCalls)await page.waitForTimeout(20)
  assert.equal(await page.locator('.el-loading-mask:visible').count(),0)
  assert.equal(await page.locator('.fire-table-card').getAttribute('aria-busy'),'false')
  assert.equal(gatewayCalls,initialCalls+1,'burst is coalesced')
  hold=false;release()
  await page.getByRole('cell',{name:'状态版本1',exact:true}).first().waitFor()
  assert.equal(await page.evaluate(()=>window.__firstDeviceRow===document.querySelector('.el-table__body tbody tr')),true)
  assert.equal(maxActive,1)
  checks.push('20 events produce one silent request; row DOM survives and no loading overlay appears')
  await page.locator('.el-pagination .btn-next').click()
  await page.getByRole('cell',{name:'实时装置21',exact:true}).waitFor()
  gatewayVersion=2;await page.evaluate(()=>window.__emitDevice())
  await page.getByRole('cell',{name:'状态版本2',exact:true}).first().waitFor()
  assert.equal(await page.locator('.el-pagination .is-active').innerText(),'2')
  fail=true;await page.evaluate(()=>window.__emitDevice())
  await page.locator('.fire-error').filter({hasText:'当前保留上次成功记录'}).waitFor()
  assert.equal(await page.getByRole('cell',{name:'实时装置21',exact:true}).count(),1)
  assert.equal(await page.locator('.el-message--error').count(),0)
  fail=false;gatewayVersion=3;await page.evaluate(()=>window.__emitDevice())
  await page.getByRole('cell',{name:'状态版本3',exact:true}).first().waitFor()
  assert.equal(await page.locator('.fire-error').count(),0)
  checks.push('page number is preserved; failed background reads retain data without repeated error toasts and recover')
  await page.getByRole('tab',{name:'设备事件',exact:true}).click()
  await page.getByRole('cell',{name:'设备事件版本0',exact:true}).waitFor()
  eventVersion=1;await page.evaluate(()=>window.__emitDevice())
  await page.getByRole('cell',{name:'设备事件版本1',exact:true}).waitFor()
  assert.equal(await page.evaluate(()=>window.__streams),1,'embedded tabs share manager stream')
  await page.getByRole('tab',{name:'通信报文',exact:true}).click()
  await page.getByRole('cell',{name:'QUEUED',exact:true}).waitFor()
  messageVersion=1;await page.evaluate(()=>window.__emitDevice())
  await page.getByRole('cell',{name:'SENT',exact:true}).waitFor()
  assert.equal(await page.evaluate(()=>window.__streams),1)
  checks.push('device event and protocol status tabs update in real time through one shared connection')
  await page.evaluate(()=>window.__closeDevice())
  await page.waitForFunction(()=>window.__streams===2)
  await page.getByText('实时更新已连接',{exact:true}).waitFor()
  fs.mkdirSync(output,{recursive:true});await page.screenshot({path:output+'/desktop.png',fullPage:true})
  await page.setViewportSize({width:390,height:844});await page.keyboard.press('Tab')
  await page.screenshot({path:output+'/narrow.png',fullPage:true})
  checks.push('stream reconnects and reconciles; keyboard and 390px layout exercised')
  await page.getByRole('tab',{name:'传输装置',exact:true}).click()
  await page.getByRole('cell',{name:'实时装置21',exact:true}).waitFor()
  const fallbackBefore=gatewayCalls
  await page.clock.fastForward(61000);await page.clock.runFor(500)
  for(let i=0;i<100&&gatewayCalls===fallbackBefore;i++)await page.waitForTimeout(20)
  assert.ok(gatewayCalls>fallbackBefore,'low-frequency fallback queries without an incoming event')
  checks.push('60-second silent reconciliation remains available for missed events')
  await page.clock.resume()
  readonly=true;await page.reload({waitUntil:'domcontentloaded'})
  await page.getByRole('cell',{name:'实时装置21',exact:true}).waitFor()
  await page.getByText('实时更新已连接',{exact:true}).waitFor()
  assert.equal(await page.getByRole('tab',{name:'设备事件',exact:true}).count(),0)
  assert.ok((await page.evaluate(()=>window.__streamURL)).includes('resource=gateways'))
  checks.push('gateway-only role subscribes without requiring alarm permission')
  const aborts=await page.evaluate(()=>window.__aborts)
  await page.getByRole('tab',{name:'通信报文',exact:true}).click()
  await page.getByRole('cell',{name:'SENT',exact:true}).waitFor()
  await page.evaluate(()=>window.__leaveDevice=window.__aborts)
  await page.goto(base+'/login',{waitUntil:'domcontentloaded'})
  assert.deepEqual(errors,[])
  fs.writeFileSync(output+'/browser.json',JSON.stringify({actualVue:true,fixtureHTTP:true,fixtureSSE:true,businessDatabaseAccessed:false,checks,gatewayCalls,eventCalls,messageCalls,maxActive,errors},null,2))
  console.log(checks.join('\n'))
 }finally{await browser?.close();server.kill()}
})().catch(error=>{console.error(error);process.exitCode=1})
