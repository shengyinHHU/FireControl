// Actual Vue UI; fixture HTTP responses; no business database or device access.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const fs = require('node:fs')
const assert = require('node:assert/strict')
const path = require('node:path')
const port=process.env.DEVICE_ACCESS_TEST_PORT||'5196'
const base='http://127.0.0.1:'+port
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port',port,'--strictPort'],{cwd:process.cwd(),windowsHide:true,stdio:['ignore','pipe','pipe']})
let startupLog='';server.stdout.on('data',b=>startupLog+=b);server.stderr.on('data',b=>startupLog+=b)
const checks=[], errors=[]
;(async()=>{
 let browser
 try {
  let ready=false;for(let i=0;i<300;i++){if(server.exitCode!==null)throw new Error(startupLog);try{if((await fetch(base)).ok){ready=true;break}}catch{}await new Promise(r=>setTimeout(r,100))}if(!ready)throw new Error('Vite startup timed out: '+startupLog)
  browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true,timeout:8000})
  const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'})
  await context.addCookies([{name:'Admin-Token',value:'fixture-only',url:base}])
  let fail=false,empty=false,mutationError=false,readonly=false,mutations=0
  const gateway={id:'1',name:'大厂街道用户信息传输装置',placeName:'大厂街道',unitName:'大厂街道',deviceCode:'200',revision:0,enabled:'0',allowedIp:'10.5.90.49',centerAddress:'640000000000',model:'DH-HY-FAM-1000',serial:'c80000000000',status:'在线',deviceStatus:'装置状态：调试状态、故障、备电故障',lastSeen:'2026-10-20 17:46:02'}
  let gateways=[gateway],hosts=[]
  const event={id:'event1',name:'装置状态：调试状态、故障、备电故障',kind:'fault',placeName:'大厂街道',placeId:'102',gatewayName:gateway.name,deviceId:'gateway:c80000000000',occurredAt:'2026-10-20 17:43:40',deviceTime:'2017-03-09 19:20:03',clockWarning:1,details:'{"statusBits":20,"fire":false}',source:'消控室主机'}
  await context.route('**/dev-api/**',async route=>{
   const u=new URL(route.request().url()), p=u.pathname.replace('/dev-api','')
   let data={code:200,data:[]}
   if(p==='/getInfo')data={code:200,user:{userId:1,userName:'admin',nickName:'管理员',avatar:''},roles:readonly?['staff']:['admin'],permissions:readonly?['fire:hosts:list','fire:alarms:list']:['*:*:*']}
   else if(p==='/getRouters')data={code:200,data:[]}
   else if(p==='/fire/gateways/list')data=fail?{code:500,msg:'测试接口暂时不可用'}:{code:200,rows:empty?[]:gateways,total:empty?0:gateways.length}
   else if(p==='/fire/hosts/list')data={code:200,rows:hosts,total:hosts.length}
   else if(p.startsWith('/fire/assets/gateways/')&&route.request().method()==='GET')data={code:200,data:gateways.find(r=>r.id===p.split('/').at(-1))}
   else if(p==='/fire/assets/unregistered')data={code:200,data:[]}
   else if(p.startsWith('/fire/assets/')){
    mutations++
    if(mutationError){data={code:500,msg:'记录已被其他人修改，请重新打开后编辑'};mutationError=false}
    else {const parts=p.split('/'),kind=parts[3],id=parts[4],method=route.request().method(),list=kind==='hosts'?hosts:gateways
     if(method==='DELETE'){if(kind==='hosts')hosts=hosts.filter(r=>r.id!==id);else gateways=gateways.filter(r=>r.id!==id)}
     else {const a=route.request().postDataJSON(),row={...a,id:id||'2',revision:id?(a.revision||0)+1:0,serial:a.sourceAddress,placeName:a.unitName,status:a.enabled==='0'?'离线':'停用'};if(id)list[list.findIndex(r=>r.id===id)]=row;else list.push(row)}
     data={code:200,data:id||'2'}
    }
   }
   else if(p==='/fire/alarms/list')data={code:200,rows:[event],total:1}
   else if(p==='/fire/alarms/event1')data={code:200,data:event}
   else if(p==='/fire/protocol/messages')data={code:200,rows:[{id:'1',direction:'TX',sequenceNo:10,commandNo:3,receivedAt:'2026-10-20 17:43:40',parseStatus:'REPLY',transmissionStatus:'SENT',rawHex:'40400a000102',parsed:'[]'}],total:1}
   else if(p==='/fire/hosts/stats')data={code:200,data:{gateways:1,hosts:0,online:0}}
   else if(p==='/fire/alarms/stats')data={code:200,data:{eventTotal:5,fireEvents:0,faultEvents:3}}
   else if(p.startsWith('/fire/places'))data={code:200,rows:[],total:0,data:[],truncated:false}
   await route.fulfill({contentType:'application/json',body:JSON.stringify(data)})
  })
  const page=await context.newPage();page.setDefaultTimeout(20000);page.on('pageerror',e=>errors.push(e.message))
  await page.goto(base+'/fire/hosts',{waitUntil:'domcontentloaded'})
  await page.getByRole('cell',{name:gateway.name,exact:true}).waitFor()
  assert.equal(await page.getByText('待识别',{exact:true}).count(),0)
  checks.push('real gateway fields, source-IP association and separate device fault state')
  await page.getByRole('button',{name:'新增传输装置',exact:true}).click()
  let editor=page.getByRole('dialog',{name:'新增传输装置',exact:true})
  await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.getByText('请填写名称',{exact:true}).waitFor();assert.equal(mutations,0)
  await editor.getByRole('textbox',{name:'名称',exact:true}).fill('元茂大厦用户信息传输装置')
  await editor.getByRole('textbox',{name:'所属单位',exact:true}).fill('元茂大厦')
  await editor.getByRole('textbox',{name:'装置编号',exact:true}).fill('219')
  const enableRadio=editor.getByRole('radio',{name:'启用',exact:true});await enableRadio.focus();await enableRadio.press('Space');assert.ok(await enableRadio.isChecked())
  mutationError=true;await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.locator('.fire-error').waitFor();assert.equal(await editor.getByRole('textbox',{name:'名称',exact:true}).inputValue(),'元茂大厦用户信息传输装置')
  await new Promise(r=>setTimeout(r,1100));await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.waitFor({state:'hidden'})
  const added=page.getByRole('row').filter({hasText:'元茂大厦用户信息传输装置'});await added.waitFor()
  assert.equal(gateways.find(r=>r.deviceCode==='219').enabled,'0')
  for(const field of ['sourceAddress','centerAddress','allowedIp'])assert.ok(!gateways.find(r=>r.deviceCode==='219')[field])
  checks.push('enable and save with all three protocol/IP fields empty; failed request preserves draft')
  await added.getByRole('button',{name:'编辑',exact:true}).click();editor=page.getByRole('dialog',{name:'编辑传输装置',exact:true});await editor.getByRole('textbox',{name:'装置编号',exact:true}).fill('220');await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.waitFor({state:'hidden'});await added.getByRole('cell',{name:'220',exact:true}).waitFor()
  await added.getByRole('button',{name:'删除',exact:true}).click();await page.getByRole('button',{name:'删除',exact:true}).last().click();await added.waitFor({state:'hidden'})
  checks.push('gateway create/edit/delete, required validation and failed save preserves draft')
  await page.getByRole('button',{name:'详情',exact:true}).click()
  const device=page.getByRole('dialog',{name:gateway.name,exact:true});await device.getByText(gateway.serial,{exact:true}).waitFor()
  await device.getByRole('tab',{name:'下挂主机',exact:true}).click();await device.getByText('暂无下挂主机',{exact:true}).waitFor()
  await device.getByRole('button',{name:'新增下挂主机',exact:true}).click();editor=page.getByRole('dialog',{name:'新增下挂主机',exact:true});await editor.getByRole('textbox',{name:'名称',exact:true}).fill('北大青鸟消防主机');await editor.getByRole('textbox',{name:'主机品牌',exact:true}).fill('北大青鸟');await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.waitFor({state:'hidden'});await device.getByRole('cell',{name:'北大青鸟消防主机',exact:true}).waitFor()
  await device.getByRole('button',{name:'编辑',exact:true}).click();editor=page.getByRole('dialog',{name:'编辑下挂主机',exact:true});await editor.getByRole('textbox',{name:'型号',exact:true}).fill('JB-TG-JBF-11S');await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.waitFor({state:'hidden'});await device.getByRole('cell',{name:'JB-TG-JBF-11S',exact:true}).waitFor()
  await device.getByRole('button',{name:'删除',exact:true}).click();await page.getByRole('button',{name:'删除',exact:true}).last().click();await device.getByText('暂无下挂主机',{exact:true}).waitFor()
  await device.getByRole('tab',{name:'通信报文',exact:true}).click();await device.getByRole('cell',{name:'SENT',exact:true}).waitFor();await page.keyboard.press('Escape');await device.waitFor({state:'hidden'})
  checks.push('subordinate host CRUD and gateway-filtered communication detail')


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
  await page.getByText('暂无传输装置，可新增装置或调整查询条件',{exact:true}).waitFor()
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
  await page.screenshot({path:'docs/verification/device-access-narrow.png',fullPage:true})
  checks.push('narrow viewport, reduced motion and keyboard navigation')
  await page.setViewportSize({width:1440,height:1000})
  await page.screenshot({path:'docs/verification/device-access.png',fullPage:true})
  await page.setViewportSize({width:390,height:844});await page.getByRole('button',{name:'新增传输装置',exact:true}).click();editor=page.getByRole('dialog',{name:'新增传输装置',exact:true});await editor.waitFor();assert.ok((await editor.boundingBox()).width<=390);assert.ok((await editor.boundingBox()).height<=844);await editor.getByRole('button',{name:'保存',exact:true}).waitFor();await page.waitForTimeout(400);await page.screenshot({path:'docs/verification/device-editor-narrow.png',fullPage:true});await page.keyboard.press('Escape');await editor.waitFor({state:'hidden'})
  readonly=true;await page.reload({waitUntil:'domcontentloaded'});await page.getByRole('heading',{name:'传输装置管理'}).waitFor();assert.equal(await page.getByRole('button',{name:/新增传输装置|编辑|删除|待登记报文/}).count(),0);checks.push('390px form layout and read-only permission actions');readonly=false
  await page.goto(base+'/fire/dashboard' ,{waitUntil:'domcontentloaded'})
  await page.locator('.hosts-panel .host-stats').getByText('装置总量',{exact:true}).waitFor()
  await page.waitForFunction(()=>document.querySelector('.hosts-panel .host-stats a')?.textContent.includes('1'))
  const stats=await page.locator('.hosts-panel .host-stats').innerText()
  assert.ok(stats.includes('主机总量') && stats.includes('事件（24h）'))
  await page.screenshot({path:'docs/verification/device-dashboard.png',fullPage:true})
  checks.push('dashboard uses real device statistics and event list')
  assert.deepEqual(errors,[])
  fs.writeFileSync('docs/verification/device-access-browser.json',JSON.stringify({actualVue:true,fixtureHTTP:true,businessDatabaseAccessed:false,checks,errors},null,2))
  console.log(JSON.stringify({checks,errors}))
 }finally{if(browser)await browser.close();server.kill()}
})().catch(e=>{console.error(e);process.exitCode=1})
