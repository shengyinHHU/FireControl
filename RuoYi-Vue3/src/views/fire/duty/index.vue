<template>
  <ResourcePage resource="duty"
    ><template #actions
      ><el-button
        v-if="canFire('duty', 'share')"
        type="primary"
        @click="selectOpen = true"
        >生成场所上报码</el-button
      ></template
    ></ResourcePage
  ><el-dialog
    v-model="selectOpen"
    title="选择值班上报场所"
    class="fire-dialog"
    width="500px"
    append-to-body
    ><el-select
      popper-class="fire-dialog"
      v-model="placeId"
      placeholder="选择场所"
      filterable
      style="width: 100%"
      ><el-option
        v-for="p in places"
        :key="p.id"
        :label="p.name"
        :value="p.id" /></el-select
    ><template #footer
      ><el-button @click="selectOpen = false">取消</el-button
      ><el-button
        type="primary"
        :disabled="!placeId"
        @click="
          () => {
            shareOpen = true
            selectOpen = false
          }
        "
        >生成二维码</el-button
      ></template
    ></el-dialog
  ><ShareCode v-model="shareOpen" kind="duty" :resource-id="placeId" />
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { canFire } from '@/api/fire/access'
import { fire } from '@/api/fire'
import { attempt } from '../useFire'
import ResourcePage from '@/components/Fire/ResourcePage.vue'
import ShareCode from '@/components/Fire/ShareCode.vue'
const selectOpen = ref(false),
  shareOpen = ref(false),
  placeId = ref(''),
  places = ref([])
onMounted(async () => {
  const r = await attempt(() => fire.list('places', { pageSize: 10000 }))
  if (r) places.value = r.rows
})
</script>
