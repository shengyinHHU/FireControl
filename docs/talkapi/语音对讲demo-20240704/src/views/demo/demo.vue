<template>
  <div>
    初始化参数
    <div>
      appId
      <input type="text" v-model="paramsRaw.appId" />
    </div>
    <div>
      密钥
      <input type="text" v-model="paramsRaw.appSecret" />
    </div>
    <div>
      连接方式
      <select name="" id="" v-model="paramsRaw.domain">
        <option value="1">https形式</option>
        <option value="0">ip形式</option>
      </select>
    </div>
    <div>
      设备码
      <input type="text" v-model="deviceId" />
    </div>
    <div>
      连接方式：
      <select name="" id="" v-model="useRequest">
        <option :value="1">使用接口返回地址</option>
        <option :value="0">手动输入地址</option>
      </select>
    </div>
    <div v-if="!useRequest">语音请求地址：<input type="text" v-model="ip" /></div>
    <div v-if="!useRequest">语音请求sessionId：<input type="text" v-model="sessionId" /></div>
    <div v-if="!useRequest">语音请求token：<input type="text" v-model="token" /></div>
    <button @click="connectHandler" v-if="broadcastStatus === 1">开始对讲</button>
    <button @click="endBroadcast" v-else>结束</button>
  </div>
</template>

<script>
import Speech from '@/utils/speechs.js';
import initSDK from '../../utils/broadcastSdk';
import { getEncodedParam } from './encode-param';
export default {
  data() {
    return {
      useRequest: 1, // 是否直接使用请求
      ip: '', // wss地址
      sessionId: '', // 可以由接口返回
      token: '', // 可以由接口返回
      port: '', // 可以由接口返回
      websock: null,
      deviceId: '',
      broadcastStatus: 1,
      paramsRaw: {
        appId: '', // 必填appid
        appSecret: '', // 密钥必填
        version: 'v1.0',
        clientType: '3',
        domain: '1' // wss是否使用ip地址
      }
    };
  },
  mounted() {},
  methods: {
    testWss() {
      initSDK(this.ip, this.port, {
        protocol: 'wss'
      });
    },
    endBroadcast() {
      Speech.endBroadcast(true, (broadcastStatus) => {
        this.broadcastStatus = broadcastStatus;
      });
    },
    connectHandler() {
      const resp = getEncodedParam({ deviceId: this.deviceId }, this.paramsRaw);
      Speech.startBroadcast(
        {
          param: resp.request.body.urlencoded,
          wss: this.ip,
          port: this.port,
          token: this.token,
          sessionId: this.sessionId,
          useRequest: this.useRequest
        },
        (data) => {
          const { broadcastStatus, wss } = data;
          this.ip = wss;
          this.broadcastStatus = broadcastStatus;
        }
      );
    }
  }
};
</script>
