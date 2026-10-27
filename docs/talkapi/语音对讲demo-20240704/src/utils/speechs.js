import Vue from 'vue';
import initSDK from './broadcastSdk.js';

let sdkInstance = null;
let broadcastStatus = 1; // 1 开始播报，2 结束播报 3 播报已结束

const Speech = {
  /**
   *
   * @param {*} param 建立对讲相关参数
   * @param {*} startSuccessCalback
   */
  startBroadcast({ param, wss, port, useRequest, sessionId, token }, startSuccessCalback) {
    console.log({ param, wss, port }, 'ss');
    CheckMediaDevices.getMicrophones().then((deviceList) => {
      if (deviceList && deviceList.length > 0) {
        this.addRealBcApi({ param, wss, port, token, sessionId, useRequest }, startSuccessCalback);
      } else {
        alert('无法获取麦克风设备');
      }
    });
  },
  /**
   * @description 结束广播
   */
  endBroadcast(isTips, endCalback) {
    sdkInstance.closeRecord(
      () => {
        if (isTips) {
          broadcastStatus = 1;
          endCalback(1);
          alert('语音对讲结束');
        }
      },
      () => {
        isTips && alert('语音对讲结束失败');
      }
    );
  },
  async addRealBcApi(params, startSuccessCalback, startErrorCallback) {
    let _this = this;
    let res = {
      code: 0,
      data: params
    };
    if (params.useRequest) {
      res = await Vue.prototype.$_api.common.voiceTalkback(params.param);
    }
    if (res && res.code === 0) {
      console.log(res.data);
      let { sessionId, token, wss } = res.data;
      // if (useRequest == 1) {
      //   wss = params.ip;
      // }
      console.log(res.data);
      sdkInstance = initSDK(wss.replace('wss://', ''), params.port, {
        sessionId,
        token,
        protocol: 'wss'
      });
      if (sdkInstance) {
        try {
          sdkInstance.openRecord(
            () => {
              broadcastStatus = 2;
              startSuccessCalback({
                broadcastStatus,
                sessionId,
                wss
              });
            },
            () => {
              alert('语音对讲失败，请检查是否有麦克风或者是否授予麦克风权限');
              startErrorCallback('语音对讲失败');
              _this.endBroadcast(sessionId, false);
            }
          );
        } catch (e) {
          console.log('sdkInstance:', e);
          alert('语音对讲初始化失败');
          startErrorCallback('语音对讲失败');
          _this.endBroadcast(sessionId, false);
        }
      } else {
        console.log('sdkInstance-error:');
        alert('语音对讲初始化失败1');
        startErrorCallback('语音对讲失败');
        _this.endBroadcast(sessionId, false);
      }
    } else {
      res && res.msg && alert(res.msg);
      startErrorCallback('语音对讲失败');
    }
  }
};

const CheckMediaDevices = {
  /**
   * @description 获取媒体设备列表
   * @returns {Promise} 媒体设备列表
   */
  getMediaDevices() {
    const { navigator } = window;
    if (!navigator.mediaDevices || !navigator.mediaDevices.enumerateDevices()) {
      console.error('无法获取媒体设备列表');
      return;
    }
    let mediaDevicesPromise = navigator.mediaDevices
      .enumerateDevices()
      .then((devices) => {
        if (devices.length > 0) {
          let devicesObj = {};
          devices.map((device, index) => {
            devicesObj[index] = {
              设备类型: device.kind,
              设备标签: device.label,
              设备id: device.deviceId
            };
          });
          console.table(devicesObj);
        }
        return devices;
      })
      .catch((err) => {
        console.error(`获取设备异常：${err.name} = ${err.message}`);
      });
    return mediaDevicesPromise;
  },
  /**
   * @description 获取麦克风设备列表
   * @returns {*} 麦克风设备列表
   */
  getMicrophones() {
    let audioInputPromise = this.getMediaDevices().then((mediaDevicesArr) => {
      if (mediaDevicesArr.length > 0) {
        mediaDevicesArr = mediaDevicesArr.filter((el) => el.kind === 'audioinput');
        if (mediaDevicesArr.length > 0) {
          let devicesObj = {};
          mediaDevicesArr.map((device, index) => {
            devicesObj[index] = {
              设备类型: device.kind,
              设备标签: device.label,
              设备id: device.deviceId
            };
          });
          console.table(devicesObj);
          return mediaDevicesArr;
        } else {
          console.log('获取不到麦克风设备');
        }
      }
    });
    return audioInputPromise;
  }
};

export default Speech;
