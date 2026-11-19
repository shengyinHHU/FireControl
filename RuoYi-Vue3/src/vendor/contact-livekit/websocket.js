class WebSocketClient {
  #heartInterval = null
  constructor(url) {
    this.url = url;
    this.socket = null;
    this.isOpen = false
    this.messageHandlers = new Map();
    this.openHandlers = new Map();
    this.closeHandlers = [];
    this.errorHandlers = [];
  }

  connect() {
    console.log('connecting');
    this.socket = new WebSocket(this.url);
    this.socket.onopen = event => {
      console.log('链接已打开');
      this.isOpen = true
      if(this.openHandlers.size > 0) {
        this.openHandlers.forEach(handler => handler());
      }
      // 定时发送心跳检测信息
      if(this.#heartInterval) {
        clearInterval(this.#heartInterval);
        this.#heartInterval = null
      }
      this.#heartInterval = setInterval(() => {
        this.send({
          cmd: 'HeartBreak'
        })
      }, 10000)
    };

    this.socket.onmessage = event => {
      console.log('messageEvent', event);
      if(this.messageHandlers.size > 0) {
        this.messageHandlers.forEach(handler => handler(event.data));
      }
    };

    this.socket.onclose = event => {
      console.log('链接已关闭');
      this.closeHandlers.forEach(handler => handler(event));
    };
    this.socket.onerror = event => {
      console.log('链接报错', event);
      this.errorHandlers.forEach(handler => handler(event));
    };
  }

  send(message) {
    console.log('state', this.socket.readyState);
    console.log('message', JSON.stringify(message));
    console.log('isOpen', this.isOpen);
    if (this.socket.readyState === WebSocket.OPEN) {
      // 链接打开
      this.socket.send(JSON.stringify(message));
    } else if(this.socket.readyState === WebSocket.CLOSED) {
      if(this.isOpen) {
        // 链接未通过close方法关闭
        console.error("WebSocket链接异常关闭");
        // 重连
        this.connect()
        this.clearOpenHandler()
        this.addOpenHandler('MsgSend', () => {
          this.socket.send(JSON.stringify(message));
        })
      } else {
        console.log('websocket链接已关闭');
      }
    } else {
      this.clearOpenHandler()
      this.addOpenHandler('MsgSend', () => {
        this.socket.send(JSON.stringify(message));
      })
    }
  }

  addMessageHandler(key, handler) {
    if(this.messageHandlers.has(key)) {
      this.messageHandlers.delete(key)
      this.messageHandlers.set(key, handler);
    } else {
      this.messageHandlers.set(key, handler);
    }
  }
  addOpenHandler(key, handler) {
    if(this.openHandlers.has(key)) {
      this.openHandlers.delete(key)
      this.openHandlers.set(key, handler);
    } else {
      this.openHandlers.set(key, handler);
    }
  }
  addCloseHandler(handler) {
    this.closeHandlers.push(handler);
  }
  addErrorHandler(handler) {
    this.errorHandlers.push(handler);
  }
  clearMessageHandler() {
    this.messageHandlers.clear();
  }
  clearOpenHandler() {
    this.openHandlers.clear();
  }
  clearCloseHandler() {
    this.closeHandlers = []
  }
  clearErrorHandler() {
    this.errorHandlers = []
  }
  close() {
    this.socket.close();
    this.isOpen = false;
    // 关闭心跳监测
    if(this.#heartInterval) {
      clearInterval(this.#heartInterval)
      this.#heartInterval = null
    }
  }
}
export default WebSocketClient;
