import Vue from 'vue';
import VueRouter from 'vue-router';
import demo from '@/views/demo/demo.vue';

Vue.use(VueRouter);

let prefix = '/';

const constantRoutes = [
  {
    path: prefix + 'demo',
    name: `demo`,
    meta: {
      title: 'demo'
    },
    component: demo
  }
];

const router = new VueRouter({
  mode: 'hash',
  routes: constantRoutes
});

export default router;
