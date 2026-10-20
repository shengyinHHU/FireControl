// Actual Vue + AMap browser interaction, with fixture HTTP responses instead of business DB.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright');
const results = JSON.parse(fs.readFileSync('../SQL/geocoding/js_output/full_101/results_all_101.json','utf8'));
const rows = results.map((r,i) => {
 const c = r.candidates.find(c => c.candidate_id === r.selected_id);
 return {placeId:String(i+5001),name:r.row.name,address:r.row.address,placeType:r.row.place_type,longitude:c.longitude,latitude:c.latitude,coordinateSystem:'GCJ02',locationStatus:'review',industryCategory:'实际清单类别',mainRiskSources:'测试展示'};
});
const base='http://127.0.0.1:5175';
(async()=>{
 const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,timeout:8000});
 const errors=[];const checks=[];
 try {
  const context=await browser.newContext({viewport:{width:1600,height:1000}});
  await context.addCookies([{name:'Admin-Token',value:'browser-test-fixture',url:base}]);
  await context.route('**/dev-api/fire/places/**',route=>{
   const url=new URL(route.request().url());const last=url.pathname.split('/').pop();
   let data=last==='map'?{code:200,data:rows,truncated:false}:last==='list'?{code:200,rows,total:rows.length}:{code:200,data:rows.find(r=>r.placeId===last)};
   return route.fulfill({contentType:'application/json',body:JSON.stringify(data)});
  });
  const page=await context.newPage();page.setDefaultTimeout(5000);page.on('pageerror',e=>errors.push(e.message));
  await page.goto(base+'/fire/dashboard',{waitUntil:'domcontentloaded',timeout:15000});
  await page.locator('.map-layer-toggle').waitFor({timeout:25000});
  await page.locator('.map-layer-toggle').click();
  const panel=page.locator('.map-layer-buttons');
  await page.waitForFunction(()=>document.querySelector('.map-place-state')?.textContent.includes('选择类别'),null,{timeout:10000});
  const toggle=async(label,count)=>{
   await panel.getByRole('button',{name:label,exact:true}).click();
   await page.waitForFunction(n=>{ const text=document.querySelector('.map-place-state')?.textContent || ''; if(n===0 && text.includes('选择类别')) return true; return Number(text.match(/[0-9]+/)?.[0])===n; },count,{timeout:5000});
   checks.push({label,expected:count});console.log('Layer check: '+label+' -> '+count);
  };
  await toggle('重点企业',52);
  await page.locator('.fire-map-place-marker').first().waitFor({timeout:20000});
  await toggle('高层建筑',83);
  await toggle('重点企业',31);
  await toggle('高层建筑',0);
  await page.waitForFunction(()=>document.querySelectorAll('.fire-map-place-marker').length===0,null,{timeout:5000});
  await toggle('综合商业体',7);await toggle('养老院',18);await toggle('综合商业体',11);await toggle('养老院',0);
  for(let i=0;i<3;i++){await toggle('重点企业',52);await toggle('重点企业',0);}
  await toggle('重点企业',52);await toggle('高层建筑',83);await toggle('综合商业体',90);await toggle('养老院',101);
  await page.locator('.fire-map-place-marker').first().click({force:true});
  await page.getByRole('dialog').filter({hasText:'场所类别'}).waitFor({timeout:5000});
  assert((await page.getByRole('dialog').filter({hasText:'场所类别'}).innerText()).includes('待核验'));
  await page.getByRole('dialog').filter({hasText:'场所类别'}).locator('.el-dialog__headerbtn').click();
  await page.getByRole('dialog').filter({hasText:'场所类别'}).waitFor({state:'hidden',timeout:3000});
  fs.mkdirSync('../docs/verification',{recursive:true});
  await page.screenshot({path:'../docs/verification/map-place-layers.png',fullPage:true});
  assert.equal(await panel.getByRole('button',{name:'重点企业',exact:true}).getAttribute('aria-pressed'),'true');
  const noLogin=await browser.newContext({viewport:{width:1440,height:900}});
  const guest=await noLogin.newPage();await guest.goto(base+'/fire/dashboard',{waitUntil:'domcontentloaded',timeout:15000});
  await guest.locator('.map-layer-toggle').click();
  assert((await guest.locator('.map-place-state').innerText()).includes('请登录'));
  assert.equal(await guest.locator('.fire-map-place-marker').count(),0);
  await noLogin.close();
  assert.deepEqual(errors,[]);
  const report={browser:'Edge + real AMap SDK',backend:'fixture responses; no business DB connection',records:101,checks,guest:'login required, no fallback markers',placeDetail:'database record, pending verification shown',errors,screenshot:'docs/verification/map-place-layers.png'};
  fs.writeFileSync('../docs/verification/map-place-layers-browser.json',JSON.stringify(report,null,2));
  console.log(JSON.stringify(report));
 } finally {await browser.close();}
})().catch(e=>{console.error(String(e.message).replace(/[a-f0-9]{32}/gi,'[redacted]'));process.exitCode=1;});
