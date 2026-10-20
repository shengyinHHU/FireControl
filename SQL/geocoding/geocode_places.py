"""AMap candidates -> review CSV and guarded UPDATE SQL. Python 3.9+, stdlib only.
No database connection. Network requests only occur when explicitly launched by user.
"""
import argparse, csv, hashlib, io, json, math, os, re, subprocess, sys, time
from datetime import datetime
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen

HERE = Path(__file__).resolve().parent
ENDPOINTS = {'geocode':'https://restapi.amap.com/v3/geocode/geo', 'poi':'https://restapi.amap.com/v3/place/text'}
PRECISE = {'门牌号', '单元号', '门址', '兴趣点', '住宅区'}
TRANSIENT = {'10004','10014','10015','10016','10019','10020','10021','10029'}
class StopRun(Exception): pass
class RequestFailed(Exception): pass

def txt(v):
    return v.strip() if isinstance(v,str) else ''

def atomic_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix+'.tmp')
    tmp.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    tmp.replace(path)

def sql_text(value):
    if value is None: return 'NULL'
    if value == '': return "''"
    return '(CONVERT(0x'+str(value).encode('utf-8').hex()+' USING utf8mb4) COLLATE utf8mb4_general_ci)'

def normalize_name(value):
    return re.sub(r'[\W_]+','',value,flags=re.UNICODE).casefold()

def location(value):
    try:
        parts=value.split(',')
        if len(parts)!=2: return None
        x,y=map(float,parts)
        if not (math.isfinite(x) and math.isfinite(y) and -180<=x<=180 and -90<=y<=90): return None
        if x==0 and y==0: return None
        return round(x,7),round(y,7)
    except (ValueError,TypeError,AttributeError): return None

def load_places(path):
    if path.suffix.lower()=='.csv':
        raw=path.read_bytes()
        try: content=raw.decode('utf-8-sig')
        except UnicodeDecodeError: content=raw.decode('gb18030')
        rows=list(csv.DictReader(io.StringIO(content)))
    else:
        rows=json.loads(path.read_text('utf-8-sig'))
    if not isinstance(rows,list): raise ValueError('输入必须是记录数组或带表头CSV')
    seen=set(); ids=set(); result=[]
    for r in rows:
        item={k:txt(r.get(k)) for k in ['source_key','name','address','normalized_address','place_type']}
        pid=r.get('place_id')
        pid='' if pid is None else str(pid).strip()
        if pid and (not re.fullmatch(r'[1-9]\d*',pid) or int(pid)>9223372036854775807):
            raise ValueError('place_id 必须是数据库原始正整数，不能是Excel格式化数值')
        if pid and pid in ids: raise ValueError('输入含重复place_id')
        if pid: ids.add(pid)
        item['place_id']=pid
        if item['source_key'] and not re.fullmatch('[0-9a-f]{64}',item['source_key']): raise ValueError('source_key 格式不正确')
        if not item['name'] or (not pid and not item['source_key']): raise ValueError('每行必须包含name，以及place_id或source_key')
        item['record_key']=item['source_key'] or 'place_id:'+pid
        if item['record_key'] in seen: raise ValueError('输入含重复来源键')
        seen.add(item['record_key']);result.append(item)
    if not result: raise ValueError('输入没有记录')
    return result

def query_address(row):
    value=row['normalized_address'] or row['address']
    if not value: return ''
    # Preserve original address; never invent a district or force an explicitly different city.
    if '南京' in value or re.search(r'市|省',value): return value
    return '江苏省南京市'+value

def worker():
    # Child process: stdin carries key; no key in process arguments/logs/cache.
    try:
        cfg=json.load(sys.stdin)
        kind=cfg['kind'];params=cfg['params'];params['key']=cfg['key']
        url=ENDPOINTS[kind]+'?'+urlencode(params)
        req=Request(url,headers={'User-Agent':'FireControl-coordinate-review/1.0'})
        with urlopen(req,timeout=cfg['timeout']) as response:
            data=response.read(2*1024*1024+1)
        if len(data)>2*1024*1024: raise ValueError('response too large')
        payload=json.loads(data.decode('utf-8'))
        if not isinstance(payload,dict): raise ValueError('unexpected response')
        print(json.dumps({'ok':True,'data':payload},ensure_ascii=True))
    except Exception as e:
        # HTTP errors can contain the request URL/key: never print exception text.
        print(json.dumps({'ok':False,'error':type(e).__name__}))

