// Actual Vue; controlled backend/stream fixtures, not a live device deployment.
const {chromium}=require(process.env.PLAYWRIGHT_PATH||'playwright')
const {spawn}=require('node:child_process')
const fs=require('node:fs'),assert=require('node:assert/strict')
const base='http://127.0.0.1:5178'
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port','5178','--strictPort'],{windowsHide:true,stdio:'ignore'})
;(async()=>{let browser;let release;const slow=new Promise(r=>release=r),checks=[];try{
 for(let i=0;i<100;i++){try{if((await fetch(base)).ok)break}catch{}await new Promise(r=>setTimeout(r,100))}
 browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true})
 const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'})
 await context.addCookies([{name:'Admin-Token',value:'fixture-only',url:base}])
 await context.addInitScript(()=>{
  const realFetch=window.fetch.bind(window)
  window.__streams=0;window.__streamAborts=0
  window.fetch=(url,options)=>{
   if(!String(url).endsWith('/fire/alarms/stream'))return realFetch(url,options)
   window.__streams++;window.__streamAuthorization=options.headers.Authorization
   const encoder=new TextEncoder()
   const stream=new ReadableStream({start(controller){window.__emitAlarm=()=>controller.enqueue(encoder.encode('event: alarm-change\ndata: refresh\n\n'));window.__endStream=()=>controller.close();options.signal.addEventListener('abort',()=>{window.__streamAborts++;try{controller.close()}catch{}})}})
   return Promise.resolve(new Response(stream,{headers:{'Content-Type':'text/event-stream'}}))
  }
 })
 let stage=0,alarmCalls=0,placeCalls=0,hostStatsCalls=0,fail=false,delay=0,active=0,maxActive=0
 const event=n=>({id:'fire-'+n,sourceCode:'device',source:'消控室主机',kind:'fire',name:'实时火警测试'+n,placeName:'测试场所',placeId:'102',occurredAt:'2026-10-22 23:00:00',status:'待核验',lng:118.75,lat:32.22})
 await context.route('**/dev-api/**',async route=>{
  const u=new URL(route.request().url()),p=u.pathname.replace('/dev-api','');let body={code:200,data:[],rows:[],total:0}
  if(p==='/getInfo')body={code:200,user:{userId:1,userName:'admin',nickName:'管理员',avatar:''},roles:['admin'],permissions:['*:*:*']}
  else if(p==='/getRouters')body={code:200,data:[]}
  else if(p==='/fire/hosts/stats'){hostStatsCalls++;await slow;body={code:200,data:{hosts:1,gateways:1,online:1}}}
  else if(p==='/fire/alarms/stats')body={code:200,data:{eventTotal:stage,fireEvents:stage,faultEvents:0}}
  else if(p==='/fire/119/status')body={code:200,data:{enabled:true}}
  else if(p==='/fire/places/list'){placeCalls++;body={code:200,rows:[],total:0}}
  else if(p==='/fire/places/map')body={code:200,data:[],truncated:false}
  else if(p==='/fire/alarms/list'){
   alarmCalls++;active++;maxActive=Math.max(active,maxActive);const snapshot=stage
   if(delay)await new Promise(r=>setTimeout(r,delay));active--
   body=fail?{code:500,msg:'测试警情查询失败'}:{code:200,rows:Array.from({length:snapshot},(_,i)=>event(i+1)),total:snapshot}
  }
  await route.fulfill({json:body}).catch(()=>{})
 })
 const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message))
 await page.goto(base+'/fire/dashboard',{waitUntil:'domcontentloaded'})
 await page.waitForFunction(()=>window.__emitAlarm!==undefined)
 while(alarmCalls<1)await page.waitForTimeout(20)
 assert.equal(alarmCalls,1,'map must not make a second initial alarm request')
 assert.equal(await page.evaluate(()=>window.__streamAuthorization),'Bearer fixture-only')
 stage=1;await page.evaluate(()=>window.__emitAlarm())
 await page.locator('.alarm-list').getByText('实时火警测试1',{exact:true}).waitFor({timeout:1800}).catch(async()=>{await page.getByText('实时火警测试1',{exact:true}).first().waitFor({timeout:1800})})
 assert.equal(hostStatsCalls,1);assert.equal(placeCalls,1);checks.push('SSE updates alarms before slow resource requests finish; map makes no duplicate alarm query')
 release();await page.waitForTimeout(200)
 const beforePolling=alarmCalls;stage=2
 await page.getByText('实时火警测试2',{exact:true}).first().waitFor({timeout:4500})
 assert.ok(alarmCalls>beforePolling);assert.equal(placeCalls,1);assert.equal(hostStatsCalls,1);checks.push('3-second polling updates alarms without reloading static resources')
 delay=800;stage=3
 for(let i=0;i<12;i++)await page.evaluate(()=>window.__emitAlarm())
 await page.getByText('实时火警测试3',{exact:true}).first().waitFor({timeout:3000})
 assert.equal(maxActive,1);checks.push('notification bursts coalesce with no overlapping alarm requests')
 delay=0;fail=true;await page.evaluate(()=>window.__emitAlarm())
 await page.locator('.fire-error').filter({hasText:'测试警情查询失败'}).waitFor()
 assert.ok(await page.getByText('实时火警测试3',{exact:true}).count()>0)
 fail=false;stage=4
 await page.getByText('实时火警测试4',{exact:true}).first().waitFor({timeout:4500})
 checks.push('failed refresh retains prior alarms and automatically recovers through polling')
 await page.evaluate(()=>window.__endStream());stage=5
 await page.getByText('实时火警测试5',{exact:true}).first().waitFor({timeout:4500})
 checks.push('polling continues after stream disconnect')
 await page.goto(base+'/fire/inspections',{waitUntil:'domcontentloaded'})
 const stoppedCalls=alarmCalls;await page.waitForTimeout(3500);assert.equal(alarmCalls,stoppedCalls)
 checks.push('leaving dashboard stops alarm polling and stream lifecycle')
 assert.deepEqual(errors,[])
 fs.mkdirSync('docs/verification',{recursive:true});fs.writeFileSync('docs/verification/alarm-refresh-browser.json',JSON.stringify({actualVue:true,fixtureHTTP:true,fixtureSSE:true,checks,alarmCalls,maxActive,errors},null,2))
 console.log(checks.join('\n'))
 }finally{release();await browser?.close();server.kill()}
})().catch(e=>{console.error(e);process.exitCode=1})
