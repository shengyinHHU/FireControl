<template>
  <div class="fire-page fire-theme">
    <div class="fire-page-title"><div><div class="fire-kicker">DA CHANG / FIRE SAFETY</div><h1>重点场所</h1><p>维护场所档案、联系人与地图位置</p></div><div class="fire-toolbar"><router-link v-if="canFire('dashboard')" to="/fire/dashboard" class="muted">指挥大屏 ↗</router-link><el-button v-if="canFire('places', 'import')" @click="importer.open()">导入场所</el-button><el-button v-if="canFire('places', 'add')" type="primary" @click="edit()">新增重点场所</el-button><el-button :loading="loading" @click="load">刷新记录</el-button></div></div>
    <p class="place-location-hint">有坐标的场所在对应地图图层显示；无坐标场所保留在本列表，可通过编辑补充定位。</p>
    <div class="fire-table-card" :aria-busy="loading">
      <el-form inline novalidate @submit.prevent="search"><el-form-item label="场所名称"><el-input v-model="keyword" aria-label="场所名称查询" placeholder="输入场所名称" clearable @clear="search" /></el-form-item><el-form-item label="场所类别"><el-select v-model="placeType" aria-label="场所类别查询" popper-class="fire-dialog" clearable placeholder="全部类别" style="width:180px" @change="search"><el-option v-for="option in placeTypes" :key="option.type" :value="option.type" :label="option.label" /></el-select></el-form-item><el-form-item><el-button type="primary" native-type="submit" :loading="loading">查询</el-button><el-button @click="reset">重置</el-button></el-form-item></el-form>
      <div v-if="error" class="fire-error" role="alert">{{ error }}<el-button link @click="load">重新加载</el-button></div>
      <el-table v-loading="loading" :data="rows" row-key="id" stripe empty-text="暂无符合条件的场所，可新增场所或调整查询条件" @row-dblclick="details">
        <el-table-column prop="name" label="场所名称" min-width="200" :fixed="desktop ? 'left' : false" show-overflow-tooltip /><el-table-column prop="category" label="场所类别" min-width="110" /><el-table-column prop="address" label="详细地址" min-width="190" :formatter="emptyValue" show-overflow-tooltip /><el-table-column prop="owner" label="负责人" min-width="100" :formatter="emptyValue" show-overflow-tooltip /><el-table-column prop="phone" label="联系电话" min-width="150" :formatter="emptyValue" /><el-table-column prop="mapLocation" label="地图位置" min-width="190" />
        <el-table-column label="操作" width="170" fixed="right"><template #default="{row}"><el-button link type="primary" @click="details(row)">详情</el-button><el-button v-if="canFire('places', 'edit')" link type="primary" :disabled="busy" @click="edit(row)">编辑</el-button><el-button v-if="canFire('places', 'remove')" link type="danger" :disabled="busy" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="navigate" @size-change="search" />
    </div>
    <PlaceEditor v-model="editorOpen" :place-id="editingId" @saved="load" />
    <PlaceDetail v-model="detailOpen" :place-id="selectedId" />
    <PlaceImport ref="importer" @success="load" />
  </div>
</template>
<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import '@/assets/styles/fire.scss'
import { listDatabasePlaces, removeDatabasePlace } from '@/api/fire/places'
import { PLACE_LAYERS } from '@/api/fire/placeLayers'
import { canFire } from '@/api/fire/access'
import PlaceEditor from './PlaceEditor.vue'
import PlaceDetail from './PlaceDetail.vue'
import PlaceImport from '@/components/ExcelImportDialog/PlaceImport.vue'
const route=useRoute(),router=useRouter(),placeTypes=[...PLACE_LAYERS,{type:'government',label:'机关单位'}]
const desktop=useMediaQuery('(min-width: 1024px)')
const emptyValue=(row,column,value)=>value==null||value===''?'—':value
const numberPage=value=>Math.max(1,Number.isSafeInteger(Number(value))?Number(value):1)
const size=value=>[10,20,50].includes(Number(value))?Number(value):10
const keyword=ref(String(route.query.keyword||'')),placeType=ref(String(route.query.placeType||'')),page=ref(numberPage(route.query.page)),pageSize=ref(size(route.query.pageSize)),rows=ref([]),total=ref(0),loading=ref(false),error=ref(''),busy=ref(false),editorOpen=ref(false),editingId=ref(),detailOpen=ref(false),selectedId=ref(''),importer=ref()
let revision=0
function persist(){router.replace({query:{...route.query,keyword:keyword.value||undefined,placeType:placeType.value||undefined,page:String(page.value),pageSize:String(pageSize.value)}})}
async function load(){const current=++revision;loading.value=true;error.value='';try{const response=await listDatabasePlaces({keyword:keyword.value,placeType:placeType.value,pageNum:page.value,pageSize:pageSize.value});if(current!==revision)return;if(page.value>1&&(page.value-1)*pageSize.value>=response.total){page.value=Math.max(1,Math.ceil(response.total/pageSize.value));persist();return load()}rows.value=response.rows;total.value=response.total}catch(e){if(current===revision){rows.value=[];total.value=0;error.value=e.message||'场所加载失败，请重试'}}finally{if(current===revision)loading.value=false}}
function search(){page.value=1;persist();load()}
function navigate(){persist();load()}
function reset(){keyword.value='';placeType.value='';search()}
function edit(row){editingId.value=row?.id;editorOpen.value=true}
function details(row){selectedId.value=row.id;detailOpen.value=true}
async function remove(row){if(busy.value)return;busy.value=true;try{await ElMessageBox.confirm(`确认删除“${row.name}”？删除后将从场所列表和地图中移除，原始导入资料及历史事件、装置关联保留。`,'删除重点场所',{type:'warning',confirmButtonText:'删除场所',cancelButtonText:'取消'});await removeDatabasePlace(row);ElMessage.success('场所已删除');await load()}catch(e){if(e!=='cancel'&&e!=='close')error.value=e.message||'删除失败，请重试'}finally{busy.value=false}}
watch(()=>route.query,q=>{const k=String(q.keyword||''),t=String(q.placeType||''),p=numberPage(q.page),s=size(q.pageSize);if(k!==keyword.value||t!==placeType.value||p!==page.value||s!==pageSize.value){keyword.value=k;placeType.value=t;page.value=p;pageSize.value=s;load()}})
onMounted(load);onBeforeUnmount(()=>++revision)
</script>
<style scoped>.place-location-hint{color:var(--fire-muted);line-height:1.7;margin:0 0 16px}</style>
