<template>
  <ResourcePage ref="page" resource="places">
    <template #actions><el-button v-if="canFire('places', 'import')" type="primary" @click="importer.open()">导入场所</el-button></template>
    <template #before><p class="place-location-hint">有坐标的场所可在地图对应图层查看；无坐标场所保留在本列表，待补充定位。</p></template>
  </ResourcePage>
  <PlaceImport ref="importer" @success="page.reload()" />
</template>
<script setup>
import { ref } from 'vue'
import ResourcePage from '@/components/Fire/ResourcePage.vue'
import PlaceImport from '@/components/ExcelImportDialog/PlaceImport.vue'
import { canFire } from '@/api/fire/access'
const importer = ref(null), page = ref(null)
</script>
<style scoped>.place-location-hint { color: var(--fire-muted, #9db3c9); line-height: 1.7; margin: 0 0 16px; }</style>
