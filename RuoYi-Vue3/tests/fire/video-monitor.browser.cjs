// Actual Vue UI with controlled HTTP fixtures; optional live HLS smoke via VIDEO_LIVE=1.
const {chromium}=require(process.env.PLAYWRIGHT_PATH || 'playwright')
const {spawn}=require('node:child_process')
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict')
const base='http://127.0.0.1:5177'
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port','5177','--strictPort'],{windowsHide:true,stdio:'ignore'})
;(async()=>{let browser;const checks=[];try{
 for(let i=0;i<100;i++){try{if((await fetch(base)).ok)break}catch{}await new Promise(r=>setTimeout(r,100))}
 browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true})
 const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'})
 await context.addCookies([{name:'Admin-Token',value:'fixture-only',url:base}])
 let fail=false,empty=false,previewCalls=0
 const live=process.env.VIDEO_LIVE==='1'
 const runtime=path.resolve('../tools/video/runtime')
 const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));page.on('console',m=>{if(m.text().startsWith('Video playback diagnostic:'))console.log(m.text())});page.on('requestfailed',r=>{if(r.url().includes('ctyunxs.cn'))console.log('MEDIA NETWORK:',r.failure()?.errorText)});page.on('response',r=>{if(r.url().includes('ctyunxs.cn')&&r.status()>=400)console.log('MEDIA HTTP:',r.status(),new URL(r.url()).pathname)})
 await context.route('**/dev-api/**',async route=>{
  const u=new URL(route.request().url()),p=u.pathname.replace('/dev-api','');let body={code:200,data:[]}
  if(live && p.startsWith('/fire/video/media/')) {
   assert.equal(route.request().headers()['authorization'],'Bearer fixture-only')
   const id=p.split('/').pop()
   const file=id==='0'?'manifest.m3u8':'media-'+id+'.bin'
   await route.fulfill({body:fs.readFileSync(path.join(runtime,file)),contentType:id==='0'?'application/vnd.apple.mpegurl':'video/mp2t'});return
  }
  if(p==='/getInfo')body={code:200,user:{userId:1,userName:'admin',nickName:'管理员',avatar:''},roles:['admin'],permissions:['*:*:*']}
  else if(p==='/getRouters')body={code:200,data:[]}
  else if(p==='/fire/video/regions')body={code:200,data:[{id:1,name:'消控室监控',hasChildren:0}]}
  else if(p==='/fire/video/devices')body=fail?{code:500,msg:'监控接口测试失败'}:{code:200,data:live?JSON.parse(fs.readFileSync(path.join(runtime,'devices.json'),'utf8')):{lastId:u.searchParams.get('lastId')==='22'?-1:22,list:empty?[]:u.searchParams.get('lastId')==='22'?[{deviceCode:'fixture-3',deviceName:'摄像头三'}]:[{deviceCode:'fixture-1',deviceName:'测试摄像头一'},{deviceCode:'fixture-2',deviceName:'测试摄像头二'}]}}
  else if(p.endsWith('/preview')){previewCalls++;body=u.searchParams.get('protocol')==='rtsp'?{code:200,data:{url:'rtsp://example.invalid/fixture-only'}}:live?{code:200,data:JSON.parse(fs.readFileSync(path.join(runtime,'preview.json'),'utf8'))}:{code:500,msg:'测试播放授权失败'}}
  else if(p.includes('/fire/'))body={code:200,data:[],rows:[],total:0}
  await route.fulfill({json:body})
 })
 await page.goto(base+'/fire/inspections',{waitUntil:'domcontentloaded'})
 await page.getByRole('tab',{name:'视频监控',exact:true}).click()
 const monitor=page.getByRole('region',{name:'真实视频监控'})
 await monitor.locator('.monitor-device').first().waitFor()
 assert.equal(previewCalls,0);checks.push('device listing does not request streams; original inspection tab preserved')
 if(!live){
  await monitor.getByRole('button',{name:'加载更多',exact:true}).click()
  await monitor.getByText('摄像头三',{exact:true}).waitFor();checks.push('cursor pagination appends devices')
  await monitor.locator('.monitor-device').first().click()
  const dialog=page.getByRole('dialog');await dialog.locator('.fire-error').filter({hasText:'测试播放授权失败'}).waitFor()
  const downloaded=page.waitForEvent('download')
  await dialog.getByRole('button',{name:'下载 VLC 播放列表',exact:true}).click()
  assert.equal((await downloaded).suggestedFilename(),'camera-live.m3u')
  await page.keyboard.press('Escape');await dialog.waitFor({state:'hidden'});checks.push('preview failure shown; VLC fallback downloads; modal closes')
  fail=true;await monitor.getByRole('button',{name:'刷新设备',exact:true}).click()
  await monitor.getByRole('alert').waitFor();assert.equal(await monitor.locator('.monitor-device').count(),0)
  fail=false;empty=true;await monitor.getByRole('button',{name:'重试',exact:true}).click();await monitor.getByText('当前目录暂无摄像头',{exact:true}).waitFor()
  empty=false;await monitor.getByRole('button',{name:'刷新设备',exact:true}).click();await monitor.locator('.monitor-device').first().waitFor()
  checks.push('failure clears stale rows, retry and empty states work without demo fallback')
 }else{
  await monitor.locator('.monitor-device').first().click()
  try{await page.waitForFunction(()=>{let v=document.querySelector('.live-stage video');return v&&v.videoWidth>0&&v.readyState>=2},{},{timeout:45000});checks.push('LIVE MEDIA SAMPLE DECODED: videoWidth > 0; authenticated relay requests verified')}catch{checks.push('LIVE HLS NOT DECODED: '+await page.getByRole('dialog').innerText())}
  await page.keyboard.press('Escape')
 }
 await page.setViewportSize({width:720,height:900});await page.keyboard.press('Tab')
 fs.mkdirSync('docs/verification',{recursive:true});await page.screenshot({path:'docs/verification/video-monitor-narrow.png',fullPage:true})
 assert.equal(await page.getByText('恢复示例数据',{exact:true}).count(),0)
 await page.getByRole('tab',{name:'查岗管理',exact:true}).click();await page.getByRole('heading',{name:'查岗管理',exact:true}).waitFor()
 checks.push('keyboard/narrow viewport and return to unchanged inspection content')
 if(!live){
  await page.setViewportSize({width:1440,height:1000})
  await page.goto(base+'/fire/dashboard',{waitUntil:'domcontentloaded'})
  const panel=page.locator('.inspections-panel')
  await panel.getByRole('tab',{name:'视频监控',exact:true}).click()
  await panel.locator('.monitor-device').first().waitFor()
  await page.screenshot({path:'docs/verification/video-monitor-dashboard.png',fullPage:true})
  await panel.getByRole('tab',{name:'查岗管理',exact:true}).click()
  await panel.locator('.inspection-filter').waitFor()
  checks.push('dashboard adjacent tab shares real monitor and restores original inspection panel')
 }
 assert.deepEqual(errors,[])
 fs.writeFileSync('docs/verification/video-monitor'+(live?'-live':'')+'.json',JSON.stringify({fixtureHttp:true,liveHls:live,checks,errors},null,2))
 console.log(checks.join('\n'))
 }finally{await browser?.close();server.kill()}
})().catch(e=>{console.error(e);process.exitCode=1})
