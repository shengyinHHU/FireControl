import * as LiveKit from "./livekit-client.esm.mjs";
import WebSocketClient from "./websocket";
import EventEmitter from "./emitter";

class LiveClient extends EventEmitter {
  // liveKit服务器地址
  #roomServerUrl = "";
  // liveKit加入服务器token
  #roomToken = "";
  // 呼入绑定后接口返回
  #ruleId = null;
  // 构造器
  constructor({
    userId,
    org,
    roomName = "",
    roomMode = "auto",
    baseUrl,
  } = {}) {
    super();
    // 与会者Id
    this.userId = userId;
    // 租户id
    this.org = org;
    // 房间名
    this.roomName = roomName;
    // 房间号
    this.roomNum;
    // 房间
    this.room = null;
    // 房间通话模式  auto-音视频通话 audio-仅语音
    this.roomMode = roomMode;
    // 接口baseUrl
    this.baseUrl = baseUrl
    // 初始化编解码
    this.encoder = new TextEncoder();
    this.decoder = new TextDecoder();
    // ws地址
    let wshead = this.baseUrl.split('//')[0] == 'http:' ? 'ws://' : 'wss://'
    this.websocketUrl = `${wshead}${this.baseUrl.split('//')[1]}/socket?org=${this.org}&identity=${this.userId}`
    // socket初始化
    this.socket = new WebSocketClient(this.websocketUrl);
    // 发送socket链接成功事件
    this.socket.addOpenHandler('SocketOpen', () => {
      this.emit('socketOpen')
    })
    this.socket.connect();
    // 初始化socket事件监听
    this.#initSocketEvent();
  }
  get isShareEnable() {
    if (this.room && this.room.localParticipant) {
      return this.room.localParticipant.isScreenShareEnabled;
    } else {
      return false;
    }
  }
  get isCameraEnable() {
    if (this.room && this.room.localParticipant) {
      return this.room.localParticipant.isCameraEnabled;
    } else {
      return false;
    }
  }
  get isMicrophoneEnable() {
    if (this.room && this.room.localParticipant) {
      return this.room.localParticipant.isMicrophoneEnabled;
    } else {
      return false;
    }
  }
  // 获取设备列表
  static async getDeviceList() {
    const audioDevices = await LiveKit.Room.getLocalDevices("audioinput");
    const videoDevices = await LiveKit.Room.getLocalDevices("videoinput");
    const outputDevices = await LiveKit.Room.getLocalDevices("audiooutput");
    return {
      audioDevices,
      videoDevices,
      outputDevices,
    };
  }
  #initSocketEvent() {
    // 房主请求用户打开摄像头/麦克风
    this.socket.addMessageHandler("ResUnMute", (message) => {
      const msg = JSON.parse(message);
      if (msg.cmd === "ResUnMute") {
        if(msg?.data.code == '200') {
          this.emit("requestDeviceOpen", msg.data);
        } else {
          this.emit("errorPush", {
            type: 'requestDeviceOpen',
            message: msg?.data.msg ?? '更改用户设备失败'
          })
        }
      }
    });
    // 接收到其他人的邀请信息
    this.socket.addMessageHandler('ResThirdInvite', (message) => {
      const msg = JSON.parse(message);
      if(msg.cmd === "ResInvite" && msg.data?.code == '200') {
        this.emit('receiveInviteSuccess', {
          roomNum: msg.data.room,
          fromIdentity: msg.data.fromIdentity,
          fromOrg: msg.data.fromOrg,
          metadata: msg.data.metadata
        })
      }
    })
    // 发出的邀请被接受
    this.socket.addMessageHandler('ResAccept', (message) => {
      const msg = JSON.parse(message);
      if(msg.cmd === "ResAccept" && msg.data?.code == '200') {
        this.emit('receiveInviteAccept', {
          roomNum: msg.data.room,
          fromIdentity: msg.data.fromIdentity,
          fromOrg: msg.data.fromOrg
        })
      }
    })
    // 发出的邀请被拒绝
    this.socket.addMessageHandler('ResRefuse', (message) => {
      const msg = JSON.parse(message);
      if(msg.cmd === "ResRefuse" && msg.data?.code == '200') {
        this.emit('receiveInviteRefuse', {
          roomNum: msg.data.room,
          fromIdentity: msg.data.fromIdentity,
          fromOrg: msg.data.fromOrg
        })
      }
    })
    // 发送消息
    this.socket.addMessageHandler("SendText", (message) => {
      const msg = JSON.parse(message)
      if(msg.cmd === 'SendText') {
        if(msg.data?.code == "200") {
          this.emit('sendTextSuccess', msg.data.msg)
        } else {
          this.emit('sendTextFailed', msg.data.msg)
        }
      }
    })
    // 接收即时消息
    this.socket.addMessageHandler("ResReceiveMsg", (message) => {
      const msg = JSON.parse(message)
      if(msg.cmd === 'ResReceiveMsg') {
        if(msg.data?.code == '200') {
          this.emit('messageReceived', {
            origin: msg?.data.fromIdentity,
            content: msg?.data.text,
						fromName: msg?.data.fromName,
						fromOrg: msg?.data.fromOrg,
						room: msg?.data.room
          })
        }
      }
    })
    // 接收到主持人添加成功
    this.socket.addMessageHandler("ResAddHost", (message) => {
      const msg = JSON.parse(message)
      if(msg.cmd === "ResAddHost") {
        if(msg.data?.code == "200") {
          this.emit('setHostSuccess', {
            hostId: msg?.data.hostId,
            hostName: msg?.data.hostName
          })
        }
      }
    })
    // 与会者被设为联席主持人(20241128新增)
    this.socket.addMessageHandler("ResAddCoHost", (message) => {
      const msg = JSON.parse(message)
      if(msg.cmd === "ResAddCoHost") {
        if(msg.data?.code == "200") {
          this.emit('addCoHostSuccess', {
            coHostId: msg?.data.coHostId,
            coHostName: msg?.data.coHostName
          })
        }
      }
    })
    // 与会者被移除联席主持人身份(20241128新增)
    this.socket.addMessageHandler("ResRemoveCoHost", (message) => {
      const msg = JSON.parse(message)
      if(msg.cmd === "ResRemoveCoHost") {
        if(msg.data?.code == "200") {
          this.emit('removeCoHostSuccess', {
            coHostId: msg?.data.coHostId,
            coHostName: msg?.data.coHostName
          })
        }
      }
    })
    // 与会者权限变更消息(20241128新增)
    this.socket.addMessageHandler("ResModifyPermission", (message) => {
      const msg = JSON.parse(message)
      if(msg.cmd === "ResModifyPermission") {
        if(msg.data?.code == "200") {
          this.emit('roomPermissionChange', {
            permission: msg.data?.permission,
            input: msg.data?.input
          })
        }
      }
    })

  }
  // 统计sdk调用
  #getSdkCallNum(sdkType) {
    if (!this.roomNum) {
      this.emit("errorPush", {
        type: 'getSdkCallNum',
        message: '请先加入房间'
      })
      return;
    }
    const message = {
      cmd: "SdkCall",
      data: {
        room: this.roomNum,
        sdkType: sdkType    //audio,video,share,board
      }
    }
    this.socket.send(message)
  }
  // 根据id获取dom元素
  #getDom(id) {
    return document.getElementById(id);
  }
  // 解除呼入绑定
  async #dispatchInCall() {
    if (!this.#ruleId) {
      return;
    }
    if (!this.baseUrl) {
      return;
    }
    let result;
    try {
      let response = await fetch(this.baseUrl + '/api/v1/deleteDispatcher', {
        method: "POST",
        body: JSON.stringify({
          ruleId: this.#ruleId,
        }),
      });
      result = await response.json();
      if(result.code !== 200) {
        this.emit("errorPush", {
          type: 'dispatchInCall',
          message: '解除呼入绑定失败'
        })
      }
    } catch (e) {
      this.emit("errorPush", {
        type: 'dispatchInCall',
        message: '解除呼入绑定失败'
      })
    }
  }
  // 获取房间token
  #getToken(roomNum, origin = "0") {
    if (!roomNum) {
      this.emit("errorPush", {
        type: 'getToken',
        message: '未获取到房间号'
      })
      return;
    }
    return new Promise((resolve, reject) => {
      const getTokenMessage = {
        cmd: "GetToken",
        data: {
          org: this.org,
          room: roomNum,
          identity: this.userId,
          origin: origin
        },
      };
      this.socket.send(getTokenMessage);
      this.socket.addMessageHandler("GetToken", (message) => {
        let msg = JSON.parse(message);
        if (msg.cmd === "GetToken") {
          if(msg?.data.code == "200") {
            resolve({
              roomUrl: msg.data.url,
              token: msg.data.token,
            });
          } else {
            this.emit("errorPush", {
              type: 'getToken',
              message: msg.data?.msg ?? '获取会议token失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      });
    });
  }
  // 即时消息发送
  sendMessage(msg, name) {
    const msgSend = {
      cmd: 'SendText',
      data: {
        room: this.roomNum,
				identity: this.userId,
				name,
        text: msg
      }
    }
    this.socket.send(msgSend)
  }
  // 截屏
  screenShot(id, config = {}) {
    let localParticipantDom = this.#getDom(id);
    if (!localParticipantDom) {
      this.emit("errorPush", {
        type: 'screenShot',
        message: 'DOM元素不存在'
      })
      return;
    }
    let canvas = document.createElement("canvas");
    let ctx = null,
      imgUrl = null;
    canvas.width = config.width;
    canvas.height = config.height;
    ctx = canvas.getContext("2d");
    ctx.drawImage(localParticipantDom, 0, 0, config.width, config.height);
    imgUrl = canvas.toDataURL("image/png", config.quality);
    return imgUrl;
  }
  // 截屏上传
  async uploadScreenShot(id, config = {}) {
    let imgParam,
      image = this.screenShot(id, config);
    let index = image.indexOf(",");
    if (index > -1) {
      imgParam = image.substring(index + 1);
    }
    let response = await fetch(this.baseUrl + '/api/FileFind/upload/image/sample', {
      method: "POST",
      headers: {
        "Content-Type": "application/json;charset=utf-8",
      },
      body: JSON.stringify({
        base64: imgParam,
        org: this.org,
        owner: this.userId,
      }),
    });
    try {
      let result = await response.json();
      if (result?.code === 200) {
        this.emit("imageUploadSuccess", image);
      } else {
        this.emit("imageUploadFail", result);
      }
    } catch (err) {
      this.emit("imageUploadFail", err);
    }
  }
  // 判断用户是否已经在会议中(20240625新增)
  judgeUserInMeeting(roomNum = '') {
    return new Promise((resolve, reject) => {
      fetch(this.baseUrl + '/api/v1/IsParticipantExist', {
        method: "POST",
        headers: {
          "Content-Type": "application/json;charset=utf-8",
        },
        body: JSON.stringify({
          room: roomNum,
          identity: this.userId
        })
      }).then(response => {
        return response.json()
      }).then(result => {
        if(result?.code === 200) {
          resolve(result?.data)
        } else {
          return reject(new Error("通信请求失败"))
        }
      }).catch(err => {
        reject(err)
      })
    })
  }
  // 判断当前会议是否存在
  judgeRoomExist(roomNum = '') {
    return new Promise((resolve, reject) => {
      fetch(this.baseUrl + '/api/v1/isRoomExist', {
        method: "POST",
        headers: {
          "Content-Type": "application/json;charset=utf-8",
        },
        body: JSON.stringify({
          room: roomNum,
        })
      }).then(response => {
        return response.json()
      }).then(result => {
        resolve(result)
      }).catch(err => {
        reject(err)
      })
    })
  }
  // 将指定与会者置聋(20240909新增)
  setParticipantDeafness(identity) {
    return new Promise((resolve, reject) => {
      if(!identity) {
        this.emit("errorPush", {
          type: 'setParticipantDeafness',
          message: 'identity不能为空'
        })
        return reject(new Error("通信请求失败"))
      }
      const setDeafnessMessage = {
        cmd: "DoDeafness",
        data: {
          room: this.roomNum,
          identity
        }
      }
      this.socket.send(setDeafnessMessage)
      this.socket.addMessageHandler("SetParticipantDeafness", (message) => {
        let msg = JSON.parse(message)
        if(msg.cmd === "DoDeafness") {
          if(msg?.data.code == "200") {
            resolve()
          } else {
            this.emit("errorPush", {
              type: 'setParticipantDeafness',
              message: msg.data.msg ?? '与会者置聋失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      })
    })
  }
  // 取消置聋与会者(20241104新增)
  cancelParticipantDeafness(identity) {
    return new Promise((resolve, reject) => {
      if(!identity) {
        this.emit("errorPush", {
          type: 'cancelParticipantDeafness',
          message: 'identity不能为空'
        })
        return reject(new Error("通信请求失败"))
      }
      const cancelDeafnessMessage = {
        cmd: "UnDoDeafness",
        data: {
          room: this.roomNum,
          identity
        }
      }
      this.socket.send(cancelDeafnessMessage)
      this.socket.addMessageHandler("CancelParticipantDeafness", (message) => {
        const msg = JSON.parse(message)
        if(msg.cmd === "UnDoDeafness") {
          if(msg?.data.code == "200") {
            resolve()
          } else {
            this.emit("errorPush", {
              type: 'cancelParticipantDeafness',
              message: msg.data.msg ?? '与会者取消置聋失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      })
    })
  }
  //  添加联席主持人(20241128新增)
  addCoHost(coHostId, coHostName) {
    return new Promise((resolve, reject) => {
      if(!coHostId || !coHostName) {
        this.emit("errorPush", {
          type: 'addCoHost',
          message: '关键参数缺失'
        })
        reject(new Error("关键参数缺失"))
      }
      const addCoHostMessage = {
        cmd: "AddCoHost",
        data: {
          room: this.roomNum,
          identity: this.userId,
          coHostId,
          coHostName
        }
      }
      this.socket.send(addCoHostMessage)
      this.socket.addMessageHandler("AddCoHost", (message) => {
        const msg = JSON.parse(message)
        if(msg.cmd === "AddCoHost") {
          if(msg?.data.code == "200") {
            resolve()
          } else {
            this.emit("errorPush", {
              type: 'addCoHost',
              message: msg.data.msg ?? '联席主持人设置失败'
            })
            reject(new Error(msg.data.msg ?? '联席主持人设置失败'))
          }
        }
      })
    })
  }
  // 移除联席主持人(20241128新增)
  removeCoHost(coHostId, coHostName) {
    return new Promise((resolve, reject) => {
      if(!coHostId || !coHostName) {
        this.emit("errorPush", {
          type: 'removeCoHost',
          message: '关键参数缺失'
        })
        reject(new Error("关键参数缺失"))
      }
      const removeCoHostMessage = {
        cmd: "RemoveCoHost",
        data: {
          room: this.roomNum,
          identity: this.userId,
          coHostId,
          coHostName
        }
      }
      this.socket.send(removeCoHostMessage)
      this.socket.addMessageHandler("RemoveCoHost", (message) => {
        const msg = JSON.parse(message)
        if(msg.cmd === "RemoveCoHost") {
          if(msg?.data.code == "200") {
            resolve()
          } else {
            this.emit("errorPush", {
              type: 'removeCoHost',
              message: msg.data.msg ?? '移除联席主持人失败'
            })
            reject(new Error(msg.data.msg ?? '联席主持人设置失败'))
          }
        }
      })
    })
  }
  // 修改普通参会者权限(20241128新增)
  // input: 1 video 2 screenShare 3 record 4 chat 5 updateName 6 invite
  // permission: 1 true 0 false
  modifyPermission(permission, input) {
    return new Promise((resolve, reject) => {
      if(!permission || !input) {
        this.emit("errorPush", {
          type: 'modifyPermission',
          message: '关键参数缺失'
        })
        reject(new Error("关键参数缺失"))
      }
      const modifyPermissionMessage = {
        cmd: "ModifyPermission",
        data: {
          room: this.roomNum,
          identity: this.userId,
          permission,
          input
        }
      }
      this.socket.send(modifyPermissionMessage)
      this.socket.addMessageHandler("ModifyPermission", (message) => {
        const msg = JSON.parse(message)
        if(msg.cmd === "ModifyPermission") {
          if(msg?.data.code == "200") {
            resolve()
          } else {
            this.emit("errorPush", {
              type: 'modifyPermission',
              message: msg.data.msg ?? '修改与会者权限失败'
            })
            reject(new Error(msg.data.msg ?? '修改与会者权限失败'))
          }
        }
      })
    })
  }
  // 创建房间
  async createRoom() {
    if (!this.baseUrl) {
      this.emit("errorPush", {
        type: 'createRoom',
        message: '请输入baseUrl'
      })
      return;
    }
    if (!this.roomName) {
      this.emit("errorPush", {
        type: 'createRoom',
        message: '房间名不能为空'
      })
      return;
    }
    if (!this.org || !this.userId) {
      this.emit("errorPush", {
        type: 'createRoom',
        message: '创建房间前请先登录'
      })
      return;
    }
    let result,
      result2,
      number = "";
    try {
      let response = await fetch(this.baseUrl + '/api/v1/creatRoom', {
        method: "POST",
        body: JSON.stringify({
          roomAlias: this.roomName,
          org: this.org,
          owner: this.userId,
        }),
      });
      result = await response.json();
      if (result.code == 200) {
        number = result.data;
        let response2 = await fetch(this.baseUrl + '/api/v1/createDispatcher', {
          method: "POST",
          body: JSON.stringify({
            org: this.org,
            room: number,
            called: number,
          }),
        });
        result2 = await response2.json();
        if (result2.code == 200) {
          this.emit("roomCreatedSuccess", number);
          this.#ruleId = result2.data;
        } else {
          this.emit("roomCreatedFail", result2.msg);
          return "";
        }
      } else {
        this.emit("roomCreatedFail", result.msg);
        return "";
      }
    } catch (err) {
      this.emit("roomCreatedFail", err);
    }
    return number;
  }
  async joinRoom({
    audioDeviceId = "",
    videoDeviceId = "",
    outputDeviceId = "",
		facingMode = "user",
    autoGainControl,
    echoCancellation,
    noiseSuppression,
    roomNum,
    cameraStatus = true,
    microphoneStatus = true,
    origin = "0",
    videoCodec
  } = {}) {
    if (!roomNum) {
      this.emit("errorPush", {
        type: 'joinRoom',
        message: '请输入房间号'
      })
      return;
    }
    let roomOption = {
      audioCaptureDefaults: {
        deviceId: audioDeviceId ? audioDeviceId : "default",
        autoGainControl: autoGainControl ?? false,
        echoCancellation: echoCancellation ?? false,
        noiseSuppression: noiseSuppression ?? false,
      },
      videoCaptureDefaults: {},
      stopLocalTrackOnUnpublish: true,
      adaptiveStream: true,
    };
		if(outputDeviceId) {
			roomOption.audioOutput = {
			  deviceId: outputDeviceId,
			}
		}
    let connectOption = {
      autoSubscribe: true,
    };
    if (videoDeviceId) {
      roomOption.videoCaptureDefaults.deviceId = videoDeviceId;
    } else {
			roomOption.videoCaptureDefaults.facingMode = facingMode
		}
    if (videoCodec && typeof videoCodec === "string") {
      const publishDefaults = {
        videoCodec: videoCodec
      }
      roomOption.publishDefaults = publishDefaults
    }
    this.roomNum = roomNum;
    try {
      let roomData = await this.#getToken(this.roomNum, origin);
      this.#roomServerUrl = roomData.roomUrl;
      this.#roomToken = roomData.token;
      this.room = new LiveKit.Room(roomOption);
      await this.room.prepareConnection(this.#roomServerUrl, this.#roomToken);
      this.#initEvents();
      await this.room.connect(this.#roomServerUrl, this.#roomToken, connectOption);
    } catch (err) {
      this.emit("joinRoomError", err);
      throw new Error(`会议连接失败：${err?.message || String(err || '未知连接错误')}`);
    }
    // 判断当前是语音通话模式还是视频通话模式
    try {
      if (this.roomMode === "auto") {
        // await this.room.localParticipant.enableCameraAndMicrophone();
        await this.room.localParticipant.setCameraEnabled(cameraStatus);
        await this.room.localParticipant.setMicrophoneEnabled(microphoneStatus);
        this.#getSdkCallNum('video')
      } else {
        await this.room.localParticipant.setMicrophoneEnabled(microphoneStatus);
        await this.room.localParticipant.setCameraEnabled(false);
        this.#getSdkCallNum('audio')
      }
    } catch(err) {
      throw new Error(`会议已连接，但媒体设备启用失败：${err?.name || ''} ${err?.message || String(err)}`)
    }
    const audioInputDevice = this.room.getActiveDevice("audioinput") ?? "default";
    const videoInputDevice = this.room.getActiveDevice("videoinput") ?? 'default';
    const audioOutputDevice = this.room.getActiveDevice("audiooutput") ?? "default";
    this.room.remoteParticipants.forEach((participant) => {
      this.#participantConnected(participant);
    });
    this.#participantConnected(this.room.localParticipant);
    return {
      audioInputDevice,
      videoInputDevice,
      audioOutputDevice,
    };
  }
  // 呼出
  makeCall(name, phone, type) {
    return new Promise((resolve, reject) => {
      let params = null
      if(!this.roomNum) {
        this.emit("errorPush", {
          type: 'makeCall',
          message: '请先加入房间'
        })
        return reject(new Error("通信请求失败"))
      }
      if(!this.baseUrl) {
        this.emit("errorPush", {
          type: 'makeCall',
          message: 'baseUrl为空'
        })
        return reject(new Error("通信请求失败"))
      }
      if(type) {
        params = {
          name: name,
          org: this.org,
          room: this.roomNum,
          dnis: phone,
          type
        }
      } else {
        params = {
          name: name,
          org: this.org,
          room: this.roomNum,
          dnis: phone,
        }
      }
      fetch(this.baseUrl + '/api/v1/makeCall', {
        method: "POST",
        body: JSON.stringify(params),
      }).then(response => {
        return response.json()
      }).then(result => {
        if(result.code == 200) {
          this.emit('makeCallSuccess')
          resolve()
        } else {
          this.emit("errorPush", {
            type: 'makeCall',
            message: result.msg ?? '呼出失败'
          })
          return reject(new Error("通信请求失败"))
        }
      }).catch(err => {
        this.emit("errorPush", {
          type: 'makeCall',
          message: err
        })
        return reject(new Error("通信请求失败"))
      })
    })
  }
  // 视频推流
  pushVideoStream(outputUrl) {
    if (!this.roomNum) {
      this.emit("errorPush", {
        type: 'pushVideoStream',
        message: result.msg ?? '请先加入房间'
      })
      return;
    }
    return new Promise((resolve, reject) => {
      let pushVideoMessage = {
        cmd: "StartOutputStream",
        data: {
          room: this.roomNum,
          url: outputUrl,
        },
      };
      this.socket.send(pushVideoMessage);
      this.socket.addMessageHandler("StartOutputStream", (message) => {
        let msg = JSON.parse(message);
        if (msg.cmd === "StartOutputStream") {
          if(msg?.data.code == "200") {
            this.emit("startPushing", msg.data);
            resolve(msg.data.result);
          } else {
            this.emit("errorPush", {
              type: 'pushVideoStream',
              message: msg.data?.msg ?? '推流失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      });
    });
  }
  // 停止推流
  stopPushVideoStream() {
    if (!this.roomNum) {
      this.emit("errorPush", {
        type: 'stopPushVideoStream',
        message: '请先加入房间'
      })
      return;
    }
    return new Promise((resolve, reject) => {
      let stopVideoMessage = {
        cmd: "StopOutputStream",
        data: {
          room: this.roomNum,
        },
      };
      this.socket.send(stopVideoMessage);
      // this.socket.clearMessageHandler()
      this.socket.addMessageHandler("StopOutputStream", (message) => {
        let msg = JSON.parse(message);
        if (msg.cmd === "StopOutputStream") {
          if(msg?.data.code == "200") {
            this.emit("stopPushing", msg.data);
            resolve();
          } else {
            this.emit("errorPush", {
              type: 'stopPushVideoStream',
              message: msg.data?.msg ?? '停止推流失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      });
    });
  }
  // 将指定与会者移出房间
  moveoutParticipant(identity) {
    if(!this.roomNum) {
      this.emit("errorPush", {
        type: 'moveoutParticipant',
        message: '请先加入房间'
      })
      return;
    }
    return new Promise((resolve, reject) => {
      let moveoutMessage = {
        cmd: "RemoveParticipant",
        data: {
          room: this.roomNum,
          identity: identity
        }
      }
      this.socket.send(moveoutMessage)
      this.socket.addMessageHandler('RemoveParticipant', (message) => {
        let msg = JSON.parse(message)
        if(msg.cmd === 'RemoveParticipant') {
          if(msg?.data.code == '200') {
            resolve()
          } else {
            this.emit("errorPush", {
              type: 'moveoutParticipant',
              message: msg.data.msg ?? '移出房间失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      })
    })
  }
  // 修改与会者名称
  updateParticipantName(identity, name, isPermission = '0') {
    if(!this.roomNum) {
      this.emit("errorPush", {
        type: 'updateParticipantName',
        message: '请先加入房间'
      })
      return;
    }
    return new Promise((resolve, reject) => {
      let updateMessage = {
        cmd: "UpdateParticipant",
        data: {
          room: this.roomNum,
          identity: identity,
          name: name,
          isPermission
        }
      }
      this.socket.send(updateMessage)
      this.socket.addMessageHandler('UpdateParticipant', (message) => {
        let msg = JSON.parse(message)
        if(msg.cmd === 'UpdateParticipant') {
          if(msg?.data.code == '200') {
            resolve()
          } else {
            this.emit("errorPush", {
              type: 'updateParticipantName',
              message: msg.data.msg ?? '修改与会者名称失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      })
    });
  }
  // 设为主持人
  setParticipantToHost(hostId, hostName) {
    return new Promise((resolve, reject) => {
      if(!this.roomNum || !hostId || !hostName || !this.userId) {
        this.emit("errorPush", {
          type: 'setParticipantToHost',
          message: '关键参数缺失'
        })
        reject('关键参数缺失')
      }
      const setHostmessage = {
        cmd: "AddHost",
        data: {
          room: this.roomNum,
          identity: this.userId,
          hostId,
          hostName
        }
      }
      this.socket.addMessageHandler('SetParticipantToHost', (message) => {
        const msg = JSON.parse(message)
        if(msg.cmd === 'AddHost') {
          if(msg?.data.code == '200') {
            resolve()
          } else {
            this.emit("errorPush", {
              type: 'setParticipantToHost',
              message: msg.data.msg ?? '添加为主持人失败'
            })
            reject(msg.data.msg ?? '添加为主持人失败')
          }
        }
      })
      this.socket.send(setHostmessage)
    });
  }
  // 开始录音/录像
  startRecord() {
    if (!this.roomNum) {
      this.emit("errorPush", {
        type: 'startRecord',
        message: '请先加入房间'
      })
      return;
    }
    return new Promise((resolve, reject) => {
      let startRecordMessage = {
        cmd: "StartRecording",
        data: {
          room: this.roomNum,
        },
      };
      this.socket.send(startRecordMessage);
      // this.socket.clearMessageHandler()
      this.socket.addMessageHandler("StartRecording", (message) => {
        let msg = JSON.parse(message);
        if (msg.cmd === "StartRecording") {
          if(msg?.data.code == "200") {
            this.emit("startRecording", msg.data.result);
            resolve(msg.data.result);
          } else {
            this.emit("errorPush", {
              type: 'startRecord',
              message: msg.data.msg ?? '录制失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      });
    });
  }
  // 停止录音/录像
  stopRecord() {
    if (!this.roomNum) {
      this.emit("errorPush", {
        type: 'stopRecord',
        message: '请先加入房间'
      })
      return;
    }
    return new Promise((resolve, reject) => {
      let stopRecordMessage = {
        cmd: "StopRecording",
        data: {
          room: this.roomNum,
        },
      };
      this.socket.send(stopRecordMessage);
      // this.socket.clearMessageHandler()
      this.socket.addMessageHandler("StopRecording", (message) => {
        let msg = JSON.parse(message);
        if (msg.cmd === "StopRecording") {
          if(msg?.data.code == "200") {
            this.emit("stopRecording", msg.data);
            resolve();
          } else {
            this.emit("errorPush", {
              type: 'stopRecord',
              message: msg.data?.msg ?? '停止录制失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      });
    });
  }
  // 拉流(20240912更新)
  pullStream(mediaUrl, streamIdentity) {
    return new Promise((resolve, reject) => {
      if(!streamIdentity) {
        reject("streamIdentity不能为空")
      }
      let pullStreamMessage = {
        cmd: "StartInputStream",
        data: {
          room: this.roomNum,
          identity: streamIdentity,
          url: mediaUrl,
        },
      };
      this.socket.send(pullStreamMessage);
      // this.socket.clearMessageHandler()
      this.socket.addMessageHandler("StartInputStream", (message) => {
        let msg = JSON.parse(message);
        if (msg.cmd === "StartInputStream") {
          if(msg?.data.code == "200") {
            this.emit("startPulling", msg.data.result);
            resolve(msg.data.result);
          } else {
            this.emit("errorPush", {
              type: 'pullStream',
              message: msg.data?.msg ?? '拉流失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      });
    });
  }
  // 停止拉流
  stopPullStream(streamIdentity) {
    return new Promise((resolve, reject) => {
      let stopPullMessage = {
        cmd: "StopInputStream",
        data: {
          room: this.roomNum,
          identity: streamIdentity,
        },
      };
      this.socket.send(stopPullMessage);
      this.socket.addMessageHandler("StopInputStream", (message) => {
        let msg = JSON.parse(message);
        if (msg.cmd === "StopInputStream") {
          if(msg?.data.code == "200") {
            this.emit("stopPulling", msg.data);
            resolve();
          } else {
            this.emit("errorPush", {
              type: 'stopPullStream',
              message: msg.data?.msg ?? '停止拉流失败'
            })
            return reject(new Error("通信请求失败"))
          }
        }
      });
    });
  }
  // 互联网邀请 identity-被邀请人姓名或联系方式 metadata-用户自定义数据
  inviteThirdParty(identity, org = "1478649882954985474", metadata = "") {
    return new Promise((resolve, reject) => {
      if(!identity) {
        this.emit("errorPush", {
          type: 'inviteThirdParty',
          message: '被邀请人姓名不能为空'
        })
        return reject(new Error("通信请求失败"))
      }
      let msgSend = {
        cmd: "Invite",
        data: {
          room: this.roomNum,
          identity: this.userId,
          inviteOrg: org,
          inviteIdentity: identity,
          metadata
        }
      }
      this.socket.addMessageHandler('inviteThirdParty', (message) => {
        let msg = JSON.parse(message)
        if(msg.cmd === 'Invite') {
          if(msg.data?.code === '200') {
            this.emit('inviteThirdSuccess')
            resolve()
          } else {
            this.emit('inviteThirdFail')
            return reject(new Error("通信请求失败"))
          }
        }
      })
      this.socket.send(msgSend)
    })
  }
  // 接受第三方邀请
  acceptInvite(roomNum, fromIdentity, fromOrg) {
    return new Promise((resolve, reject) => {
      if(!roomNum || !fromIdentity || !fromOrg) {
        this.emit("errorPush", {
          type: 'acceptInvite',
          message: '关键参数缺失'
        })
        return reject(new Error("通信请求失败"))
      }
      let msgSend = {
        cmd: "Accept",
        data: {
          room: roomNum,
          identity: this.userId,
          toOrg: fromOrg,
          toIdentity: fromIdentity
        }
      }
      this.socket.addMessageHandler('inviteAccept', (message) => {
        let msg = JSON.parse(message)
        if(msg.cmd === 'Accept') {
          if(msg.data?.code === '200') {
            this.emit('acceptInviteSuccess')
            resolve()
          } else {
            this.emit('acceptInviteFail')
            return reject(new Error("通信请求失败"))
          }
        }
      })
      this.socket.send(msgSend)
    })
  }
  // 拒绝第三方邀请
  refuseInvite(roomNum, fromIdentity, fromOrg) {
    return new Promise((resolve, reject) => {
      if(!roomNum || !fromIdentity || !fromOrg) {
        this.emit("errorPush", {
          type: 'refuseInvite',
          message: '关键参数缺失'
        })
        return reject(new Error("通信请求失败"))
      }
      let msgSend = {
        cmd: "Refuse",
        data: {
          room: roomNum,
          identity: this.userId,
          toOrg: fromOrg,
          toIdentity: fromIdentity
        }
      }
      this.socket.addMessageHandler('inviteRefuse', (message) => {
        let msg = JSON.parse(message)
        if(msg.cmd === 'Refuse') {
          if(msg.data?.code === '200') {
            this.emit('refuseInviteSuccess')
            resolve()
          } else {
            this.emit('refuseInviteFail')
            return reject(new Error("通信请求失败"))
          }
        }
      })
      this.socket.send(msgSend)
    })
  }
  // 发送消息
  enterText(value) {
    if (this.room && this.room.localParticipant) {
      const localParticipant = this.room.localParticipant;
      const msg = this.encoder.encode(value);
      localParticipant.publishData(msg, LiveKit.DataPacket_Kind.RELIABLE);
    } else {
      this.emit("errorPush", {
        type: 'enterText',
        message: '请先加入房间'
      })
    }
  }
  // 初始化房间事件
  #initEvents() {
    this.room.on(LiveKit.RoomEvent.Connected, () => {
      this.emit("roomConnected");
    });
    this.room.on(LiveKit.RoomEvent.Disconnected, async (reason) => {
      await this.#handleRoomDisconnect();
      this.emit("roomDisconnected", reason);
    });
    this.room.on(LiveKit.RoomEvent.ParticipantConnected, (participant) => {
      // console.log("参与者链接");
      this.emit("participantConnected", participant)
      this.#participantConnected(participant);
    });
    this.room.on(LiveKit.RoomEvent.ParticipantDisconnected, (participant) => {
      // console.log("远程人员离开");
      this.#participantDisconnected(participant);
    });
    // room.switchActiveDevice切换当前设备后调用
    this.room.on(LiveKit.RoomEvent.ActiveDeviceChanged, (kind, deviceId) => {
      this.emit("mediaDeviceChange", {
        kind: kind,
        deviceId: deviceId
      })
    });
    this.room.on(LiveKit.RoomEvent.ActiveSpeakersChanged, (participants) => {
      let speakers = []
      speakers = participants.map(item => {
        return {
          identity: item.identity,
          isLocal: item.isLocal,
          audioLevel: item.audioLevel,
          name: item.name ?? item.identity
        }
      })
      this.emit('activeSpeakerChange', speakers)
    })
    this.room.on(LiveKit.RoomEvent.LocalTrackPublished, (pub) => {
      // console.log("本地轨道发布");
      this.renderParticipant(this.room.localParticipant);
      this.startTrackAttach(this.room.localParticipant)
      // this.renderScreenShare();
    });
    this.room.on(LiveKit.RoomEvent.LocalTrackUnpublished, () => {
      // console.log("本地轨道取消发布");
      this.renderParticipant(this.room.localParticipant);
      this.startTrackAttach(this.room.localParticipant)
      // this.renderScreenShare();
    });
    this.room.on(LiveKit.RoomEvent.TrackPublished, (publication, participant) => {
      this.startTrackAttach(participant)
    });
    this.room.on(LiveKit.RoomEvent.TrackUnpublished, (publication, participant) => {
      this.startTrackAttach(participant)
    });
    this.room.on(LiveKit.RoomEvent.TrackSubscribed, (track, publication, participant) => {
      // console.log("其他人员接入", track, publication, participant);
      this.renderParticipant(participant);
      this.startTrackAttach(participant)
      // this.renderScreenShare();
    });
    this.room.on(LiveKit.RoomEvent.TrackUnsubscribed, (track, publication, participant) => {
      // console.log("其他人员取消接入", track, publication, participant);
      this.renderParticipant(participant);
      this.startTrackAttach(participant)
      // this.renderScreenShare();
    });
    // 监听roomMetadata变化（20241125新增）
    this.room.on(LiveKit.RoomEvent.RoomMetadataChanged, (newRoomMetadata) => {
      this.emit("roomMetadataChanged", newRoomMetadata)
    })
  }
  // 退出房间
  async leaveRoom() {
    if (this.room) {
      await this.room.disconnect();
      this.room = null;
    }
  }
  // 删除正在进行的会议
  deleteRoom(identity) {
    return new Promise((resolve, reject) => {
      const closeRoomMessage = {
        cmd: "DeleteRoom",
        data: {
          room: this.roomNum,
          identity: identity,
        },
      }
      this.socket.send(closeRoomMessage)
      this.socket.addMessageHandler("DeleteRoom", (message) => {
        const msg = JSON.parse(message)
        if(msg.cmd === "DeleteRoom") {
          if(msg?.data.code == "200") {
            resolve();
          } else {
            reject(msg?.data.msg)
          }
        }
      })
    })
  }
  // 关闭socket链接
  closeSocket() {
    this.socket.close()
  }
  async publishLocalParticipant() {
    const tracks = await LiveKit.createLocalTracks({
      audio: true,
      video: true,
    });
    console.log('createLocalTrack', tracks);
    if(this.room.localParticipant) {
      tracks.forEach(track => {
        this.room.localParticipant.publishTrack(tracks)
      })
    }
  }
  // 切换音频输入设备
  async changeAudioDevice(deviceId) {
    if (this.room) {
      await this.room.switchActiveDevice("audioinput", deviceId);
    }
  }
  // 切换视频输入设备
  async changeVideoDevice(deviceId) {
    if (this.room) {
      try {
        let result = await this.room.switchActiveDevice("videoinput", deviceId);
        console.log('changeVideoDevice-result', result);
      } catch(err) {
        throw new Error('sdkError', err)
      }
    }
  }
  // 切换音频输出设备
  async changeOutputDevice(deviceId) {
    if (this.room) {
      await this.room.switchActiveDevice("audiooutput", deviceId);
    }
  }
	// 翻转摄像头
	flipCamera() {
		const videoPub = this.room?.localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
		if(!videoPub) {
			this.emit("errorPush", {
			  type: 'flipCamera',
			  message: '请先加入房间'
			})
			return
		}
		const constraints = videoPub.videoTrack?.constraints
		let options = {
			facingMode: constraints.facingMode === 'user' ? 'environment' : 'user'
		}
		videoPub.videoTrack?.restartTrack(options)
		return options.facingMode
	}
  // 启动/关闭摄像头
  async changeCameraStatus() {
    if (this.room && this.room.localParticipant) {
      let localParticipant = this.room.localParticipant;
      if (localParticipant.isCameraEnabled) {
        await localParticipant.setCameraEnabled(false);
      } else {
        await localParticipant.setCameraEnabled(true);
      }
      this.emit("localCameraChange", localParticipant.isCameraEnabled);
    } else {
      this.emit("errorPush", {
        type: 'changeCameraStatus',
        message: '请先加入房间'
      })
    }
  }
  // 启动/关闭麦克风
  async changeMicrophoneStatus() {
    if (this.room && this.room.localParticipant) {
      let localParticipant = this.room.localParticipant;
      if (localParticipant.isMicrophoneEnabled) {
        await localParticipant.setMicrophoneEnabled(false);
      } else {
        await localParticipant.setMicrophoneEnabled(true);
      }
      this.emit("localMicrophoneChange", localParticipant.isMicrophoneEnabled);
    } else {
      this.emit("errorPush", {
        type: 'changeMicrophoneStatus',
        message: '请先加入房间'
      })
    }
  }
  // 启用/关闭屏幕共享
  async changeScreenShareStatus() {
    if (this.room && this.room.localParticipant) {
      let localParticipant = this.room.localParticipant;
      const enable = localParticipant.isScreenShareEnabled;
      await localParticipant.setScreenShareEnabled(!enable, {
        audio: true,
      });
      if(!enable) {
        this.#getSdkCallNum('share')
      }
      this.emit("localScreenShareChange", localParticipant.isScreenShareEnabled);
    } else {
      this.emit("errorPush", {
        type: 'changeScreenShareStatus',
        message: '请先加入房间'
      })
    }
  }
  // 启用/关闭指定参与者麦克风
  async changeParticipantMicrophoneStatus(identity, mute) {
    if (!identity) {
      this.emit("errorPush", {
        type: 'changeParticipantMicrophoneStatus',
        message: '请输入要查询的identity'
      })
      return;
    }
    let currentParticipant = this.room.getParticipantByIdentity(identity);
    if (!currentParticipant) {
      this.emit("errorPush", {
        type: 'changeParticipantMicrophoneStatus',
        message: '该参与者不存在'
      })
      return;
    }
    if (currentParticipant.isLocal) {
      // 当前为本地参与者
      await currentParticipant.setMicrophoneEnabled(!mute);
      this.emit("localMicrophoneChange", this.room.localParticipant.isMicrophoneEnabled);
    } else {
      // 当前为远程参与者
      let audioTrackPub = currentParticipant.getTrackPublication(LiveKit.Track.Source.Microphone);
      let trackId = audioTrackPub ? audioTrackPub.trackSid : undefined;
      if (!trackId) {
        this.emit("errorPush", {
          type: 'changeParticipantMicrophoneStatus',
          message: 'trackId不存在'
        })
        return;
      }
      if (mute) {
        let muteMessage = {
          cmd: "Mute",
          data: {
            org: this.org,
            room: this.roomNum,
            input: "1",
            identity: identity,
            trackId: trackId,
          },
        };
        this.socket.addMessageHandler("Mute", (message) => {
          let msg = JSON.parse(message);
          if (msg.cmd === "Mute") {
            if(msg?.data.code == "200") {

            } else {
              this.emit("errorPush", {
                type: 'changeParticipantMicrophoneStatus',
                message: msg.data?.msg ?? '关闭指定参与者麦克风失败'
              })
            }
          }
        });
        this.socket.send(muteMessage);
      } else {
        let muteMessage = {
          cmd: "UnMute",
          data: {
            org: this.org,
            room: this.roomNum,
            input: "1",
            identity: identity,
            trackId: trackId,
          },
        };
        this.socket.addMessageHandler("UnMute", (message) => {
          let msg = JSON.parse(message);
          if (msg.cmd === "UnMute") {
            if(msg?.data.code == "200") {

            } else {
              if(msg.data?.msg) {
                this.emit("errorPush", {
                  type: 'changeParticipantMicrophoneStatus',
                  message: msg.data?.msg ?? '请求开启指定参与者麦克风失败'
                })
              }
            }
          }
        });
        this.socket.send(muteMessage);
      }
    }
  }
  // 启用/关闭指定参与者摄像头
  async changeParticipantCameraStatus(identity, mute) {
    if (!identity) {
      this.emit("errorPush", {
        type: 'changeParticipantCameraStatus',
        message: '请输入要查询的identity'
      })
      return;
    }
    let currentParticipant = this.room.getParticipantByIdentity(identity);
    if (!currentParticipant) {
      this.emit("errorPush", {
        type: 'changeParticipantCameraStatus',
        message: '该参与者不存在'
      })
      return;
    }
    if (currentParticipant.isLocal) {
      // 当前为本地参与者
      await currentParticipant.setCameraEnabled(!mute);
      this.emit("localCameraChange", this.room.localParticipant.isCameraEnabled);
    } else {
      // 当前为远程参与者
      let videoTrackPub = currentParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
      let trackId = videoTrackPub ? videoTrackPub.trackSid : undefined;
      if (!trackId) {
        this.emit("errorPush", {
          type: 'changeParticipantCameraStatus',
          message: 'trackId不存在'
        })
        return;
      }
      if (mute) {
        let muteMessage = {
          cmd: "Mute",
          data: {
            org: this.org,
            room: this.roomNum,
            identity: identity,
            trackId: trackId,
            input: "2",
          },
        };
        this.socket.addMessageHandler("Mute", (message) => {
          let msg = JSON.parse(message);
          if (msg.cmd === "Mute") {
            if(msg?.data.code == "200") {

            } else {
              this.emit("errorPush", {
                type: 'changeParticipantCameraStatus',
                message: msg.data?.msg ?? '关闭指定参与者摄像头失败'
              })
            }
          }
        });
        this.socket.send(muteMessage);
      } else {
        let muteMessage = {
          cmd: "UnMute",
          data: {
            org: this.org,
            room: this.roomNum,
            identity: identity,
            trackId: trackId,
            input: "2",
          },
        };
        this.socket.addMessageHandler("UnMute", (message) => {
          let msg = JSON.parse(message);
          if (msg.cmd === "UnMute") {
            if(msg?.data.code == "200") {

            } else {
              if(msg.data?.msg) {
                this.emit("errorPush", {
                  type: 'changeParticipantCameraStatus',
                  message: msg.data?.msg ?? '请求打开指定参与者摄像头失败'
                })
              }
            }
          }
        });
        this.socket.send(muteMessage);
      }
    }
  }
  // 与会者连接
  #participantConnected(participant) {
    // const { identity } = participant;
    participant.on(LiveKit.ParticipantEvent.ParticipantMetadataChanged, () => {
      this.renderParticipant(participant)
    })
    participant.on(LiveKit.ParticipantEvent.TrackMuted, (pub) => {
      // console.log("轨道静音", pub);
      this.renderParticipant(participant);
      this.startTrackAttach(participant)
    });
    participant.on(LiveKit.ParticipantEvent.TrackUnmuted, (pub) => {
      // console.log("轨道解除静音", pub);
      this.renderParticipant(participant);
      this.startTrackAttach(participant)
    });
    participant.on(LiveKit.ParticipantEvent.IsSpeakingChanged, () => {
      this.renderParticipant(participant);
    });
    participant.on(LiveKit.ParticipantEvent.ConnectionQualityChanged, () => {
      this.renderParticipant(participant);
    });
    participant.on(LiveKit.ParticipantEvent.ParticipantNameChanged, (name) => {
      this.renderParticipant(participant);
    });
  }
  // 远程与会者中断链接
  #participantDisconnected(participant) {
    this.renderParticipant(participant, true);
  }
  // 房间断开连接
  async #handleRoomDisconnect(reason) {
    if (!this.room) {
      return;
    }
    this.renderParticipant(this.room.localParticipant, true);
    console.log('远程与会者列表', this.room.remoteParticipants);
    this.room.remoteParticipants.forEach((p) => {
      this.renderParticipant(p, true);
    });
    await this.#dispatchInCall();
  }
  // 音视频轨道绑定
  startTrackAttach(participant) {
    const renderItem = {}
    renderItem.name = participant.name ? participant.name : participant.identity
    renderItem.roomMode = this.roomMode;
    renderItem.identity = participant.identity;
    renderItem.audioLevel = participant.audioLevel;
    renderItem.isSpeaking = participant.isSpeaking;
    renderItem.metadata = participant.metadata;
    renderItem.sid = participant.sid;
    renderItem.isLocal = participant.isLocal;
    renderItem.isMicrophoneEnabled = participant.isMicrophoneEnabled;
    if(this.roomMode === "auto") {
      renderItem.isCameraEnabled = participant.isCameraEnabled;
      renderItem.isScreenShareEnabled = participant.isScreenShareEnabled
      let cameraPub = participant.getTrackPublication(LiveKit.Track.Source.Camera);
      let micPub = participant.getTrackPublication(LiveKit.Track.Source.Microphone);
      if(participant.isScreenShareEnabled) {
        cameraPub = participant.getTrackPublication(LiveKit.Track.Source.ScreenShare)
        micPub = participant.getTrackPublication(LiveKit.Track.Source.ScreenShareAudio)
      }
      renderItem.videoTrack = cameraPub ? cameraPub.videoTrack : undefined;
      renderItem.audioTrack = micPub ? micPub.audioTrack : undefined;
      this.emit("trackAttach", renderItem);
    } else {
      let micPub = participant.getTrackPublication(LiveKit.Track.Source.Microphone);
      renderItem.audioTrack = micPub ? micPub.audioTrack : undefined;
      this.emit("trackAttach", renderItem);
    }
  }
  // 远程与会者画面渲染到页面上
  renderParticipant(participant, remove = false) {
    if (this.roomMode === "auto") {
      this.renderToVideo(participant, remove);
    } else if (this.roomMode === "audio") {
      this.renderToAudio(participant, remove);
    }
  }
  // 视频渲染
  renderToVideo(participant, remove) {
    let renderItem = {};
    const {
      name,
      identity,
      audioLevel,
      isSpeaking,
      metadata,
      sid,
      isCameraEnabled,
      isLocal,
      isMicrophoneEnabled,
      isScreenShareEnabled,
      connectionQuality
    } = participant;
    renderItem.remove = remove;
    renderItem.name = name ? name : identity
    renderItem.roomMode = this.roomMode;
    renderItem.identity = identity;
    renderItem.audioLevel = audioLevel;
    renderItem.isSpeaking = isSpeaking;
    renderItem.metadata = metadata;
    renderItem.sid = sid;
    renderItem.isCameraEnabled = isCameraEnabled;
    renderItem.isLocal = isLocal;
    renderItem.isMicrophoneEnabled = isMicrophoneEnabled;
    renderItem.isScreenShareEnabled = isScreenShareEnabled;
    renderItem.connectionQuality = connectionQuality;
    // console.log(`${identity} screenShare`, isScreenShareEnabled);
    let cameraPub = participant.getTrackPublication(LiveKit.Track.Source.Camera);
    let micPub = participant.getTrackPublication(LiveKit.Track.Source.Microphone);
    if(isScreenShareEnabled) {
      cameraPub = participant.getTrackPublication(LiveKit.Track.Source.ScreenShare)
      micPub = participant.getTrackPublication(LiveKit.Track.Source.ScreenShareAudio)
    }
    // console.log('cameraPub', cameraPub);
    // console.log('micPub', micPub);
    renderItem.videoTrack = cameraPub ? cameraPub.videoTrack : undefined;
    renderItem.audioTrack = micPub ? micPub.audioTrack : undefined;
    this.emit("videoCallRender", renderItem);
  }
  // 音频渲染
  renderToAudio(participant, remove) {
    let renderItem = {};
    const {
      name,
      identity,
      audioLevel,
      isSpeaking,
      metadata,
      sid,
      // isCameraEnabled,
      isLocal,
      isMicrophoneEnabled,
      connectionQuality
    } = participant;
    renderItem.remove = remove;
    renderItem.name = name ? name : identity
    renderItem.roomMode = this.roomMode;
    renderItem.identity = identity;
    renderItem.audioLevel = audioLevel;
    renderItem.isSpeaking = isSpeaking;
    renderItem.metadata = metadata;
    renderItem.sid = sid;
    renderItem.isLocal = isLocal;
    renderItem.isMicrophoneEnabled = isMicrophoneEnabled;
    renderItem.connectionQuality = connectionQuality;
    const micPub = participant.getTrackPublication(LiveKit.Track.Source.Microphone);
    renderItem.audioTrack = micPub ? micPub.audioTrack : undefined;
    this.emit("audioCallRender", renderItem);
  }
  updateVideoSize(element, target) {
    target.innerHTML = `(${element.videoWidth}x${element.videoHeight})`;
  }
  // 发送语音播报(20241108新增)
  sendInviteNotice(name, dnis, roomNum) {
    return new Promise((resolve, reject) => {
      if(!roomNum) {
        reject("缺少roomNum参数")
      }
      fetch(this.baseUrl + "/api/v1/noticeMakeCall", {
        method: "POST",
        headers: {
          "Content-Type": "application/json;charset=utf-8",
        },
        body: JSON.stringify({
          name,
          dnis,
          room: roomNum
        })
      }).then(response => {
        return response.json()
      }).then(result => {
        if(result?.code == 200) {
          resolve()
        } else {
          reject(result?.msg ?? "语音播报接口出错")
        }
      }).catch(err => {
        reject(err)
      })
    })
  }
}

export { LiveClient, LiveKit };
