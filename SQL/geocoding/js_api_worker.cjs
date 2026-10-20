// Uses the official JS SDK in the supplied example. No REST key or database connection.
const fs = require('node:fs');
const { chromium } = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const scrub = s => String(s).replace(/[a-f0-9]{32}/gi,'[redacted]').replace(/key=[^&\s"'<>]+/gi,'key=[redacted]');
async function main() {
 const cfg = JSON.parse(fs.readFileSync(0,'utf8'));
 let cache = fs.existsSync(cfg.cache) ? JSON.parse(fs.readFileSync(cfg.cache,'utf8')) : {};
 let browser;
 const deadline = Date.now()+cfg.maxSeconds*1000;
 const report = {completed:0,errors:[],stopped:''};
 function save() {
  fs.writeFileSync(cfg.cache+'.tmp',JSON.stringify(cache,null,2),'utf8');fs.renameSync(cfg.cache+'.tmp',cfg.cache);
  fs.writeFileSync(cfg.report,JSON.stringify(report,null,2),'utf8');
 }
 try {
  browser = await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,timeout:8000});
  const page=await browser.newPage();
  await page.goto(cfg.url,{waitUntil:'domcontentloaded',timeout:12000});
  await page.waitForFunction(()=>window.AMap && typeof geocoder!=='undefined',{},{timeout:8000});
  if(cfg.tasks.some(t=>t.kind==='poi')) {
   await page.evaluate(()=>new Promise((resolve,reject)=>{
    const timer=setTimeout(()=>reject(Error('PlaceSearch plugin timeout')),8000);
    AMap.plugin('AMap.PlaceSearch',()=>{clearTimeout(timer);resolve();});
   }));
  }
  for(let i=0;i<cfg.tasks.length;i++) {
   const t=cfg.tasks[i];
   if(cache[t.fingerprint])continue;
   if(Date.now()+8000>deadline){report.stopped='total time budget reached';break;}
   const answer=await page.evaluate(async task => {
    return await new Promise(resolve=>{
     let done=false;
     const finish=value=>{if(done)return;done=true;clearTimeout(timer);resolve(value);};
     const timer=setTimeout(()=>finish({status:'0',info:'LOCAL_TIMEOUT'}),7000);
     const plain=p=>p && typeof p.toString==='function' ? p.toString() : '';
     try {
      if(task.kind==='geocode') {
       const gc=new AMap.Geocoder({city:'南京市'});
       gc.getLocation(task.params.address,(status,r)=>{
        if(status==='no_data')return finish({status:'1',info:'OK',geocodes:[]});
        if(status!=='complete')return finish({status:'0',info:String(r?.info||r||status)});
        finish({status:'1',info:'OK',geocodes:(r.geocodes||[]).map(g=>({formatted_address:g.formattedAddress||'',location:plain(g.location),level:g.level||'',adcode:g.adcode||g.addressComponent?.adcode||'',city:g.city||g.addressComponent?.city||'',province:g.province||'',district:g.district||''}))});
       });
      } else {
       const ps=new AMap.PlaceSearch({city:'南京市',citylimit:true,pageSize:5,pageIndex:1,extensions:'base'});
       ps.search(task.params.keywords,(status,r)=>{
        if(status==='no_data')return finish({status:'1',info:'OK',count:'0',pois:[]});
        if(status!=='complete')return finish({status:'0',info:String(r?.info||r||status)});
        finish({status:'1',info:'OK',count:String(r.poiList?.count||0),pois:(r.poiList?.pois||[]).map(p=>({id:p.id||'',name:p.name||'',location:plain(p.location),address:p.address||'',adcode:p.adcode||'',pname:p.pname||p.province||'',cityname:p.cityname||p.city||'',adname:p.adname||p.district||''}))});
       });
      }
     } catch(e){finish({status:'0',info:e.name});}
    });
   },t);
   if(answer.status==='1') {cache[t.fingerprint]=answer;report.completed++;}
   else {
    const info=scrub(answer.info);report.errors.push({record_key:t.record_key,kind:t.kind,info});
    if(/KEY|SCODE|SIGNATURE|PRIVILEGE|OVER_LIMIT|PLAT_NOMATCH|INVALID_USER|INSUFFICIENT/i.test(info))report.stopped=info;
   }
   save();
   console.log(JSON.stringify({phase:t.kind,index:i+1,total:cfg.tasks.length,name:t.name,ok:answer.status==='1',matches:(answer.geocodes||answer.pois||[]).length,error:answer.status==='1'?'':scrub(answer.info)}));
   if(report.stopped)break;
   await new Promise(r=>setTimeout(r,650));
  }
 } catch(e) {report.stopped=scrub(e.message).slice(0,400);console.log(JSON.stringify({stopped:report.stopped}));}
 finally {save();if(browser)await browser.close();}
}
main().catch(e=>{console.error(scrub(e.message));process.exitCode=1;});