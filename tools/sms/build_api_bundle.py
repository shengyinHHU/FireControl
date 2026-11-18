"""Generate portable public API contracts and a secret-free caller ZIP from an explicit allowlist."""
from pathlib import Path
import json, zipfile

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'docs/sms-api'
def write(name,data):
    (OUT/name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n','utf-8')
def event(kind,script): return dict(listen=kind,script=dict(type='text/javascript',exec=script.strip().splitlines()))

guard="""
function stop(message) { pm.execution.skipRequest(); throw new Error(message); }
const root = String(pm.environment.get('baseUrl') || '');
if (!/^https?:\/\//.test(root) || root.endsWith('/')) stop('请设置无末尾斜杠的 API 根地址 baseUrl');
"""
login_pre=guard+"""
const username = String(pm.environment.get('username') || '');
const password = String(pm.environment.get('password') || '');
if (!username || !password) stop('请在选中的环境中填写 username 和 password');
pm.variables.set('tokenPayload', JSON.stringify({username, password}));
"""
login_test="""
pm.test('获取 Token 成功', function () {
    pm.response.to.have.status(200);
    const r = pm.response.json();
    pm.expect(r.code).to.eql(200);
    pm.expect(r.token).to.be.a('string').and.not.empty;
    pm.environment.set('token', r.token);
    pm.environment.set('expiresIn', String(r.expiresIn));
});
if (pm.response.code !== 200 || pm.response.json().code !== 200) pm.environment.unset('token');
"""
send_pre=guard+"""
if (!pm.environment.get('token')) stop('请先执行 01 获取 Token');
const mobiles = String(pm.environment.get('mobiles') || '').split(',').map(x => x.trim()).filter(Boolean);
if (mobiles.length < 1 || mobiles.length > 10 || mobiles.some(x => !/^1[3-9]\\d{9}$/.test(x)) || new Set(mobiles).size !== mobiles.length) stop('请填写 1 至 10 个真实手机号，用英文逗号分隔且不重复');
const content = String(pm.environment.get('content') || '');
if (!content.trim()) stop('请填写短信正文 content');
const bizId = String(pm.environment.get('bizId') || '');
if (bizId.length > 100) stop('bizId 长度不能超过 100');
let requestId = String(pm.environment.get('requestId') || '');
if (!requestId) { requestId = pm.variables.replaceIn('{{$guid}}').toLowerCase(); pm.environment.set('requestId', requestId); }
if (!/^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/.test(requestId)) stop('requestId 必须是小写 UUID');
pm.variables.set('requestId', requestId);
pm.variables.set('smsPayload', JSON.stringify({requestId, mobiles, content, bizId}));
"""
result_test="""
pm.test('服务处理成功（另看 taskStatus 确认平台受理状态）', function () {
    pm.response.to.have.status(200);
    const r = pm.response.json();
    pm.expect(r.code).to.eql(200);
    pm.expect(r.data.requestId).to.eql(pm.environment.get('requestId'));
    pm.expect(['submitted','partial','failed','pending','unknown']).to.include(r.data.taskStatus);
});
"""
query_pre=guard+"""
if (!pm.environment.get('token')) stop('请先获取 Token');
const requestId = String(pm.environment.get('requestId') || '');
if (!/^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/.test(requestId)) stop('请填写原发送使用的 requestId');
pm.variables.set('requestId', requestId);
"""
def item(name,method,path,pre,test,body=None,noauth=False):
    parts=['fire','sms','open']+path.split('/')
    request=dict(method=method,header=[dict(key='Accept',value='application/json')],url=dict(raw='{{baseUrl}}/fire/sms/open/'+path,host=['{{baseUrl}}'],path=parts))
    if noauth: request['auth']={'type':'noauth'}
    if body:
        request['header'].append(dict(key='Content-Type',value='application/json'))
        request['body']=dict(mode='raw',raw=body,options=dict(raw=dict(language='json')))
    return dict(name=name,event=[event('prerequest',pre),event('test',test)],request=request,response=[])
write('FireControl-SMS.postman_collection.json',dict(info=dict(name='FireControl 短信服务',description='先获取 Token，再发送和查询。新短信清空 requestId；重试保留原编号。实际发送会消耗短信额度。详见随附说明。',schema='https://schema.getpostman.com/json/collection/v2.1.0/collection.json'),auth=dict(type='bearer',bearer=[dict(key='token',value='{{token}}',type='string')]),item=[item('01 获取 Token','POST','token',login_pre,login_test,'{{tokenPayload}}',True),item('02 发送短信','POST','send',send_pre,result_test,'{{smsPayload}}'),item('03 查询结果','GET','requests/{{requestId}}',query_pre,result_test)]))
values=dict(baseUrl='http://localhost:8080',username='sms_api',password='',token='',expiresIn='',mobiles='',content='您好，这是一条短信接口测试。',bizId='',requestId='')
write('FireControl-SMS.postman_environment.json',dict(name='FireControl SMS（填写本地值）',values=[dict(key=k,value=v,type='secret' if k in ('password','token') else 'default',enabled=True)for k,v in values.items()],_postman_variable_scope='environment'))

ref=lambda name:{'$ref':'#/components/schemas/'+name}
string=lambda desc='',**kw:dict(type='string',description=desc,**kw)
uuid_schema=string('同一发送意图沿用此编号；新发送生成新编号',format='uuid',pattern='^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$')
phones=dict(type='array',items=string(pattern='^1[3-9][0-9]{9}$'),minItems=1,maxItems=10,uniqueItems=True)
task_properties=dict(recordId=string('按字符串使用'),requestId=uuid_schema,recipientType=string(enum=['external']),mobiles=phones,content=string('含服务端固定签名'),recipientName=string(),sourceName=string(),taskStatus=string(enum=['submitted','partial','failed','pending','unknown']),deliveryStatus=string(enum=['unconfirmed']),platformCode=string(),resultText=string(),platformTaskId=string(),acceptedPhones=dict(type='array',items=string()),bizId=string(maxLength=100),createTime=string('数据库格式化时间 yyyy-MM-dd HH:mm:ss'))
schemas=dict(
    Login=dict(type='object',additionalProperties=False,required=['username','password'],properties=dict(username=string(minLength=2,maxLength=20),password=string(minLength=5,maxLength=20,writeOnly=True))),
    Token=dict(type='object',required=['code','msg','token','tokenType','expiresIn'],properties=dict(code=dict(type='integer',enum=[200]),msg=string(),token=string(),tokenType=string(enum=['Bearer']),expiresIn=dict(type='integer',description='秒；若依活跃会话可滑动续期，以401判断失效'))),
    Send=dict(type='object',additionalProperties=False,required=['requestId','mobiles','content'],properties=dict(requestId=uuid_schema,mobiles=phones,content=string('正文原文；不加签名、不编码，含签名默认总长1000 UTF-16字符',minLength=1,maxLength=4000),bizId=string('可选业务号，不用于去重',maxLength=100))),
    Task=dict(type='object',required=list(task_properties),properties=task_properties),
    Result=dict(type='object',required=['code','msg','data'],properties=dict(code=dict(type='integer',enum=[200]),msg=string(),data=ref('Task'))),
    Error=dict(type='object',required=['code','msg'],properties=dict(code=dict(type='integer'),msg=string())))
def response(name,desc):return dict(description=desc,content={'application/json':dict(schema=ref(name))})
errors={str(n):response('Error',desc)for n,desc in [(400,'参数无效'),(401,'Token失效或登录失败'),(403,'账号角色/权限/来源限制'),(404,'本账号未找到记录'),(409,'requestId内容冲突'),(429,'限频或额度不足'),(500,'结果不可靠：保留编号先查询'),(503,'服务或依赖未就绪')]}
errors['429']['headers']={'Retry-After':dict(description='重试等待秒数',schema=dict(type='integer'))}
def operation(name,desc,success='Result',body=None):
    op=dict(operationId=name,summary=desc,responses={'200':response(success,'成功处理；发送仍需检查taskStatus'),**errors})
    if body:op['requestBody']=dict(required=True,content={'application/json':dict(schema=ref(body))})
    return op
token_op=operation('getSmsToken','短信专用账号登录；内部复用若依鉴权','Token','Login');token_op['security']=[]
query_op=operation('getSmsRequest','只查询本账号的开放提交记录');query_op['parameters']=[dict(name='requestId',**{'in':'path'},required=True,schema=uuid_schema)]
write('openapi.json',dict(openapi='3.0.3',info=dict(title='FireControl 短信开放服务',version='1.0.0',description='账号密码换Token → 提交短信 → 按requestId查询。禁止未知结果自动换编号重发；submitted仅表示平台受理。'),servers=[dict(url='http://localhost:8080',description='替换为实际部署API根地址')],security=[dict(bearerAuth=[])],paths={'/fire/sms/open/token':dict(post=token_op),'/fire/sms/open/send':dict(post=operation('sendSms','持久化去重和预留额度后向平台发送',body='Send')),'/fire/sms/open/requests/{requestId}':dict(get=query_op)},components=dict(securitySchemes=dict(bearerAuth=dict(type='http',scheme='bearer',bearerFormat='JWT')),schemas=schemas)))

PUBLIC=['README.md','短信服务接口调用说明.md','FireControl-SMS.postman_collection.json','FireControl-SMS.postman_environment.json','openapi.json','sms_client.py']
with zipfile.ZipFile(ROOT/'docs/sms-api-caller.zip','w',zipfile.ZIP_DEFLATED)as archive:
    for name in PUBLIC:archive.write(OUT/name,'sms-api/'+name)
print('Generated public contracts and caller ZIP ('+str(len(PUBLIC))+' files, no credentials).')
