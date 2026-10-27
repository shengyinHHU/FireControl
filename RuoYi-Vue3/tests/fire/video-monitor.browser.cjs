// Actual Vue UI with controlled HTTP fixtures; optional captured provider FLV decode via VIDEO_LIVE=1 (not a live network test).
const {chromium}=require(process.env.PLAYWRIGHT_PATH || 'playwright')
const {spawn}=require('node:child_process')
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict')
const base='http://127.0.0.1:5177'
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port','5177','--strictPort'],{windowsHide:true,stdio:'ignore'})
;(async()=>{let browser;const checks=[];try{
 for(let i=0;i<100;i++){try{if((await fetch(base)).ok)break}catch{}await new Promise(r=>setTimeout(r,100))}
 browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true})
 const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'})
 await context.addInitScript(() => {
  window.audioCheck={started:0,energy:0,state:'none'}
  const NativeAudio=window.AudioContext
  window.AudioContext=class extends NativeAudio {
   constructor(...args){super(...args);this.addEventListener('statechange',()=>{window.audioCheck.state=this.state})}
   createBufferSource(){const source=super.createBufferSource();const start=source.start.bind(source);source.start=(...args)=>{window.audioCheck.started++;if(source.buffer){const samples=source.buffer.getChannelData(0);for(const value of samples)window.audioCheck.energy+=Math.abs(value)}return start(...args)};return source}
  }
 })
 await context.addCookies([{name:'Admin-Token',value:'fixture-only',url:base}])
 let fail=false,empty=false,previewCalls=0,flvSuccess=false,mediaRequests=0,closedStreams=0
 const live=process.env.VIDEO_LIVE==='1'||process.env.VIDEO_FLV_RELAY==='1'
 const relay=process.env.VIDEO_FLV_RELAY==='1'
 const runtime=path.resolve('../tools/video/runtime')
 const page=await context.newPage();const errors=[];page.on('pageerror',e=>{errors.push(e.message);console.log(e.stack)});page.on('console',m=>{if(m.text().startsWith('Video playback diagnostic:'))console.log(m.text())});page.on('requestfailed',r=>{if(r.url().includes('ctyunxs.cn'))console.log('MEDIA NETWORK:',r.failure()?.errorText)});page.on('response',r=>{if(r.url().includes('ctyunxs.cn')&&r.status()>=400)console.log('MEDIA HTTP:',r.status(),new URL(r.url()).pathname)})
 await context.route('**/dev-api/**',async route=>{
  const u=new URL(route.request().url()),p=u.pathname.replace('/dev-api','');let body={code:200,data:[]}
  if(p.startsWith('/fire/video/flv/')) {
   assert.equal(route.request().headers()['authorization'],'Bearer fixture-only')
   if(route.request().method()==='DELETE'){closedStreams++;if(relay){await fetch('http://127.0.0.1:5186'+p,{method:'DELETE',headers:{Authorization:'Bearer fixture-only'}})}await route.fulfill({json:{code:200}});return}
   if(relay){await route.continue({url:'http://127.0.0.1:5186'+p});return}
   mediaRequests++
   await route.fulfill({body:live?fs.readFileSync(path.join(runtime,'flv-sample.flv')):Buffer.from([70,76,86,1,1,0,0,0,9,0,0,0,0]),contentType:'video/x-flv'});return
  }
  if(p==='/getInfo')body={code:200,user:{userId:1,userName:'admin',nickName:'管理员',avatar:''},roles:['admin'],permissions:['*:*:*']}
  else if(p==='/getRouters')body={code:200,data:[]}
  else if(p==='/fire/video/regions')body={code:200,data:[{id:1,name:'消控室监控',hasChildren:0}]}
  else if(p==='/fire/video/devices')body=fail?{code:500,msg:'监控接口测试失败'}:{code:200,data:{lastId:u.searchParams.get('lastId')==='22'?-1:22,list:empty?[]:u.searchParams.get('lastId')==='22'?[{deviceCode:'fixture-3',deviceName:'摄像头三'}]:[{deviceCode:'fixture-1',deviceName:'测试摄像头一'},{deviceCode:'fixture-2',deviceName:'测试摄像头二'}]}}
  else if(p.endsWith('/preview')){if(relay){const response=await fetch('http://127.0.0.1:5186/preview',{headers:{Authorization:'Bearer fixture-only'}});await route.fulfill({json:await response.json()});return}previewCalls++;const protocol=u.searchParams.get('protocol');assert.ok(['flv','rtsp'].includes(protocol));body=protocol==='rtsp'?{code:200,data:{url:'rtsp://example.invalid/fixture-only'}}:(flvSuccess||live)?{code:200,data:{url:'/fire/video/flv/fixture-'+previewCalls,protocol:'flv',requiresAuth:true}}:{code:500,msg:'测试播放授权失败'}}
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
  flvSuccess=true
  await monitor.locator('.monitor-device').first().click()
  await page.waitForFunction(()=>!!document.querySelector('.live-stage video')?.src)
  for(let until=Date.now()+10000;mediaRequests<1&&Date.now()<until;)await page.waitForTimeout(100)
  assert.ok(mediaRequests>=1)
  // Controlled UI readiness only; actual provider sample decode is tested in VIDEO_LIVE mode.
  await dialog.locator('video').evaluate(v=>Object.defineProperty(v,'videoWidth',{configurable:true,get:()=>640}))
  await dialog.locator('video').dispatchEvent('playing')
  await dialog.getByRole('button',{name:'重新连接',exact:true}).click()
  for(let until=Date.now()+10000;(mediaRequests<2||closedStreams<1)&&Date.now()<until;)await page.waitForTimeout(100)
  assert.ok(mediaRequests>=2&&closedStreams>=1)
  await page.keyboard.press('Escape');await dialog.waitFor({state:'hidden'})
  await page.waitForTimeout(300)
  assert.equal(closedStreams,2)
  checks.push('FLV requests carry auth; reconnect and close destroy MSE and delete owned streams; no HLS/RTC fallback')
  fail=true;await monitor.getByRole('button',{name:'刷新设备',exact:true}).click()
  await monitor.getByRole('alert').waitFor();assert.equal(await monitor.locator('.monitor-device').count(),0)
  fail=false;empty=true;await monitor.getByRole('button',{name:'重试',exact:true}).click();await monitor.getByText('当前目录暂无摄像头',{exact:true}).waitFor()
  empty=false;await monitor.getByRole('button',{name:'刷新设备',exact:true}).click();await monitor.locator('.monitor-device').first().waitFor()
  checks.push('failure clears stale rows, retry and empty states work without demo fallback')
 }else{
  await monitor.locator('.monitor-device').first().click()
  try{await page.waitForFunction(()=>{let v=document.querySelector('.live-stage video');return v&&v.videoWidth>0&&v.readyState>=2},{},{timeout:45000});if(relay)await page.waitForFunction(()=>{const v=document.querySelector('.live-stage video');return v?.currentTime>5&&v.getVideoPlaybackQuality().totalVideoFrames>50},{},{timeout:20000});checks.push({result:relay?'LIVE FLV RELAY DECODED':'PROVIDER FLV SAMPLE DECODED',video:await page.locator('.live-stage video').evaluate(v=>({width:v.videoWidth,height:v.videoHeight,frames:v.getVideoPlaybackQuality().totalVideoFrames,currentTime:v.currentTime}))});if(process.env.VIDEO_G711==='1'){
    const cdp=await context.newCDPSession(page);await cdp.send('DOM.enable');await cdp.send('CSS.enable')
    const tree=await cdp.send('DOM.getDocument',{depth:-1,pierce:true});const nativeVolume=[]
    async function inspect(node){
      const attrs=node.attributes||[]
      if(attrs.some(value=>['-webkit-media-controls-mute-button','-webkit-media-controls-volume-slider','-webkit-media-controls-volume-control-container'].includes(value))){
        const {computedStyle}=await cdp.send('CSS.getComputedStyleForNode',{nodeId:node.nodeId});nativeVolume.push({control:attrs.find(value=>value.startsWith('-webkit-media-controls-')),display:computedStyle.find(row=>row.name==='display')?.value})
      }
      for(const child of [...(node.children||[]),...(node.shadowRoots||[])])await inspect(child)
    }
    await inspect(tree.root);assert.ok(nativeVolume.length>=2);assert.ok(nativeVolume.every(row=>row.display==='none'));checks.push({nativeVolumeHidden:nativeVolume});await cdp.detach()
    const sound=page.getByRole('button',{name:'开启声音',exact:true});await sound.click();await page.waitForFunction(()=>window.audioCheck.started>5&&window.audioCheck.energy>0,{},{timeout:10000});assert.equal(await page.locator('.live-stage video').evaluate(v=>v.muted),false);checks.push({g711Audio:await page.evaluate(()=>window.audioCheck)});await page.getByRole('button',{name:'关闭声音',exact:true}).click();assert.equal(await page.locator('.live-stage video').evaluate(v=>v.muted),true)
   }await page.screenshot({path:'docs/verification/video-flv'+(process.env.VIDEO_G711==='1'?'-g711':relay?'-live':'-sample')+'.png'})}catch{throw Error('FLV SAMPLE NOT DECODED: '+await page.getByRole('dialog').innerText())}
  await page.keyboard.press('Escape');await page.getByRole('dialog').waitFor({state:'hidden'});await page.waitForTimeout(300);assert.ok(closedStreams>=1);if(process.env.VIDEO_G711==='1'){await page.waitForFunction(()=>window.audioCheck.state==='closed');checks.push('G.711 AudioContext closes with dialog')}
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
 fs.writeFileSync('docs/verification/video-monitor'+(process.env.VIDEO_G711==='1'?(relay?'-flv-g711':'-flv-g711-sample'):relay?'-flv-live':live?'-flv-sample':'')+'.json',JSON.stringify({fixtureHttp:true,providerFlvSample:live&&!relay,liveFlvRelay:relay,checks,errors},null,2))
 console.log(JSON.stringify(checks,null,2))
 }finally{await browser?.close();server.kill()}
})().catch(e=>{console.error(e);process.exitCode=1})
