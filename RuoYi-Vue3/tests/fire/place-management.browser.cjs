// Real Vue/Element Plus UI with controlled HTTP. Never writes the business database.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const fs = require('node:fs'), assert = require('node:assert/strict')
const base='http://127.0.0.1:5197'
const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--mode','firemock','--host','127.0.0.1','--port','5197','--strictPort'],{cwd:process.cwd(),windowsHide:true,stdio:'ignore'})
;(async()=>{let browser;const checks=[],errors=[]
try {
 for(let i=0;i<200;i++){try{if((await fetch(base)).ok)break}catch{}await new Promise(r=>setTimeout(r,100))}
 browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true})
 const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'});await context.addCookies([{name:'Admin-Token',value:'fixture-only',url:base}])
 let reader=false,failList=false,failEdit=false,failSave=false,slowSave=false,conflict=false,mutations=0
 let records=[{placeId:'1',revision:0,name:'真实重点场所一',placeType:'industry',address:'原地址',industryCategory:'加工制造',managementCompany:'管理单位',riskLevel:'一般',mainRiskSources:'电气设备',longitude:118.75,latitude:32.23,coordinateSystem:'GCJ02',locationStatus:'confirmed',remark:'现有备注',contacts:[{contactId:'101',contactName:'张先生',phone:'025-12345678 转 801',contactRole:'principal'}]},...Array.from({length:10},(_,i)=>({placeId:String(i+2),revision:0,name:'其他场所'+String(i+2).padStart(2,'0'),placeType:'highrise',longitude:null,latitude:null,contacts:[]}))]
 await context.route('**/dev-api/**',async route=>{
  const req=route.request(),url=new URL(req.url()),p=url.pathname.replace('/dev-api',''),method=req.method();let result={code:200,data:[]}
  if(p==='/getInfo')result={code:200,user:{userId:reader?8:1,userName:reader?'reader':'admin',nickName:'测试用户'},roles:reader?['reader']:['admin'],permissions:reader?['fire:places:list']:['*:*:*']}
  else if(p==='/getRouters')result={code:200,data:[]}
  else if(p==='/fire/places/list'){
   let selected=records.filter(r=>(!url.searchParams.get('name')||r.name.includes(url.searchParams.get('name')))&&(!url.searchParams.get('placeType')||r.placeType===url.searchParams.get('placeType')))
   const page=Number(url.searchParams.get('pageNum')||1),size=Number(url.searchParams.get('pageSize')||10)
   result=failList?{code:500,msg:'场所列表暂时不可用'}:{code:200,rows:selected.slice((page-1)*size,page*size).map(r=>({...r,owner:r.contacts[0]?.contactName,phone:r.contacts[0]?.phone})),total:selected.length}
  }
  else if(/^\/fire\/places\/[^/]+\/edit$/.test(p)){
   const found=records.find(r=>r.placeId===p.split('/')[3]);await new Promise(r=>setTimeout(r,120));result=failEdit?{code:500,msg:'资料加载失败，请重试'}:{code:200,data:{place:{...found},contacts:found.contacts}}
  }
  else if(p.endsWith('/contacts'))result={code:200,data:records.find(r=>r.placeId===p.split('/')[3])?.contacts||[]}
  else if(/^\/fire\/places\/[^/]+$/.test(p)&&method==='GET')result={code:200,data:records.find(r=>r.placeId===p.split('/')[3])}
  else if(p==='/fire/places'||/^\/fire\/places\/[^/]+$/.test(p)){
   mutations++;if(slowSave)await new Promise(r=>setTimeout(r,650))
   if(failSave){failSave=false;result={code:500,msg:'保存测试失败，请重试'}}
   else if(conflict){conflict=false;const r=records.find(r=>r.placeId===p.split('/')[3]);r.revision++;result={code:500,msg:'场所已被其他人修改，请重新打开后编辑'}}
   else if(method==='DELETE'){
    const found=records.find(r=>r.placeId===p.split('/')[3]);assert.equal(Number(url.searchParams.get('revision')),found.revision);records=records.filter(r=>r!==found);result={code:200}
   }else {
    const data=req.postDataJSON();assert.ok(Array.isArray(data.contacts));assert.ok(!data.contacts.some(c=>'_key' in c));const id=method==='POST'?'100':p.split('/')[3]
    const old=records.find(r=>r.placeId===id);if(old)assert.equal(data.revision,old.revision)
    const row={...data,placeId:id,revision:old?old.revision+1:0,locationStatus:data.longitude==null?'pending':'review',contacts:data.contacts.map((c,i)=>({...c,contactId:c.contactId||String(500+i)}))}
    if(old)records[records.indexOf(old)]=row;else records.unshift(row);result={code:200,data:id}
   }
  }
  await route.fulfill({contentType:'application/json',body:JSON.stringify(result)})
 })
 const page=await context.newPage();page.setDefaultTimeout(15000);page.on('pageerror',e=>errors.push(e.message));await page.goto(base+'/fire/places')
 const row=name=>page.getByRole('row').filter({hasText:name})
 await row('真实重点场所一').getByRole('button',{name:'编辑',exact:true}).waitFor()
 await page.getByRole('button',{name:'新增重点场所',exact:true}).click();let editor=page.getByRole('dialog',{name:'新增重点场所',exact:true})
 await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.getByText('请填写场所名称',{exact:true}).waitFor();assert.equal(mutations,0)
 await editor.getByRole('textbox',{name:'场所名称',exact:true}).fill('新增养老机构测试')
 const category=editor.getByRole('combobox',{name:'场所类别',exact:true});await category.focus();await category.press('ArrowDown');await page.getByRole('option',{name:'养老机构',exact:true}).click()
 await editor.getByRole('textbox',{name:'详细地址',exact:true}).fill('测试地址')
 await editor.getByRole('button',{name:'添加联系人',exact:true}).click();await editor.getByRole('textbox',{name:'联系人 1 姓名',exact:true}).fill('新负责人');await editor.getByRole('textbox',{name:'联系人 1 电话',exact:true}).fill('025-11111111 转 802')
 failSave=true;await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.getByText('保存测试失败，请重试',{exact:true}).waitFor();assert.equal(await editor.getByRole('textbox',{name:'场所名称',exact:true}).inputValue(),'新增养老机构测试')
 slowSave=true;const before=mutations;await editor.getByRole('button',{name:'保存',exact:true}).click();await page.keyboard.press('Escape');await editor.waitFor();await editor.getByRole('button',{name:'取消',exact:true}).isDisabled().then(x=>assert.equal(x,true));await editor.waitFor({state:'hidden'});slowSave=false;assert.equal(mutations,before+1)
 await row('新增养老机构测试').waitFor();assert.equal(records[0].longitude,null);assert.equal(records[0].contacts[0].phone,'025-11111111 转 802');checks.push('real create, required validation, category selection, contact and unlocated save; failures retain drafts and busy state blocks close/duplicates')
 await row('真实重点场所一').getByRole('button',{name:'编辑',exact:true}).click();editor=page.getByRole('dialog',{name:'编辑重点场所',exact:true});await editor.getByRole('textbox',{name:'场所名称',exact:true}).waitFor();await page.waitForFunction(()=>document.querySelector('.place-editor-dialog input[aria-label="场所名称"]')?.value==='真实重点场所一')
 assert.equal(await editor.getByRole('textbox',{name:'联系人 1 电话',exact:true}).inputValue(),'025-12345678 转 801')
 await editor.getByRole('textbox',{name:'场所名称',exact:true}).fill('编辑后场所一');await editor.getByRole('textbox',{name:'联系人 1 电话',exact:true}).fill('025-22222222 转 803')
 conflict=true;await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.getByText('场所已被其他人修改，请重新打开后编辑',{exact:true}).waitFor();assert.equal(await editor.getByRole('textbox',{name:'场所名称',exact:true}).inputValue(),'编辑后场所一')
 await editor.getByRole('button',{name:'取消',exact:true}).click();await page.getByRole('dialog',{name:'放弃修改'}).getByRole('button',{name:'继续编辑',exact:true}).click();await editor.waitFor()
 await editor.getByRole('button',{name:'取消',exact:true}).click();await page.getByRole('dialog',{name:'放弃修改'}).getByRole('button',{name:'放弃修改',exact:true}).click();await editor.waitFor({state:'hidden'})
 await row('真实重点场所一').getByRole('button',{name:'编辑',exact:true}).click();await page.waitForFunction(()=>document.querySelector('.place-editor-dialog input[aria-label="场所名称"]')?.value==='真实重点场所一')
 await editor.getByRole('textbox',{name:'场所名称',exact:true}).fill('编辑后场所一');await editor.getByRole('textbox',{name:'联系人 1 电话',exact:true}).fill('025-22222222 转 803');await editor.getByRole('textbox',{name:'经度',exact:true}).fill('118.7600000');await editor.getByRole('textbox',{name:'纬度',exact:true}).fill('')
 await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.getByText(/请成对填写有效经纬度/).first().waitFor();await editor.getByRole('textbox',{name:'纬度',exact:true}).fill('32.2400000');await editor.getByRole('button',{name:'保存',exact:true}).click();await editor.waitFor({state:'hidden'});await row('编辑后场所一').waitFor()
 const updated=records.find(r=>r.placeId==='1');assert.equal(updated.revision,2);assert.equal(updated.contacts[0].contactId,'101');assert.equal(updated.latitude,32.24)
 await row('编辑后场所一').getByRole('button',{name:'详情',exact:true}).click();let detail=page.getByRole('dialog',{name:'编辑后场所一',exact:true});await detail.getByText('025-22222222 转 803',{exact:true}).waitFor();await page.keyboard.press('Escape');await detail.waitFor({state:'hidden'});checks.push('persisted edit and stable contact identity, coordinate-pair validation, conflict recovery, unsaved-draft confirmation and refreshed detail')
 failEdit=true;await row('编辑后场所一').getByRole('button',{name:'编辑',exact:true}).click();await editor.getByRole('alert').filter({hasText:'资料加载失败，请重试'}).waitFor();assert.ok(await editor.getByRole('button',{name:'保存',exact:true}).isDisabled());failEdit=false;await editor.getByRole('button',{name:'重新加载',exact:true}).click();await editor.getByRole('textbox',{name:'场所名称',exact:true}).waitFor();await page.waitForFunction(()=>document.querySelector('.place-editor-dialog input[aria-label="场所名称"]')?.value==='编辑后场所一');await editor.getByRole('button',{name:'取消',exact:true}).click();await editor.waitFor({state:'hidden'});checks.push('failed edit load cannot save partial contact data and retries safely')
 const search=page.getByRole('textbox',{name:'场所名称查询',exact:true});await search.fill('新增养老');await page.getByRole('button',{name:'查询',exact:true}).click();await row('新增养老机构测试').waitFor();assert.ok(page.url().includes('keyword='));await page.reload();await row('新增养老机构测试').waitFor();await page.getByRole('button',{name:'重置',exact:true}).click();await row('编辑后场所一').waitFor()
 await page.locator('.el-pagination').getByRole('button',{name:'下一页',exact:true}).click();await row('其他场所11').waitFor();await row('其他场所11').getByRole('button',{name:'删除',exact:true}).click();await page.getByRole('dialog',{name:'删除重点场所'}).getByRole('button',{name:'取消',exact:true}).click();assert.ok(records.some(r=>r.placeId==='11'))
 await row('其他场所11').getByRole('button',{name:'删除',exact:true}).click();await page.getByRole('dialog',{name:'删除重点场所'}).getByRole('button',{name:'删除场所',exact:true}).click();await row('其他场所10').getByRole('button',{name:'删除',exact:true}).click();await page.getByRole('dialog',{name:'删除重点场所'}).getByRole('button',{name:'删除场所',exact:true}).click();await row('编辑后场所一').waitFor();assert.ok(page.url().includes('page=1'));checks.push('URL search restoration, delete cancel/confirm and last-page clamp after deletion')
 failList=true;await page.getByRole('button',{name:'刷新记录',exact:true}).click();await page.locator('.fire-table-card .fire-error').filter({hasText:'场所列表暂时不可用'}).waitFor();assert.equal(await row('编辑后场所一').count(),0);failList=false;await page.getByRole('button',{name:'重新加载',exact:true}).click();await row('编辑后场所一').waitFor();checks.push('list error clears stale rows and retry restores real results')
 await page.locator('.el-message').last().waitFor({state:'hidden',timeout:8000});await page.screenshot({path:'docs/verification/place-management-desktop.png',fullPage:true})
 await page.setViewportSize({width:390,height:844});await page.getByRole('button',{name:'新增重点场所',exact:true}).click();editor=page.getByRole('dialog',{name:'新增重点场所',exact:true});await editor.waitFor();await page.waitForTimeout(400);const box=await editor.boundingBox();assert.ok(box.width<=390&&box.height<=844);await editor.getByRole('textbox',{name:'备注',exact:true}).fill('窄屏可编辑备注');await editor.getByRole('textbox',{name:'场所名称',exact:true}).fill('窄屏测试');await page.screenshot({path:'docs/verification/place-management-editor-narrow.png',fullPage:true});await editor.getByRole('button',{name:'取消',exact:true}).click();await page.getByRole('dialog',{name:'放弃修改'}).getByRole('button',{name:'放弃修改',exact:true}).click();await editor.waitFor({state:'hidden'});checks.push('390px form scroll keeps bottom fields and actions reachable')
 reader=true;await page.reload();await page.getByRole('heading',{name:'重点场所',exact:true}).waitFor();assert.equal(await page.getByRole('button',{name:/新增重点场所|编辑|删除|导入场所/}).count(),0);await row('编辑后场所一').getByRole('button',{name:'详情',exact:true}).click();await page.getByRole('dialog',{name:'编辑后场所一',exact:true}).getByText('025-22222222 转 803',{exact:true}).waitFor();checks.push('readonly role sees details without any mutation or import actions')
 assert.deepEqual(errors,[]);fs.writeFileSync('docs/verification/place-management-browser.json',JSON.stringify({actualVue:true,fixtureHTTP:true,businessDatabaseAccessed:false,checks,errors},null,2));console.log(JSON.stringify({checks,errors}))
}finally{if(browser)await browser.close();server.kill()}})().catch(e=>{console.error(e);process.exitCode=1})