class Client:
    def __init__(self,key,out,args):
        self.key=key;self.args=args;self.path=out/'cache.json'
        self.cache=json.loads(self.path.read_text('utf-8')) if self.path.exists() else {}
        self.deadline=time.monotonic()+args.max_seconds;self.last_request=0;self.requests=0
    def check(self):
        if time.monotonic()>=self.deadline: raise StopRun('达到本次总时限；再次运行可从缓存继续')
    def get(self,kind,params):
        fingerprint=hashlib.sha256(json.dumps([kind,params],ensure_ascii=False,sort_keys=True).encode()).hexdigest()
        if fingerprint in self.cache: return self.cache[fingerprint]
        if self.args.offline: raise RequestFailed('离线缓存中尚无该查询')
        for attempt in range(self.args.retries+1):
            self.check()
            wait=max(0,self.args.interval-(time.monotonic()-self.last_request))
            if wait:
                time.sleep(min(wait,max(0,self.deadline-time.monotonic())))
            self.check()
            remaining=self.deadline-time.monotonic()
            wall=min(self.args.timeout+2,remaining)
            self.last_request=time.monotonic();self.requests+=1
            try:
                response=subprocess.run([sys.executable,str(Path(__file__).resolve()),'--http-worker'],
                    input=json.dumps({'kind':kind,'params':dict(params),'key':self.key,'timeout':self.args.timeout}),
                    capture_output=True,text=True,encoding='utf-8',timeout=wall,
                    creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
                payload=json.loads(response.stdout)
                if not payload.get('ok'): raise RequestFailed('网络请求失败：'+str(payload.get('error','unknown')))
                data=payload['data']
                if str(data.get('status'))!='1':
                    code=str(data.get('infocode','unknown'))
                    if code not in TRANSIENT:
                        raise StopRun('高德返回错误码 '+code+'；停止请求，请检查Web服务Key、服务权限、IP及额度。')
                    raise RequestFailed('高德限流或暂时不可用：'+code)
                self.cache[fingerprint]=data
                atomic_json(self.path,self.cache)
                return data
            except subprocess.TimeoutExpired:
                problem='单次请求已到硬超时，子进程已结束'
            except (json.JSONDecodeError,RequestFailed):
                problem='网络请求失败或接口暂时不可用'
            if attempt==self.args.retries: raise RequestFailed(problem)
        raise RequestFailed('请求失败')

def candidates_from(data,kind,row):
    results=[]
    for raw in data.get('geocodes' if kind=='geocode' else 'pois',[]) or []:
        if not isinstance(raw,dict): continue
        xy=location(raw.get('location'))
        if xy is None: continue
        code=txt(raw.get('adcode'))
        city=txt(raw.get('city' if kind=='geocode' else 'cityname'))
        local=code.startswith('3201') if code else city in ('南京','南京市')
        name=txt(raw.get('name')) if kind=='poi' else txt(raw.get('formatted_address'))
        level=txt(raw.get('level'))
        address=txt(raw.get('formatted_address')) if kind=='geocode' else txt(raw.get('pname'))+city+txt(raw.get('adname'))+txt(raw.get('address'))
        precise=level in PRECISE if kind=='geocode' else normalize_name(name)==normalize_name(row['name'])
        item={'method':'amap_geocode' if kind=='geocode' else 'amap_poi',
            'name':name,'matched_address':address,'longitude':xy[0],'latitude':xy[1],
            'adcode':code,'level':level,'poi_id':txt(raw.get('id')),'in_nanjing':local,'eligible':local and precise}
        item['candidate_id']=hashlib.sha256(json.dumps([row['record_key'],item],sort_keys=True,ensure_ascii=False).encode()).hexdigest()[:24]
        if not any(c['candidate_id']==item['candidate_id'] for c in results): results.append(item)
    return results

def resolve(row,client):
    result={'row':row,'query_address':query_address(row),'candidates':[],'selected_id':'','reason':''}
    if result['query_address']:
        geo=client.get('geocode',{'address':result['query_address'],'city':'南京市','output':'JSON'})
        result['candidates']=candidates_from(geo,'geocode',row)
        eligible=[c for c in result['candidates'] if c['eligible']]
        if len(eligible)==1 and len(result['candidates'])==1:
            result['selected_id']=eligible[0]['candidate_id'];result['reason']='唯一精细地址候选，仍需地图核验'
            return result
    poi=client.get('poi',{'keywords':row['name'],'city':'南京市','citylimit':'true','offset':'5','page':'1','extensions':'base','output':'JSON'})
    found=candidates_from(poi,'poi',row);result['candidates'].extend(found)
    eligible=[c for c in found if c['eligible']]
    try: count=int(poi.get('count',0))
    except (ValueError,TypeError): count=999
    # Never claim uniqueness if there are unseen pages or conflicting precise geocodes.
    if len(eligible)==1 and count<=5 and not any(c['eligible'] for c in result['candidates'] if c['method']=='amap_geocode'):
        result['selected_id']=eligible[0]['candidate_id'];result['reason']='同名本市POI唯一候选，地址仍需核验'
    else: result['reason']='结果含歧义、精度不足、异地或无匹配；不自动输出更新'
    return result

def selected_csv(path):
    raw=path.read_bytes()
    try: content=raw.decode('utf-8-sig')
    except UnicodeDecodeError: content=raw.decode('gb18030')
    chosen={};known=set()
    for r in csv.DictReader(io.StringIO(content)):
        record=txt(r.get('record_key'));candidate=txt(r.get('candidate_id'))
        if not record: continue
        known.add(record)
        if txt(r.get('use')).lower() in ('yes','y','1','是'):
            if record in chosen: raise ValueError('同一个场所只能选择一个候选')
            chosen[record]=candidate
    return chosen,known

def generate_sql(results):
    stamp=datetime.now().strftime('%Y%m%d_%H%M%S')
    marker='amap_candidate_'+stamp
    lines=['-- 高德候选坐标；执行前审查CSV。只写待核验坐标，不连接地图或自动确认。',
        '-- 已有坐标、已核验、停用/删除或名称地址已变更的记录均跳过。',
        'SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;','USE `dachang`;','START TRANSACTION;','SET @amap_updated = 0;']
    count=0
    for result in results:
        selected=[c for c in result['candidates'] if c['candidate_id']==result['selected_id']]
        if not selected: continue
        c=selected[0];r=result['row']
        guard=['`name` = '+sql_text(r['name']), '`address` <=> '+sql_text(r['address'] or None)]
        # CSV represents the current DB snapshot; source-only mode represents the seed snapshot.
        if r['source_key']: guard.append('`source_key` = '+sql_text(r['source_key']))
        if r['place_type']: guard.append('`place_type` = '+sql_text(r['place_type']))
        if r['place_id']: pid=r['place_id']
        else:
            lines.append('SET @fire_place_id = (SELECT `place_id` FROM `fire_place` WHERE '+ ' AND '.join(guard)+');')
            pid='@fire_place_id'
        label=re.sub(r'[\r\n]+',' ',r['name'])
        lines.append('-- '+label+' | '+c['candidate_id']+' | '+c['method'])
        assignments=['`longitude` = '+format(c['longitude'],'.7f'),'`latitude` = '+format(c['latitude'],'.7f'),
            "`coordinate_system` = 'GCJ02'",'`coordinate_source` = '+sql_text(c['method']),
            "`location_status` = 'review'",'`matched_address` = '+sql_text(c['matched_address'] or None),
            '`amap_poi_id` = '+sql_text(c['poi_id'] or None),'`adcode` = '+sql_text(c['adcode'] or None),
            '`update_by` = '+sql_text(marker),'`update_time` = NOW()']
        lines.append('UPDATE `fire_place` SET\n  '+',\n  '.join(assignments)+'\nWHERE `place_id` = '+pid+'\n  AND '+ '\n  AND '.join(guard)+
            "\n  AND `longitude` IS NULL AND `latitude` IS NULL\n  AND `location_status` IN ('pending','failed')\n  AND `location_checked_by` IS NULL AND `location_checked_at` IS NULL\n  AND `status` = '0' AND `del_flag` = '0';")
        lines.append('SET @amap_updated = @amap_updated + ROW_COUNT();');count+=1
    lines+=['SELECT @amap_updated AS actual_updated_count, '+str(count)+' AS generated_candidate_count;','COMMIT;']
    return '\n\n'.join(lines)+'\n',count

def export(results,out,reason,requests):
    out.mkdir(parents=True,exist_ok=True)
    fields=['record_key','place_id','source_key','institution','original_address','query_address','candidate_id','use','method','candidate_name','matched_address','longitude','latitude','adcode','level','in_nanjing','reason']
    buf=io.StringIO(newline='');writer=csv.DictWriter(buf,fieldnames=fields);writer.writeheader()
    for result in results:
        r=result['row']
        for c in result['candidates'] or [{}]:
            writer.writerow(dict(record_key=r['record_key'],place_id=r['place_id'],source_key=r['source_key'],institution=r['name'],original_address=r['address'],query_address=result['query_address'],candidate_id=c.get('candidate_id',''),use='yes' if c.get('candidate_id')==result['selected_id'] and result['selected_id'] else '',method=c.get('method',''),candidate_name=c.get('name',''),matched_address=c.get('matched_address',''),longitude=c.get('longitude',''),latitude=c.get('latitude',''),adcode=c.get('adcode',''),level=c.get('level',''),in_nanjing=c.get('in_nanjing',''),reason=result['reason']))
    # Review exports use a separate filename so manual edits to candidates.csv survive.
    suffix='_reviewed' if reason.get('review_mode') else ''
    (out/('candidates'+suffix+'.csv')).write_text(buf.getvalue(),encoding='utf-8-sig')
    sql,count=generate_sql(results)
    (out/('update_coordinates'+suffix+'.sql')).write_text(sql,encoding='utf-8')
    atomic_json(out/('summary'+suffix+'.json'),dict(records=len(results),sql_candidates=count,unresolved=len(results)-count,network_requests=requests,**reason))
    print('已输出：'+str(out/('update_coordinates'+suffix+'.sql')),flush=True)
    print('记录 '+str(len(results))+'，候选更新 '+str(count)+'，本次网络请求 '+str(requests),flush=True)

def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input',type=Path,default=HERE/'places_input.json')
    parser.add_argument('--output',type=Path,default=HERE/'output')
    parser.add_argument('--limit',type=int,default=101)
    parser.add_argument('--max-seconds',type=float,default=300)
    parser.add_argument('--timeout',type=float,default=8)
    parser.add_argument('--interval',type=float,default=1)
    parser.add_argument('--retries',type=int,default=1)
    parser.add_argument('--offline',action='store_true')
    parser.add_argument('--review-csv',type=Path,help='只用缓存重新生成SQL，use列填yes；不联网')
    args=parser.parse_args(argv)
    if not (1<=args.limit<=10000 and 1<=args.max_seconds<=3600 and 1<=args.timeout<=30 and .2<=args.interval<=60 and 0<=args.retries<=2):
        parser.error('参数超出范围；timeout 1..30、retries 0..2、总时限 1..3600')
    if args.review_csv: args.offline=True
    rows=load_places(args.input)[:args.limit]
    key=os.environ.get('AMAP_WEB_SERVICE_KEY','').strip()
    config=HERE/'config.local.json'
    if not key and config.exists(): key=txt(json.loads(config.read_text('utf-8-sig')).get('web_service_key'))
    if not args.offline and not re.fullmatch(r'[0-9a-fA-F]{32}',key):
        raise ValueError('请设置AMAP_WEB_SERVICE_KEY或config.local.json中的Web服务类型Key；程序不读取前端JS Key')
    args.output.mkdir(parents=True,exist_ok=True)
    client=Client(key,args.output,args);results=[];stopped='';fatal=False
    overrides,known=selected_csv(args.review_csv) if args.review_csv else ({},set())
    try:
        for i,row in enumerate(rows,1):
            try:
                client.check();result=resolve(row,client)
            except RequestFailed as e:
                result={'row':row,'query_address':query_address(row),'candidates':[],'selected_id':'','reason':str(e)}
            results.append(result)
            print('['+str(i)+'/'+str(len(rows))+'] '+row['name']+'：'+result['reason'],flush=True)
    except (StopRun,KeyboardInterrupt) as e:
        stopped='用户中断' if isinstance(e,KeyboardInterrupt) else str(e)
        fatal=isinstance(e,StopRun) and '错误码' in str(e)
        print(stopped+'；保存已有结果。',flush=True)
    if len(results)<len(rows):
        for row in rows[len(results):]:
            results.append({'row':row,'query_address':query_address(row),'candidates':[],'selected_id':'','reason':'本次尚未完成；重跑可续用缓存'})
    if args.review_csv:
        bykey={r['row']['record_key']:r for r in results}
        if not known.issubset(bykey): raise ValueError('审查CSV包含不属于当前输入的场所')
        for result in results: result['selected_id']=''
        for record,cid in overrides.items():
            result=bykey[record]
            if not any(c['candidate_id']==cid and c['in_nanjing'] for c in result['candidates']):
                raise ValueError('审查选择不在当前候选中、坐标无效或不在南京；请核对输入及缓存')
            result['selected_id']=cid;result['reason']='人工选择候选；导入后仍标记待地图核验'
    export(results,args.output,{'stopped_reason':stopped,'review_mode':bool(args.review_csv),'input':str(args.input),'coordinate_status':'review'},client.requests)
    return 2 if fatal else (3 if stopped else 0)

if __name__=='__main__':
    if sys.argv[1:]==['--http-worker']:
        worker()
    else:
        try: sys.exit(main())
        except (ValueError,OSError,json.JSONDecodeError) as e:
            print('错误：'+str(e),file=sys.stderr);sys.exit(1)