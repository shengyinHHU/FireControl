import axios from 'axios';
import service from './index';

let instance = axios.create({
  timeout: 20000,
  baseURL: '/'
});
const Http = {};
for (let module in service) {
  Http[module] = {};
  for (let key in service[module]) {
    let api = service[module][key];
    Http[module][key] = async function (
      params,
      isFormData = api.isFormData || false,
      config = {
        isLoading: api.isLoading !== false,
        isErrorToast: api.isErrorToast, // 是否显示异常toast，默认为显示
        message: api.message,
        extendsUrl: api.extendsUrl
      }
    ) {
      console.log(params, 'params', isFormData);
      let newParams = params;
      if (params && isFormData) {
        newParams = new FormData();
        for (let i in params) {
          newParams.append(i, params[i]);
        }
      }
      let response = {};
      const apiConfig = api;
      console.log(apiConfig, 'apiconfig');
      if (apiConfig.method === 'post' && apiConfig.isJson !== false) {
        try {
          response = await instance[apiConfig.method](apiConfig.url, newParams, config);
        } catch (err) {
          response = err;
        }
      }
      if (apiConfig.method === 'post' && apiConfig.isJson === false) {
        try {
          response = await instance[apiConfig.method](apiConfig.url, newParams, config);
        } catch (err) {
          response = err;
        }
      }
      if (apiConfig.method === 'get') {
        config.params = newParams;
        try {
          response = await instance[apiConfig.method](apiConfig.url, config);
        } catch (err) {
          response = err;
        }
      }
      return response;
    };
  }
}

instance.interceptors.request.use((config) => {
  if (config.isLoading) {
    // Vue.prototype.$Toast.loading({ content: config.message || '请求中', duration: null })
    // setTimeout(() => {
    // }, 500);
  }
  return config;
});

instance.interceptors.response.use((res) => {
  return res.data;
});
export default Http;
