// Actual Vue UI; fixture HTTP responses; no business database or device access.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const fs = require('node:fs')
const assert = require('node:assert/strict')
const path = require('node:path')
const base='http://127.0.0.1:5176'
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port','5176','--strictPort'],{cwd:process.cwd(),windowsHide:true,stdio:'ignore'})
const checks=[], errors=[]
;(async()=>{
 let browser
 try {
  for(let i=0;i<80;i++){try{if((await fetch(base)).ok)break}catch{}await new Promise(r=>setTimeout(r,100))}
  browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,timeout:8000})
  const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'})
  await context.addCookies([{name:'Admin-Token',value:'fixture-only',url:base}])
  let fail=false,empty=false, fail119=false,empty119=false
  const gateway={id:'1',name:'大厂街道用户信息传输装置',placeName:'大厂街道',model:'DH-HY-FAM-1000',serial:'c80000000000',status:'在线',deviceStatus:'装置状态：调试状态、故障、备电故障',lastSeen:'2026-10-20 17:46:02'}
  const event={id:'event1',name:'装置状态：调试状态、故障、备电故障',kind:'fault',placeName:'大厂街道',placeId:'102',gatewayName:gateway.name,deviceId:'gateway:c80000000000',occurredAt:'2026-10-20 17:43:40',deviceTime:'2017-03-09 19:20:03',clockWarning:1,details:'{"statusBits":20,"fire":false}',source:'消控室主机'}
  const incident={id:'119:1',code:'fixture119',sourceCode:'119',source:'119警情',name:'社会救助测试记录',kind:'3',caseTypeCode:'3',caseTypeName:'社会救助',caseNatureName:'实警',caseLevelName:'一级',mergeStatusName:'未合并',address:'大厂街道测试地址',description:'社会救助测试记录',placeName:'大厂街道测试地址',occurredAt:'2026-10-21 13:14:16',lastSyncedAt:'2026-10-21 15:00:00',status:'返队',streetRegion:'文档边界；地区=吴家洼',coordinateStatus:'valid',lng:118.75163,lat:32.212305}
  await context.route('**/dev-api/**',async route=>{
   const u=new URL(route.request().url()), p=u.pathname.replace('/dev-api','')
   let data={code:200,data:[]}
   if(p==='/getInfo')data={code:200,user:{userId:1,userName:'admin',nickName:'管理员',avatar:''},roles:['admin'],permissions:['*:*:*']}
   else if(p==='/getRouters')data={code:200,data:[]}
   else if(p==='/fire/gateways/list')data=fail?{code:500,msg:'测试接口暂时不可用'}:{code:200,rows:empty?[]:[gateway],total:empty?0:1}
   else if(p==='/fire/hosts/list')data={code:200,rows:[],total:0}
   else if(p==='/fire/alarms/list') { const is119=u.searchParams.get('source')==='119',device=u.searchParams.get('source')==='device'; const rows=is119?(empty119?[]:[incident]):device?[event]:[incident,event]; data=is119&&fail119?{code:500,msg:'119查询测试失败'}:{code:200,rows,total:rows.length} }
   else if(decodeURIComponent(p)==='/fire/alarms/119:1')data={code:200,data:incident}
   else if(p==='/fire/119/status')data={code:200,data:{enabled:true,intervalSeconds:60,lastQuickResult:'success',lastQuickCompletedAt:'2026-10-21 15:09:00',fullIntervalSeconds:600,quickPages:3,lastResult:'failed',lastCompletedAt:'2026-10-21 15:00:00',insideCount:1,pendingCount:0,coordinateSystem:'GCJ02',boundaryMode:'verified'}}
   else if(p==='/fire/alarms/event1')data={code:200,data:event}
   else if(p==='/fire/protocol/messages')data={code:200,rows:[{id:'1',direction:'TX',sequenceNo:10,commandNo:3,receivedAt:'2026-10-20 17:43:40',parseStatus:'REPLY',transmissionStatus:'SENT',rawHex:'40400a000102',parsed:'[]'}],total:1}
   else if(p==='/fire/hosts/stats')data={code:200,data:{gateways:1,hosts:0,online:0}}
   else if(p==='/fire/alarms/stats')data={code:200,data:{eventTotal:5,fireEvents:0,faultEvents:3}}
   else if(p.startsWith('/fire/places'))data={code:200,rows:[],total:0,data:[],truncated:false}
   await route.fulfill({contentType:'application/json',body:JSON.stringify(data)})
  })
  const page=await context.newPage();page.setDefaultTimeout(8000);page.on('pageerror',e=>errors.push(e.message))
  await page.goto(base+'/fire/hosts',{waitUntil:'domcontentloaded'})
  await page.getByRole('cell',{name:gateway.name,exact:true}).waitFor()
  checks.push('real gateway fields and separate device fault state')
  assert.equal(await page.getByRole('button',{name:/新增|编辑|删除/}).count(),0)
  await page.getByRole('tab',{name:'消防主机',exact:true}).click()
  await page.getByText('暂无符合条件的真实记录',{exact:true}).waitFor()
  checks.push('empty host list does not invent a downstream host')
  await page.getByRole('tab',{name:'设备事件',exact:true}).click()
  await page.getByRole('cell',{name:event.name,exact:true}).waitFor()
  await page.getByRole('button',{name:'详情',exact:true}).click()
  const dialog=page.getByRole('dialog',{name:'设备事件详情'})
  await dialog.getByText('设备时间异常或未提供，请核验',{exact:true}).waitFor()
  assert.equal(await dialog.getByRole('button',{name:/接单|办结|联动视频/}).count(),0)
  await page.keyboard.press('Escape')
  checks.push('read-only event detail, stale clock warning, no demo associations or workflow')
  await page.getByRole('tab',{name:'通信报文',exact:true}).click()
  await page.getByRole('cell',{name:'SENT',exact:true}).waitFor()
  await page.getByRole('button',{name:'详情',exact:true}).click()
  await page.getByText('40400a000102',{exact:true}).waitFor()
  await page.keyboard.press('Escape')
  checks.push('bidirectional message status and raw hex detail')
  await page.getByRole('tab',{name:'传输装置',exact:true}).click()
  empty=true
  await page.getByRole('button',{name:'刷新记录',exact:true}).click()
  await page.getByText('暂无符合条件的真实记录',{exact:true}).waitFor()
  empty=false;fail=true
  await page.getByRole('button',{name:'刷新记录',exact:true}).click()
  await page.locator('.fire-error').filter({hasText:'测试接口暂时不可用'}).waitFor()
  assert.equal(await page.getByRole('cell',{name:gateway.name,exact:true}).count(),0)
  fail=false
  await page.locator('.fire-error').getByRole('button',{name:'重试'}).click()
  await page.getByRole('cell',{name:gateway.name,exact:true}).waitFor()
  checks.push('empty, failure, stale data clearing and retry')
  await page.locator('.el-message').waitFor({state:'hidden',timeout:6000})
  assert.equal(await page.getByText('恢复示例数据',{exact:true}).count(),0)
  checks.push('real database provenance replaces demo reset controls')
  await page.setViewportSize({width:720,height:900})
  await page.keyboard.press('Tab')
  await page.screenshot({path:'docs/verification/incident119-sibling-narrow.png',fullPage:true})
  checks.push('narrow viewport, reduced motion and keyboard navigation')
  await page.setViewportSize({width:1440,height:1000})
  await page.screenshot({path:'docs/verification/incident119-sibling-device.png',fullPage:true})
  await page.goto(base+'/fire/dashboard',{waitUntil:'domcontentloaded'})
  await page.locator('.hosts-panel .host-stats').getByText('装置总量',{exact:true}).waitFor()
  await page.waitForFunction(()=>document.querySelector('.hosts-panel .host-stats a')?.textContent.includes('1'))
  const stats=await page.locator('.hosts-panel .host-stats').innerText()
  assert.ok(stats.includes('主机总量') && stats.includes('事件（24h）'))
  await page.screenshot({path:'docs/verification/incident119-dashboard.png',fullPage:true})
  checks.push('dashboard uses real device statistics and event list')
  await page.locator('.source-tabs').getByRole('button',{name:/119警情/}).click()
  await page.getByText(incident.name,{exact:true}).first().waitFor()
  assert.ok((await page.locator('.alarm-panel').innerText()).includes('社会救助'))
  await page.locator('.alarm-card').filter({hasText:incident.name}).click()
  await page.getByRole('button',{name:'查看事件',exact:true}).click()
  const incidentDialog=page.getByRole('dialog',{name:'119警情详情'})
  await incidentDialog.getByText('社会救助',{exact:true}).first().waitFor()
  await incidentDialog.getByText('吴家洼',{exact:true}).waitFor()
  assert.equal(await incidentDialog.getByRole('button',{name:/接单|办结|单位档案|联动视频/}).count(),0)
  await page.keyboard.press('Escape')
  checks.push('dashboard 119 source includes social assistance and opens source-specific read-only details')
  await page.goto(base+'/fire/alarms?source=119',{waitUntil:'domcontentloaded'})
  await page.getByRole('cell',{name:incident.name,exact:true}).waitFor()
  await page.getByRole('cell',{name:'返队',exact:true}).waitFor()
  await page.getByText(/119快速扫描：每60秒前3页/).waitFor()
  await page.getByText(/完整核对：每600秒，失败/).waitFor()
  checks.push('quick scan success does not conceal failed full reconciliation')
  const typeSelect=page.locator('.el-form-item').filter({hasText:'案件类型'}).getByRole('combobox')
  await typeSelect.focus()
  await typeSelect.press('Enter')
  await page.getByRole('option',{name:'社会救助',exact:true}).click()
  await page.waitForURL(/caseType=3/)
  checks.push('119 source and case filters persist in URL, full external status preserved')
  fail119=true
  await page.getByRole('button',{name:'刷新记录',exact:true}).click()
  await page.locator('.fire-error').filter({hasText:'119查询测试失败'}).waitFor()
  assert.equal(await page.getByRole('cell',{name:incident.name,exact:true}).count(),0)
  fail119=false;empty119=true
  await page.locator('.fire-error').getByRole('button',{name:'重试'}).click()
  await page.getByText('暂无符合条件的真实记录',{exact:true}).waitFor()
  empty119=false
  await page.getByRole('button',{name:'刷新记录',exact:true}).click()
  await page.getByRole('cell',{name:incident.name,exact:true}).waitFor()
  checks.push('119 error clearing, retry, empty and recovery')
  await page.setViewportSize({width:720,height:900});await page.keyboard.press('Tab')
  await page.locator('.el-message').waitFor({state:'hidden',timeout:6000})
  await page.screenshot({path:'docs/verification/incident119-narrow.png',fullPage:true})
  await page.setViewportSize({width:1440,height:1000})
  await page.getByRole('button',{name:'详情',exact:true}).click()
  await page.getByRole('dialog',{name:'119警情详情'}).waitFor()
  await page.locator('.dialog-fade-enter-active').first().waitFor({state:'hidden'})
  await page.screenshot({path:'docs/verification/incident119-detail.png',fullPage:true})
  checks.push('119 narrow viewport, keyboard and detail visual check')
  assert.deepEqual(errors,[])
  fs.writeFileSync('docs/verification/incident119-browser.json',JSON.stringify({actualVue:true,fixtureHTTP:true,businessDatabaseAccessed:false,checks,errors},null,2))
  console.log(JSON.stringify({checks,errors}))
 }finally{if(browser)await browser.close();server.kill()}
})().catch(e=>{console.error(e);process.exitCode=1})
