const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require(process.env.PLAYWRIGHT_PATH||'C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const ROOT=path.resolve(__dirname,'../..'),OUT=path.join(ROOT,'docs/verification');
const key=process.env.AMAP_COMPARE_KEY,security=process.env.AMAP_COMPARE_SECURITY;
if(!key||!security)throw Error('Set AMAP_COMPARE_KEY and AMAP_COMPARE_SECURITY in the environment; credentials are not stored in reports.');
const ring='118.777719,32.265056;118.789851,32.260523;118.79634,32.25253;118.801418,32.246804;118.809036,32.241077;118.812422,32.235827;118.817359,32.228071;118.818064,32.225445;118.807202,32.223059;118.799302,32.220433;118.791967,32.216733;118.78449,32.212437;118.784632,32.211959;118.778142,32.204201;118.773487,32.197039;118.769968,32.192857;118.760268,32.192926;118.751554,32.195013;118.742429,32.193691;118.732565,32.192717;118.727879,32.195013;118.71859,32.196196;118.720645,32.203778;118.722043,32.211846;118.724673,32.218314;118.73051,32.223461;118.736593,32.229789;118.74358,32.237507;118.747773,32.242235;118.752623,32.250231;118.758624,32.256835;118.763309,32.2633;118.770708,32.266011'.split(';').map(s=>s.split(',').map(Number));
// Classic public WGS84->GCJ02 approximation, same mathematical family as gcoord's GCJ02 implementation.
function local([lng,lat]){
 if(lng<72.004||lng>137.8347||lat<.8293||lat>55.8271)return[lng,lat];
 const x=lng-105,y=lat-35,P=Math.PI;
 const wave=(20*Math.sin(6*x*P)+20*Math.sin(2*x*P))*2/3;
 let dLat=-100+2*x+3*y+.2*y*y+.1*x*y+.2*Math.sqrt(Math.abs(x))+wave;
 dLat+=(20*Math.sin(y*P)+40*Math.sin(y*P/3))*2/3+(160*Math.sin(y*P/12)+320*Math.sin(y*P/30))*2/3;
 let dLng=300+x+2*y+.1*x*x+.1*x*y+.1*Math.sqrt(Math.abs(x))+wave;
 dLng+=(20*Math.sin(x*P)+40*Math.sin(x*P/3))*2/3+(150*Math.sin(x*P/12)+300*Math.sin(x*P/30))*2/3;
 const rad=lat*P/180,e=.006693421622965823,a=6378245,m=1-e*Math.sin(rad)**2;
 return[lng+dLng*180/(a/Math.sqrt(m)*Math.cos(rad)*P),lat+dLat*180/(a*(1-e)/(m*Math.sqrt(m))*P)];
}
function inverse(target){let w=[...target];for(let i=0;i<12;i++){const p=local(w);w=[w[0]+target[0]-p[0],w[1]+target[1]-p[1]];}return w;}
const R=6371008.8,P=Math.PI/180;
function distance(a,b){const dLat=(b[1]-a[1])*P,dLng=(b[0]-a[0])*P;return 2*R*Math.asin(Math.min(1,Math.sqrt(Math.sin(dLat/2)**2+Math.cos(a[1]*P)*Math.cos(b[1]*P)*Math.sin(dLng/2)**2)));}
const sx=R*P*Math.cos(32.23*P),sy=R*P;
function boundaryDistance(p){let min=Infinity;for(let i=0;i<ring.length;i++){const a=ring[i],b=ring[(i+1)%ring.length],x=(p[0]-a[0])*sx,y=(p[1]-a[1])*sy,dx=(b[0]-a[0])*sx,dy=(b[1]-a[1])*sy,t=Math.max(0,Math.min(1,(x*dx+y*dy)/(dx*dx+dy*dy)));min=Math.min(min,Math.hypot(x-t*dx,y-t*dy));}return min;}
function inside(p){if(boundaryDistance(p)<=.001)return true;let yes=false;for(let i=0,j=ring.length-1;i<ring.length;j=i++){const a=ring[j],b=ring[i];if((a[1]>p[1])!==(b[1]>p[1])&&p[0]<(b[0]-a[0])*(p[1]-a[1])/(b[1]-a[1])+a[0])yes=!yes;}return yes;}
const samples=[],seen=new Set();
function add(group,wgs,meta={}){const source=wgs.map(n=>Number(n.toFixed(6))),id=source.join(',');if(seen.has(id))return;seen.add(id);samples.push({sample:samples.length+1,group,source,...meta});}
for(let x=0;x<5;x++)for(let y=0;y<5;y++)add('regional-grid',[118.713+(.105*x/4),32.194+(.075*y/4)]);
// Explicitly synthetic boundary probes: derive WGS84 from desired GCJ02 positions using local inverse, then round to 6 decimals.
for(let i=0;i<ring.length;i++){
 add('boundary-vertex-probe',inverse(ring[i]),{edge:i+1,targetOffsetM:0});
 const a=ring[i],b=ring[(i+1)%ring.length],mid=[(a[0]+b[0])/2,(a[1]+b[1])/2],dx=(b[0]-a[0])*sx,dy=(b[1]-a[1])*sy,len=Math.hypot(dx,dy);
 for(const m of [0,-10,-2,2,10])add(m===0?'boundary-edge-probe':'boundary-offset-probe',inverse([mid[0]-dy/len*m/sx,mid[1]+dx/len*m/sy]),{edge:i+1,targetOffsetM:m});
}
let sourceFileRows=0,sourceFileNearDachang=0;
try{const json=JSON.parse(fs.readFileSync(path.join(ROOT,'tools/119-secret-key/result.json'),'utf8').replace(/^\uFEFF/,''));const rows=json?.data?.list||[];sourceFileRows=rows.length;for(const row of rows){if(row.jd==null||row.wd==null||String(row.jd).trim()===''||String(row.wd).trim()==='')continue;const p=[Number(row.jd),Number(row.wd)];if(p.every(Number.isFinite)&&p[0]>118.69&&p[0]<118.84&&p[1]>32.17&&p[1]<32.30){sourceFileNearDachang++;if(sourceFileNearDachang<=25)add('119-file-coordinate',p);}}}catch{ }
function stats(rows){if(!rows.length)return null;const values=rows.map(r=>r.errorM).sort((a,b)=>a-b);return{count:rows.length,meanM:values.reduce((a,b)=>a+b,0)/values.length,medianM:values[Math.floor(values.length/2)],p95M:values[Math.ceil(values.length*.95)-1],maxM:values.at(-1),classificationDisagreements:rows.filter(r=>r.localInside!==r.officialInside).length};}
(async()=>{let browser;const server=http.createServer((req,res)=>{res.writeHead(200,{'Content-Type':'text/html;charset=utf-8'});res.end('<!doctype html><meta charset="utf-8"><title>坐标转换对照测试</title>');});
const report={createdAt:new Date().toISOString(),officialApi:'AMap JS API 2.0 convertFrom gps',localAlgorithm:'classic WGS84->GCJ02 approximation (a=6378245, ee=0.006693421622965823)',algorithmReference:'https://github.com/hujiulong/gcoord/blob/master/src/crs/GCJ02.ts',boundaryCoordinateSystem:'GCJ02',boundary:ring,sourceFileRows,sourceFileNearDachang,businessDatabaseAccessed:false,applicationModified:false,credentialsSaved:false,syntheticProbeNote:'Boundary probes are constructed using local inverse and six-decimal WGS84 rounding; they are sensitivity tests, not independent ground truth or real incidents.',batches:[],results:[]};
try{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true,timeout:20000});
 const page=await browser.newPage();await page.goto('http://localhost:'+server.address().port,{timeout:10000});
 const sdk=await page.evaluate(({key,security})=>new Promise(resolve=>{window._AMapSecurityConfig={securityJsCode:security};const script=document.createElement('script');const timer=setTimeout(()=>resolve({ok:false,reason:'SDK load timeout'}),25000);script.src='https://webapi.amap.com/maps?v=2.0&key='+encodeURIComponent(key);script.onload=()=>{clearTimeout(timer);resolve({ok:!!window.AMap,version:window.AMap?.version})};script.onerror=()=>{clearTimeout(timer);resolve({ok:false,reason:'SDK network error'})};document.head.appendChild(script)}),{key,security});report.sdk=sdk;if(!sdk.ok)throw Error(sdk.reason||'SDK unavailable');
 console.log('Official JS SDK loaded. Comparing '+samples.length+' coordinates, batches <=40.');
 for(let offset=0;offset<samples.length;offset+=40){const batch=samples.slice(offset,offset+40),started=Date.now();
 const converted=await page.evaluate(points=>new Promise(resolve=>{let done=false;const finish=value=>{if(!done){done=true;clearTimeout(timer);resolve(value)}};const timer=setTimeout(()=>finish({ok:false,status:'timeout'}),15000);try{AMap.convertFrom(points,'gps',(status,result)=>{if(status!=='complete'||!Array.isArray(result?.locations))return finish({ok:false,status,info:String(result?.info||'unknown').slice(0,100)});finish({ok:true,status,info:result.info,points:result.locations.map(p=>[p.getLng(),p.getLat()])})})}catch{finish({ok:false,status:'exception'})}}),batch.map(s=>s.source));
 report.batches.push({batch:report.batches.length+1,size:batch.length,elapsedMs:Date.now()-started,status:converted.status,info:converted.info});if(!converted.ok||converted.points.length!==batch.length)throw Error('Official conversion failed: '+converted.status+' '+(converted.info||''));
 batch.forEach((sample,i)=>{const ours=local(sample.source),official=converted.points[i];if(!official.every(Number.isFinite))throw Error('Nonfinite official coordinate');report.results.push({...sample,local:ours,official,errorM:distance(ours,official),localInside:inside(ours),officialInside:inside(official),officialDistanceToBoundaryM:boundaryDistance(official)});});console.log('Batch '+report.batches.length+' succeeded ('+report.results.length+'/'+samples.length+').');
 }
 report.success=true;report.overall=stats(report.results);report.groups=Object.fromEntries([...new Set(samples.map(s=>s.group))].map(group=>[group,stats(report.results.filter(r=>r.group===group))]));report.worst=[...report.results].sort((a,b)=>b.errorM-a.errorM).slice(0,10);
}catch(e){report.success=false;report.error=String(e.message).replaceAll(key,'[key]').replaceAll(security,'[security]');process.exitCode=1;}
finally{await browser?.close();await new Promise(resolve=>server.close(resolve));fs.mkdirSync(OUT,{recursive:true});fs.writeFileSync(path.join(OUT,'119-coordinate-comparison.json'),JSON.stringify(report,null,2));const header=['sample','group','wgs_lng','wgs_lat','local_lng','local_lat','official_lng','official_lat','error_m','local_inside','official_inside','official_distance_to_boundary_m'];const csv=[header.join(','),...report.results.map(r=>[r.sample,r.group,...r.source,...r.local,...r.official,r.errorM,r.localInside,r.officialInside,r.officialDistanceToBoundaryM].join(','))].join('\n');fs.writeFileSync(path.join(OUT,'119-coordinate-comparison.csv'),'\uFEFF'+csv);console.log(JSON.stringify({success:report.success,error:report.error,sdk:report.sdk,sourceFileRows,sourceFileNearDachang,overall:report.overall,groups:report.groups},null,2));}
})();
