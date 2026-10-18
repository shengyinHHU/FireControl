<template>
  <div class="fire-page fire-theme">
    <div class="fire-page-title">
      <div>
        <div class="fire-kicker">EMERGENCY RESPONSE</div>
        <h1>警情处置中心</h1>
        <p>119警情、消防主机与AI预警统一接入，记录处置全过程</p>
      </div>
      <el-button @click="load">刷新警情</el-button>
    </div>
    <div class="alarm-stats">
      <button
        v-for="s in summary"
        :key="s.name"
        :class="{ selected: query.status === s.name }"
        @click="
          () => {
            query.status = s.name === '全部警情' ? '' : s.name
            search()
          }
        "
      >
        <span>{{ s.name }}</span
        ><strong>{{ s.value }}</strong
        ><i :class="s.class"></i>
      </button>
    </div>
    <div class="fire-table-card">
      <div class="fire-filters">
        <el-input
          v-model="query.keyword"
          placeholder="搜索警情编号 / 场所 / 内容"
          clearable
          @keyup.enter="search"
        /><el-select
          popper-class="fire-dialog"
          v-model="query.source"
          clearable
          placeholder="全部来源"
          @change="search"
          ><el-option
            v-for="s in sources"
            :key="s"
            :value="s"
            :label="s" /></el-select
        ><el-select
          popper-class="fire-dialog"
          v-model="query.status"
          clearable
          placeholder="全部状态"
          @change="search"
          ><el-option
            v-for="s in ['待处理', '处理中', '已办结']"
            :key="s"
            :value="s" /></el-select
        ><el-date-picker
          popper-class="fire-dialog"
          v-model="dates"
          type="daterange"
          value-format="YYYY-MM-DD"
          @change="dateChange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        /><el-button type="primary" plain @click="search">查询</el-button
        ><el-button @click="reset">重置</el-button>
      </div>
      <div v-if="error" class="fire-error">
        {{ error }}<el-button link @click="load">重试</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe
        ><el-table-column label="警情内容" min-width="290"
          ><template #default="{ row }"
            ><div style="color: #d5e9f5">{{ row.name }}</div>
            <small class="muted">{{ row.code }}</small></template
          ></el-table-column
        ><el-table-column
          prop="placeName"
          label="发生场所"
          min-width="180"
        /><el-table-column
          prop="source"
          label="来源"
          min-width="140"
        /><el-table-column
          prop="occurredAt"
          label="发生时间"
          width="180"
        /><el-table-column label="状态" width="110"
          ><template #default="{ row }"
            ><StatusTag :value="row.status" /></template></el-table-column
        ><el-table-column label="操作" width="145" fixed="right"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="show(row.id)"
              >查看 / 处置</el-button
            ></template
          ></el-table-column
        ></el-table
      ><el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </div>
    <AlarmDetail v-model="open" :alarm-id="id" @changed="refresh" />
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { fire } from '@/api/fire'
import { useList, attempt } from '../useFire'
import StatusTag from '@/components/Fire/StatusTag.vue'
import AlarmDetail from '@/components/Fire/AlarmDetail.vue'
const route = useRoute(),
  sources = ['119警情', '消控室主机', 'AI监测预警'],
  dates = ref([]),
  stats = ref({}),
  open = ref(false),
  id = ref('')
const { rows, total, loading, error, query, load, search } = useList('alarms', {
  source: route.query.source || '',
  status: route.query.status || '',
})
const summary = computed(() => [
  {
    name: '全部警情',
    value:
      (stats.value.pending || 0) +
      (stats.value.processing || 0) +
      (stats.value.closed || 0),
    class: 'all',
  },
  { name: '待处理', value: stats.value.pending || 0, class: 'pending' },
  { name: '处理中', value: stats.value.processing || 0, class: 'processing' },
  { name: '已办结', value: stats.value.closed || 0, class: 'closed' },
])
async function refresh() {
  load()
  const res = await attempt(() => fire.dashboard())
  if (res) stats.value = res.data
}
function show(v) {
  id.value = v
  open.value = true
}
function dateChange() {
  query.dateFrom = dates.value?.[0] || ''
  query.dateTo = dates.value?.[1] || ''
  search()
}
function reset() {
  query.keyword = ''
  query.source = ''
  query.status = ''
  dates.value = []
  dateChange()
}
onMounted(() => {
  refresh()
  if (route.query.id) show(String(route.query.id))
})
</script>
<style scoped>
.alarm-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.alarm-stats button {
  background: #0b2942;
  border: 1px solid #20445b;
  border-radius: 4px;
  color: #9bb6cb;
  text-align: left;
  padding: 20px 24px;
  position: relative;
  cursor: pointer;
}
.alarm-stats button.selected {
  border-color: #2ba8c2;
}
.alarm-stats strong {
  display: block;
  font-size: 32px;
  color: #d4eaf7;
  font-family: Consolas, monospace;
  margin-top: 10px;
}
.alarm-stats i {
  position: absolute;
  width: 5px;
  height: 24px;
  right: 24px;
  top: 34px;
  background: #26c7de;
}
.alarm-stats .pending {
  background: #edb95b;
}
.alarm-stats .processing {
  background: #688fec;
}
.alarm-stats .closed {
  background: #38c59e;
}
@media (max-width: 760px) {
  .alarm-stats {
    grid-template-columns: 1fr 1fr;
  }
  .alarm-stats button {
    padding: 15px;
  }
}
</style>
