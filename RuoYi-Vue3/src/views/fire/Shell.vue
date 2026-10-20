<template>
  <div class="fire-app" :class="{ 'fire-app-screen': screen }">
    <aside v-if="!screen" class="fire-sidebar">
      <router-link to="/fire/dashboard" class="fire-brand"
        ><span class="brand-mark">消</span>
        <div>大厂智慧消防<small>街道消防监管平台</small></div></router-link
      >
      <div class="nav-caption">消防监管工作台</div>
      <nav>
        <router-link
          v-for="item in menus"
          :key="item[0]"
          :to="'/fire/' + item[0]"
          ><el-icon><component :is="item[2]" /></el-icon
          >{{ item[1] }}</router-link
        >
      </nav>
      <div class="sidebar-foot">
        <span class="online-dot"></span
        >{{ databasePage ? '真实数据库记录' : isFireMock ? '演示环境 · 演示数据' : '消防监管业务'
        }}<small>DA CHANG · FIRE SAFETY</small>
      </div>
    </aside>
    <main class="fire-main">
      <header v-if="!screen" class="fire-topbar">
        <div>
          <span class="online-dot"></span> 大厂街道 /
          <strong>{{ route.meta.title }}</strong>
        </div>
        <div class="top-actions">
          <span>{{ date }}</span
          ><router-link to="/fire/dashboard">进入指挥大屏 ↗</router-link
          ><el-dropdown popper-class="fire-dialog"
            ><el-button text
              >系统管理 <el-icon><ArrowDown /></el-icon></el-button
            ><template #dropdown
              ><el-dropdown-menu
                ><el-dropdown-item
                  v-for="s in systemLinks"
                  :key="s[0]"
                  @click="router.push(s[0])"
                  >{{ s[1] }}</el-dropdown-item
                ></el-dropdown-menu
              ></template
            ></el-dropdown
          >
        </div>
      </header>
      <div v-if="isFireMock && !screen" class="fire-demo-bar">
        <span v-if="databasePage">当前页面使用真实数据库记录；设备通信状态以后台接收结果为准</span>
        <span v-else>当前业务使用演示数据，操作保存在当前浏览器</span>
        <el-button v-if="!databasePage" link @click="reset">恢复示例数据</el-button>
      </div>
      <router-view :key="route.path" />
    </main>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { navigation } from './catalog'
import { isFireMock } from '@/api/fire/config'
import { canFire } from '@/api/fire/access'
import { fire } from '@/api/fire'
import { attempt } from './useFire'
import '@/assets/styles/fire.scss'
const route = useRoute(),
  router = useRouter()
const databasePage = computed(() => ['/fire/places', '/fire/hosts', '/fire/alarms'].includes(route.path))
const screen = computed(() => route.path === '/fire/dashboard')
const menus = computed(() =>
  navigation.filter((n) => canFire(n[0] === 'video' ? 'cameras' : n[0])),
)
const date = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'full',
  timeZone: 'Asia/Shanghai',
}).format(new Date())
const systemLinks = [
  ['/system/user', '用户管理'],
  ['/system/role', '角色权限'],
  ['/system/dept', '组织架构'],
  ['/user/profile', '个人中心'],
]
async function reset() {
  await attempt(async () => {
    await ElMessageBox.confirm(
      '将清空本浏览器的演示修改并恢复示例，是否继续？',
      '恢复示例数据',
    )
    await fire.action('demo', '', 'reset')
    location.reload()
  })
}
</script>
