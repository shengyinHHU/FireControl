// Actual Vue UI; fixture HTTP responses; no business database or device access.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const fs = require('node:fs')
const assert = require('node:assert/strict')
const path = require('node:path')
const base='http://127.0.0.1:5177'
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port','5177','--strictPort'],{cwd:process.cwd(),windowsHide:true,stdio:'ignore'})
const checks=[], errors=[]
;(async()=>{
 let browser
 try {
  for(let i=0;i<80;i++){try{if((await fetch(base)).ok)break}catch{}await new Promise(r=>setTimeout(r,100))}
  browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,timeout:8000})
  const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'})
  await context.addCookies([{name:'Admin-Token',value:'fixture-only',url:base}])
  const places=[{placeId:'1',name:'钻石星城测试',placeType:'highrise',address:'大厂街道新华路468号',longitude:118.76181,latitude:32.230323,coordinateSystem:'GCJ02'}, {placeId:'2',name:'另一个测试场所',placeType:'industry'}]
  let mode='success',slow=false
  await context.route('**/dev-api/**',async route=>{
   const p=decodeURIComponent(new URL(route.request().url()).pathname.replace('/dev-api',''));let data={code:200,data:[]}
   if(p==='/getInfo')data={code:200,user:{userId:1,userName:'admin',nickName:'管理员',avatar:''},roles:['admin'],permissions:['*:*:*']}
   else if(p==='/getRouters')data={code:200,data:[]}
   else if(p==='/fire/places/list')data={code:200,rows:places,total:2}
   else if(p.endsWith('/contacts')){const id=p.split('/')[3];if(slow&&id==='1')await new Promise(r=>setTimeout(r,450));data=mode==='error'?{code:500,msg:'联系人测试失败'}:{code:200,data:mode==='empty'?[]:[{contactId:id,contactName:id==='1'?'测试张先生':'测试李女士',phone:id==='1'?'025-12345678':'13800000000'},{contactId:id+'-second',contactName:'第二位联系人',phone:'13900000000'}]}}
   else if(/^\/fire\/places\/\d+$/.test(p))data={code:200,data:places.find(x=>x.placeId===p.split('/').pop())}
   await route.fulfill({contentType:'application/json',body:JSON.stringify(data)})
  })
  await context.route('https://www.jnrhtx.top/**',r=>r.fulfill({contentType:'text/html',body:'<!doctype html><title>拨号测试</title>'}))
  const page=await context.newPage();page.setDefaultTimeout(8000);page.on('pageerror',e=>errors.push(e.message))
  await page.goto(base+'/fire/places',{waitUntil:'domcontentloaded',timeout:20000})
  const first=page.getByRole('row').filter({hasText:places[0].name})
  await first.getByRole('button',{name:'详情',exact:true}).click()
  const dialog=page.getByRole('dialog',{name:places[0].name,exact:true})
  await dialog.getByText('测试张先生',{exact:true}).waitFor();await dialog.getByText('025-12345678',{exact:true}).waitFor()
  assert.equal(await dialog.getByRole('link',{name:/的拨号页面/}).count(),2)
  assert.equal(await dialog.locator('.place-record-details tr').filter({hasText:'第二位联系人'}).getByRole('link').count(),1)
  assert.equal(await dialog.getByRole('heading',{name:'联系人',exact:true}).count(),0)
  const labelWidths=await dialog.locator('.el-descriptions__label').evaluateAll(cells=>cells.map(cell=>Math.round(cell.getBoundingClientRect().width)))
  assert.equal(new Set(labelWidths).size,1)
  const dial=dialog.getByRole('link',{name:'测试张先生的拨号页面（新窗口打开）'})
  assert.equal(await dial.locator('svg').count(),1)
  assert.equal(await dial.getAttribute('target'),'_blank');assert.ok((await dial.getAttribute('rel')).includes('noopener'))
  const [popup]=await Promise.all([page.waitForEvent('popup'),dial.click()]);await popup.waitForLoadState('domcontentloaded');assert.equal(popup.url(),'https://www.jnrhtx.top/meeting/index?from=Dc');await popup.close()
  checks.push('database contact fields, landline preserved and exact external URL opens in a new tab')
  await page.keyboard.press('Escape');mode='error';await first.getByRole('button',{name:'详情',exact:true}).click()
  await dialog.getByRole('alert').filter({hasText:'联系人测试失败'}).waitFor();assert.equal(await dialog.getByText('测试张先生',{exact:true}).count(),0)
  mode='empty';await dialog.getByRole('button',{name:'重试',exact:true}).click();await dialog.getByText('暂无联系人信息',{exact:true}).waitFor();assert.equal(await dialog.getByRole('link',{name:/的拨号页面/}).count(),0)
  checks.push('error clears stale contact values, retry and empty states')
  await page.keyboard.press('Escape');mode='success';slow=true;await first.getByRole('button',{name:'详情',exact:true}).click();await dialog.getByText('正在加载联系人…',{exact:true}).waitFor();await page.keyboard.press('Escape')
  await page.getByRole('row').filter({hasText:places[1].name}).getByRole('button',{name:'详情',exact:true}).click()
  const second=page.getByRole('dialog',{name:places[1].name,exact:true});await second.getByText('测试李女士',{exact:true}).waitFor();await page.waitForTimeout(550);assert.equal(await second.getByText('测试张先生',{exact:true}).count(),0)
  checks.push('slow previous request cannot overwrite newly selected place')
  await page.setViewportSize({width:720,height:900});await page.keyboard.press('Tab');await page.locator('.el-message').waitFor({state:'hidden',timeout:6000});await page.locator('.dialog-fade-enter-active').first().waitFor({state:'hidden'})
  await page.screenshot({path:'docs/verification/place-contacts-narrow.png',fullPage:true});checks.push('narrow viewport and keyboard')
  assert.deepEqual(errors,[])
  fs.writeFileSync('docs/verification/place-contacts-browser.json',JSON.stringify({actualVue:true,fixtureHTTP:true,businessDatabaseAccessed:false,externalDialPageStubbed:true,checks,errors},null,2))
  console.log(JSON.stringify({checks,errors}))
 }finally{if(browser)await browser.close();server.kill()}
})().catch(e=>{console.error(e);process.exitCode=1})
