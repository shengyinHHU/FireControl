// Real Vue/Element Plus import flow with fixture HTTP; never writes the business database.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict')
const port=5186, base=`http://127.0.0.1:${port}`
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port',String(port),'--strictPort'],{cwd:process.cwd(),windowsHide:true,stdio:'ignore'})
;(async()=>{let browser;const checks=[],errors=[]
try{
 for(let i=0;i<100;i++){try{if((await fetch(base)).ok)break}catch{}await new Promise(r=>setTimeout(r,100))}
 browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});const context=await browser.newContext({viewport:{width:1440,height:1000},acceptDownloads:true});await context.addCookies([{name:'Admin-Token',value:'fixture',url:base}]);
 let previewCalls=0,confirmCalls=0,listCalls=0;const row=(rowNumber,name,status,coordinates=true)=>({rowNumber,name,category:'养老机构',status,longitude:coordinates?118.76:null,latitude:coordinates?32.23:null,reason:status==='duplicate'?'数据库中已有近似名称，不覆盖':status==='invalid'?'经纬度需成对填写':'',matches:status==='duplicate'?['已有测试养老机构']:[]})
 const rows=[row(2,'新增定位场所','new'),row(3,'新增无坐标场所','new',false),row(4,'相似名称场所','duplicate'),row(5,'错误坐标场所','invalid',false)];
 const preview={batchId:'fixture-owner-batch',state:'ready',summary:{total:4,new:2,duplicate:1,invalid:1,inserted:0,withCoordinates:1,withoutCoordinates:1},rows}
 const final={...preview,state:'complete',summary:{...preview.summary,new:0,inserted:2},rows:rows.map(r=>({...r,status:r.status==='new'?'inserted':r.status}))}
 await context.route('**/dev-api/**',async route=>{const p=new URL(route.request().url()).pathname.replace('/dev-api','');let result={code:200,data:[]}
 if(p==='/getInfo')result={code:200,user:{userId:1,userName:'admin',nickName:'管理员',avatar:''},roles:['admin'],permissions:['*:*:*']}
 else if(p==='/getRouters')result={code:200,data:[]}
 else if(p==='/fire/places/list'){listCalls++;result={code:200,total:2,rows:[{placeId:'1',name:'列表有坐标场所',placeType:'elderly',longitude:118.76,latitude:32.23,coordinateSystem:'GCJ02',locationStatus:'review'},{placeId:'2',name:'列表无坐标场所',placeType:'industry',longitude:null,latitude:null}]}}
 else if(p==='/fire/places/import/preview'){previewCalls++;await new Promise(r=>setTimeout(r,200));result=previewCalls===1?{code:500,msg:'预检测试失败，可重试'}:{code:200,data:preview}}
 else if(p==='/fire/places/import/confirm'){confirmCalls++;assert.equal(route.request().postDataJSON().batchId,preview.batchId);result=confirmCalls===1?{code:500,msg:'导入测试失败，可重试'}:{code:200,data:final}}
 await route.fulfill({contentType:'application/json',body:JSON.stringify(result)})
 })
 const page=await context.newPage();page.setDefaultTimeout(20000);page.on('pageerror',e=>errors.push(e.message));await page.goto(base+'/fire/places',{waitUntil:'domcontentloaded',timeout:30000});
 await page.getByText('列表无坐标场所',{exact:true}).waitFor();await page.getByText('无有效坐标 · 列表保留',{exact:true}).waitFor();assert.equal(await page.getByRole('button',{name:/新增重点场所/}).count(),0);checks.push('unlocated row remains visible, explicit location status, no fake CRUD')
 await page.getByRole('button',{name:'导入场所',exact:true}).click();const dialog=page.getByRole('dialog',{name:'导入重点场所'});await dialog.locator('input[type=file]').setInputFiles(path.resolve('../outputs/regulatory-geocoded/监管对象_公开地址补查定位.xlsx'));
 await dialog.getByRole('button',{name:'检查文件',exact:true}).click();await page.keyboard.press('Escape');await dialog.waitFor();await dialog.getByRole('alert').filter({hasText:'预检测试失败'}).waitFor();await dialog.getByRole('button',{name:'检查文件',exact:true}).click();await dialog.getByText('预检结果 · 4 条',{exact:true}).waitFor();assert.equal(confirmCalls,0);checks.push('preview failure retains selection, retry, no commit before confirmation')
 await dialog.getByText('重复跳过',{exact:true}).first().click();await dialog.getByText('相似名称场所',{exact:true}).waitFor();assert.equal(await dialog.getByText('新增定位场所',{exact:true}).count(),0);await dialog.getByText('全部',{exact:true}).click();
 const dl=page.waitForEvent('download');await dialog.getByRole('button',{name:'下载检查结果',exact:true}).click();assert.ok((await dl).suggestedFilename().endsWith('.csv'));checks.push('duplicate and validation reasons can be filtered and downloaded')
 await dialog.getByRole('button',{name:'确认新增 2 条',exact:true}).click();await dialog.getByRole('alert').filter({hasText:'导入测试失败'}).waitFor();await dialog.getByRole('button',{name:'确认新增 2 条',exact:true}).click();await dialog.getByText('导入完成 · 4 条',{exact:true}).waitFor();assert.equal(confirmCalls,2);assert.ok(listCalls>=2);assert.equal(await dialog.getByRole('button',{name:'确认新增 2 条'}).count(),0);checks.push('commit retry uses same owner batch, result remains visible and list reloads')
 await page.setViewportSize({width:720,height:950});await page.locator('.el-message').last().waitFor({state:'hidden',timeout:8000});await page.screenshot({path:'docs/verification/place-import-narrow.png',fullPage:true});const box=await dialog.boundingBox();assert.ok(box.width<=720);checks.push('narrow viewport keeps modal within screen')
 await page.getByRole('button',{name:'关闭',exact:true}).last().click();await page.getByRole('button',{name:'导入场所',exact:true}).click();assert.equal(await dialog.getByText('导入完成 · 4 条').count(),0);checks.push('reopen clears previous batch and result')
 const other=await browser.newContext();await other.addCookies([{name:'Admin-Token',value:'fixture-reader',url:base}]);await other.route('**/dev-api/**',async r=>{let x={code:200,data:[]};const u=r.request().url();if(u.endsWith('/getInfo'))x={code:200,user:{userId:8,userName:'reader'},roles:['reader'],permissions:['fire:places:list']};if(u.includes('/fire/places/list'))x={code:200,rows:[],total:0};await r.fulfill({contentType:'application/json',body:JSON.stringify(x)})});const reader=await other.newPage();await reader.goto(base+'/fire/places');await reader.getByRole('heading',{name:'重点场所',exact:true}).waitFor();assert.equal(await reader.getByRole('button',{name:'导入场所',exact:true}).count(),0);checks.push('read-only user cannot see import action')
 assert.deepEqual(errors,[]);fs.writeFileSync('docs/verification/place-import-browser.json',JSON.stringify({actualVue:true,fixtureHTTP:true,businessDatabaseAccessed:false,checks,errors},null,2));console.log(JSON.stringify({checks,errors}))
}finally{if(browser)await browser.close();server.kill()}})().catch(e=>{console.error(e);process.exitCode=1})

