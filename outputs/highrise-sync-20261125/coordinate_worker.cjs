const fs=require('node:fs'),path=require('node:path');
const {chromium}=require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const scrub=s=>String(s).replace(/[a-f0-9]{32}/gi,'[redacted]');
(async()=>{
 const cfg=JSON.parse(fs.readFileSync(0,'utf8')),cache=JSON.parse(fs.readFileSync(cfg.cache,'utf8'));
 const html=fs.readFileSync(path.resolve(__dirname,'../../SQL/geocoding/geocoding.html'),'utf8');
 const key=html.match(/maps\?v=2\.0&key=([^&"']+)/)[1],security=html.match(/securityJsCode:\s*["']([^"']+)/)[1];
 const report={completed:0,errors:[]};let browser;
 const save=()=>{fs.writeFileSync(cfg.cache,JSON.stringify(cache,null,2));fs.writeFileSync(cfg.report,JSON.stringify(report,null,2));};
 try{
  browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true,args:['--no-proxy-server'],timeout:12000});
  const page=await browser.newPage();page.on('requestfailed',r=>{report.errors.push({host:new URL(r.url()).hostname,error:r.failure()?.errorText});});await page.goto(cfg.url,{waitUntil:'domcontentloaded',timeout:10000});
  const ready=await page.evaluate(({key,security})=>new Promise(resolve=>{
   window._AMapSecurityConfig={securityJsCode:security};const el=document.createElement('script');
   const timer=setTimeout(()=>resolve(false),45000);el.src='https://webapi.amap.com/maps?v=2.0&key='+encodeURIComponent(key)+'&plugin=AMap.PlaceSearch';
   el.onload=()=>{clearTimeout(timer);resolve(!!window.AMap)};el.onerror=()=>{clearTimeout(timer);resolve(false)};document.head.appendChild(el);
  }),{key,security});
  if(!ready)throw Error('AMap SDK could not load');
  await page.evaluate(()=>new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('Map plugins loading timeout')),30000);AMap.plugin(['AMap.PlaceSearch','AMap.Geocoder'],()=>{clearTimeout(timer);resolve()})}));
  console.log('AMap SDK ready');
  for(const t of cfg.tasks){
   if(cache[t.fingerprint]){console.log(JSON.stringify({name:t.name,query:t.params.keywords,cached:true}));continue;}
   const answer=await page.evaluate(t=>new Promise(resolve=>{
    let done=false;const finish=v=>{if(done)return;done=true;clearTimeout(timer);resolve(v)};
    const timer=setTimeout(()=>finish({status:'0',info:'TIMEOUT'}),8000);
    if(t.kind==='geocode')return new AMap.Geocoder({city:'南京市'}).getLocation(t.params.address,(status,r)=>{
     if(status==='no_data')return finish({status:'1',info:'OK',geocodes:[]});
     if(status!=='complete')return finish({status:'0',info:String(r?.info||status)});
     finish({status:'1',info:'OK',geocodes:(r.geocodes||[]).map(g=>({formatted_address:g.formattedAddress,location:g.location.toString(),level:g.level,adcode:g.adcode}))});
    });
    new AMap.PlaceSearch({city:'南京市',citylimit:true,pageSize:10,pageIndex:1,extensions:'base'}).search(t.params.keywords,(status,r)=>{
     if(status==='no_data')return finish({status:'1',info:'OK',pois:[]});
     if(status!=='complete')return finish({status:'0',info:String(r?.info||status)});
     finish({status:'1',info:'OK',pois:(r.poiList?.pois||[]).map(p=>({id:p.id,name:p.name,location:p.location.toString(),address:p.address||'',adcode:p.adcode||'',type:p.type||''}))});
    });
   }),t);
   if(answer.status==='1'){cache[t.fingerprint]=answer;report.completed++;}else report.errors.push({name:t.name,info:scrub(answer.info)});
   save();console.log(JSON.stringify({name:t.name,query:t.params.keywords,matches:answer.pois?.length||0,error:answer.status==='1'?'':scrub(answer.info)}));
   await new Promise(r=>setTimeout(r,650));
  }
 }catch(e){report.error=scrub(e.message);console.log(JSON.stringify({error:report.error}));process.exitCode=1;}
 finally{save();await browser?.close();}
})();
