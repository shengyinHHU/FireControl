import hashlib,json,re,unicodedata
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2];OUT=Path(__file__).resolve().parent
source=json.loads((OUT/'source.json').read_text(encoding='utf-8'))
comparison=json.loads((ROOT/'outputs/highrise-comparison-20261125/comparison.json').read_text(encoding='utf-8'))
preflight=json.loads((OUT/'preflight.json').read_text(encoding='utf-8'))
old={p['place_id']:p for p in preflight['highrise']}
cache=json.loads((OUT/'coordinate_cache.json').read_text(encoding='utf-8'))
tasks=json.loads((OUT/'coordinate_tasks.json').read_text(encoding='utf-8'))
pois={p['id']:p for value in cache.values()for p in value.get('pois',[])}
boundary=json.loads((ROOT/'RuoYi-Vue3/src/assets/geo/dachang-boundary.json').read_text(encoding='utf-8'))['geometry']

def inside(x,y):
    def ring(r):
        result=False
        for (a,b),(c,d)in zip(r,r[1:]+r[:1]):
            if (b>y)!=(d>y) and x<(c-a)*(y-b)/(d-b)+a:result=not result
        return result
    return any(ring(p[0]) and not any(ring(h)for h in p[1:])for p in boundary['coordinates'])

def clean(value):
    if value is None:return None
    s=re.sub(r'\s+','',str(value))
    return None if s in ['','/','／','无'] else s

numbers={2:'二',3:'三',4:'四',5:'五',7:'七',8:'八',9:'九',10:'十',11:'十一',12:'十二',15:'十五',16:'十六',17:'十七',18:'十八'}
selected={}
for n,zh in numbers.items():
    expected='扬子'+zh+'村'
    candidates=[p for p in pois.values()if p['name']==expected]
    if n==17:candidates=[pois['B0FFFZDTWC']]
    assert len(candidates)==1,(n,candidates)
    selected['扬子'+str(n)+'村']=(candidates[0],'高德小区本体代表点' if n!=17 else '高德扬子十七村北区代表点，覆盖原表扬子17村条目，未精确到楼栋')
choices={'紫景园':'B00190AJAL','草芳新苑（含晓山北村）':'B00190D2M0','欣乐南村':'B0LR35CDLK','欣乐北村':'B00190BWQ9','南钢二村':'B0FFGMNJ6G','新华四村':'B00190YJNS','九村':'B0FFF2H26Y','新华三村':'B0FFG02MHU','新华五村':'B00190YJNT','新华六村':'B00190CQX3','新华二村（1-13栋）':'B0FFHX7ZJ1'}
for name,poi in choices.items():
    reason='复用高德小区本体代表点'
    if name.startswith('新华二村'):reason='高德新华二村东区代表点；搜索结果地址明确包含东区2幢，属于1-13栋范围；未逐栋核验'
    if name.startswith('草芳'):reason='高德草芳社区住宅小区代表点（草芳路69号），草芳新苑店铺地址同属草芳路69号；含晓山北村的整个条目采用一个代表点'
    if name=='欣乐北村':reason='高德欣乐新村毕洼路234号代表点；与北区店铺地址对应，区别于园西路南区'
    selected[name]=(pois[poi],reason)
geotask=next(t for t in tasks if t['kind']=='geocode')
geocodes=cache[geotask['fingerprint']]['geocodes']
geo=next(g for g in geocodes if g['level']=='门址')
selected['晓山公寓']=(dict(id=None,name='晓山公寓（杨新路195号门址）',location=geo['location'],address=geo['formatted_address']), '按Excel完整门牌查询高德，优先采用门址级候选；另有门牌号候选，待现场核验')

byrow={s['row']:s for s in comparison['source']}
rows=[]
for s in source['rows']:
    f=s['fields'];name=clean(s['name']);matches=byrow[s['row']]['matches']
    assert len(matches)<=1
    ident=matches[0]['id']if matches else None
    current=old.get(ident,{})
    coord=None
    if current.get('longitude') is not None and current.get('latitude') is not None:
        coord=dict(lng=float(current['longitude']),lat=float(current['latitude']),source=current['coordinate_source'],poi_id=current['amap_poi_id'],matched_address=current['matched_address'],status=current['location_status'],reason='保留已有GCJ02坐标',matched_name=current['name'])
    else:
        p,reason=selected[name];lng,lat=map(float,p['location'].split(','))
        coord=dict(lng=lng,lat=lat,source='amap_geocode' if name=='晓山公寓' else 'amap_poi',poi_id=p['id'],matched_address=p['address'],status='review',reason=reason,matched_name=p['name'])
    assert 118.70<coord['lng']<118.82 and 32.18<coord['lat']<32.28,(name,'outside Dachang vicinity',coord)
    coord['inside_project_boundary']=inside(coord['lng'],coord['lat'])
    high=f['高层\n（栋数）'];area=f['建筑面积（㎡）']
    assert isinstance(high,(int,float)) and high>=0 and int(high)==high
    summary_keys=['房屋性质','管理模式','小区户数','小区人数（万）','多层\n（栋数）','电梯\n（部）','充电桩\n（个）','交付时间','物业收费标准','车位数量\n（个）']
    remark='来源：大厂!D'+str(s['row'])+'；'+ '；'.join(k.replace('\n','')+'：'+str(f[k]).replace('\n',' ')for k in summary_keys if f[k] is not None)
    if s['row'] in range(57,78):remark+='；物业、交付、收费及停车等共享数据来自原表合并单元格，不应跨村相加'
    assert len(remark)<=500,(name,len(remark))
    source_key=hashlib.sha256((source['sha256']+'|大厂|'+str(s['row'])).encode()).hexdigest()
    rows.append(dict(row=s['row'],id=ident,name=name,source_key=source_key,community=clean(f['所属社区']),address=clean(f['项目地址']),management=clean(f['服务主体']),category=clean(f['小区类型']),area=area,highrise_count=int(high),person=clean(f['项目经理']),phone=clean(f['联系方式']),remark=remark,coordinate=coord,raw=s))
retire=[dict(id=12,target=268),dict(id=15,target=274),dict(id=39,target=325),dict(id=41,target=326),dict(id=327,target=None)]
assert len(rows)==len({r['name']for r in rows})==101
assert len([r for r in rows if r['id']is None])==15
assert len({r['id']for r in rows if r['id']is not None})==86
assert {r['id']for r in rows if r['id']is not None}|{r['id']for r in retire}==set(old)
plan=dict(source_file=source['file'],source_sha256=source['sha256'],sheet='大厂',rows=rows,retire=retire,summary=dict(update=86,insert=15,retire=5,final=101,coordinates_preserved=75,coordinates_added=26))
(OUT/'plan.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(plan['summary'],ensure_ascii=False))
for r in rows:
    if r['coordinate']['reason']!='保留已有GCJ02坐标':print(json.dumps(dict(name=r['name'],**r['coordinate']),ensure_ascii=False))
