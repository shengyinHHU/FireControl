from pathlib import Path
root=Path(r'D:\Downloads\FireControl\RuoYi-Vue3')
pending=[]
def save(name,s):
 p=root/name
 try: p.write_text(s,encoding='utf-8');print('Saved '+name)
 except PermissionError:
  Path(str(p)+'.local-handle.pending').write_text(s,encoding='utf-8');pending.append(name);print('Prepared '+name)
def read(name):return (root/name).read_text(encoding='utf-8-sig')
name='src/api/fire/localAlarmHandling.js';s=read(name).replace("window.addEventListener('storage'", "window.addEventListener?.('storage'");save(name,s)
name='src/api/fire/rules.js';s=read(name);s="import { isLocallyHandled } from './localAlarmHandling.js'\n"+s
s=s.replace("alarms.filter(a => a.sourceCode !== '119')", "alarms.filter(a => a.sourceCode !== '119' && !isLocallyHandled(a))")
s=s.replace("export const isActiveFire = a => a.alarmType", "export const isActiveFire = a => !isLocallyHandled(a) && a.alarmType")
s=s.replace("export const isDisplayAlarm = a => a.sourceCode === '119' || isActiveFire(a)", "export const isDisplayAlarm = a => !isLocallyHandled(a) && (a.sourceCode === '119' || isActiveFire(a))");save(name,s)
name='src/api/fire/deviceAccess.js';s="import { visibleLocalAlarms } from './localAlarmHandling.js'\n"+read(name)
s=s.replace('return { ...res, rows: res.rows.map(normalizeDeviceRow) }', "const rows = res.rows.map(normalizeDeviceRow)\n  return { ...res, rows: resource === 'alarms' ? visibleLocalAlarms(rows) : rows }");save(name,s)
name='src/views/fire/dashboard/index.vue';s=read(name)
s=s.replace('<script setup>',"<script setup>\nimport { USE_FIXED_PLACE_COUNTS, FIXED_PLACE_COUNTS } from '@/api/fire/dashboardDisplay'")
s=s.replace('v-for="(s, i) in stats.categories || []"','v-for="(s, i) in (USE_FIXED_PLACE_COUNTS ? FIXED_PLACE_COUNTS : stats.categories || [])"')
s=s.replace('categories: PLACE_LAYERS.map(', 'categories: USE_FIXED_PLACE_COUNTS ? FIXED_PLACE_COUNTS : PLACE_LAYERS.map(')
s=s.replace('>查看事件</el-button>', '>查看 / 处理</el-button>');save(name,s)
name='src/components/Fire/AlarmDetail.vue';s=read(name)
s=s.replace('<script setup>',"<script setup>\nimport { handleAlarmLocally } from '@/api/fire/localAlarmHandling'")
s=s.replace('119平台案件记录，保留原始案件状态；实时同步，不在本平台模拟接单或办结。','119 原始案件状态保持同步，本地处理不会修改来源系统。')
s=s.replace('    </div>\n    <PlaceDetail', '''      <section v-if="alarm?.dataSource === 'database' && canFire('alarms')" style="margin-top:24px">
        <h3>处理说明</h3>
        <el-input v-model="note" type="textarea" :rows="4" maxlength="1000" show-word-limit
          :disabled="saving" aria-label="处理说明" placeholder="请填写核查情况及处理结果（必填）" />
        <p class="muted">处理后在当前浏览器隐藏，刷新后仍保留；清除浏览器数据或更换设备后需重新处理。</p>
        <el-button type="primary" :loading="saving" :disabled="!note.trim()" @click="handleLocal">确认处理</el-button>
      </section>
    </div>
    <PlaceDetail''')
pos=s.index('async function transition()')
s=s[:pos]+'''async function handleLocal() {
  if (saving.value || !alarm.value || alarm.value.id !== props.alarmId) return
  const target = alarm.value
  saving.value = true
  try {
    const result = await attempt(async () => { handleAlarmLocally(target, note.value); return true }, '处理完成，该警情已隐藏')
    if (result) { if (props.alarmId === target.id) { note.value = ''; open.value = false }; emit('changed') }
  } finally { saving.value = false }
}
'''+s[pos:];save(name,s)
name='src/components/Fire/DeviceAccessView.vue';s=read(name)
s=s.replace('<script setup>',"<script setup>\nimport { visibleLocalAlarms } from '@/api/fire/localAlarmHandling'")
s=s.replace(':data="rows"', ':data="tab === \'alarms\' ? visibleLocalAlarms(rows) : rows"')
s=s.replace(':alarm-id="selectedId" />', ':alarm-id="selectedId" @changed="load" />');save(name,s)
import json,hashlib
manifest=[{'path':name,'sha256':hashlib.sha256((root/name).read_bytes()).hexdigest()} for name in pending]
(root/'local-handling-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
