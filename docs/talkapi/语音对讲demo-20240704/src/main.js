import Vue from 'vue';
import App from './App.vue';
import router from './router';
import http from './service/request';
Vue.prototype.$_api = http;
new Vue({
  router,
  render: h => h(App)
}).$mount('#app')