import mitt from 'mitt'

class EventEmitter {
  #emitter
  constructor() {
    this.#emitter = mitt()
  }

  // 绑定事件监听
  on(eventName, handler) {
    this.#emitter.on(eventName, handler)
  }
  // 解除事件监听
  off(eventName, handler = null) {
    if(handler && (typeof handler === 'function')) {
      this.#emitter.off(eventName, handler)
    } else {
      this.#emitter.off(eventName)
    }
  }
  // 触发事件监听
  emit(eventName, event) {
    this.#emitter.emit(eventName, event)
  }
  // 移除所有事件监听器
  removeAllListeners() {
    this.#emitter.all.clear()
  }
}

export default EventEmitter