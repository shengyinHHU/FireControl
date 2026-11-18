import { fabric } from "fabric-with-erasing";
import { createCanvas } from "canvas";
import { LiveClient } from "./index";
import Arrow from "./arrow";

class FabricCanvas extends LiveClient {
  #canvas;
  #currentShape;
  #drawingTool;
  #isDrawing = false;
  #startX = 0;
  #startY = 0;
  #option = {
    stroke: "#ff0000",
    strokeWidth: 5,
    fill: "transparent",
    opacity: 1,
  };
  #images = [];
  #curImageIndex = 0;

  constructor({ canvasId, userId, org, token, roomName = "", roomMode = "auto", baseUrl } = {}) {
    super({ userId, org, token, roomName, roomMode, baseUrl });
    this.#canvas = new fabric.Canvas(canvasId, {
      isDrawingMode: true,
      selection: false,
      includeDefaultValues: false,
    });
    this.setDrawingTool("pencil");
    this.#initEvent();
  }
  // 创建画布
  static createCanvas(config, parentEle) {
    const canvasEle = createCanvas(config.width, config.height);
    canvasEle.id = config.id;
    canvasEle.style.border = "1px solid black";
    parentEle?.appendChild(canvasEle);
  }
  // 获取画布
  getCanvas() {
    return this.#canvas;
  }
  // 清空画布
  clearCanvas() {
    this.#canvas.clear();
  }
  // 设置画布背景色
  setBackgroundColor(color) {
    this.#canvas.setBackgroundColor(color, () => {
      this.#canvas.renderAll();
    });
  }
  // 设置画布背景图片（居中显示）
  setBackgroundImage(imageUrl, options = {}) {
    fabric.Image.fromURL(imageUrl, (image) => {
      const canvasWidth = this.#canvas.getWidth();
      const canvasHeight = this.#canvas.getHeight();
      const scale = canvasHeight / image.height;
      const imageWidth = image.width * scale;
      image.set({
        scaleX: scale,
        scaleY: scale,
        top: 0,
        left: (canvasWidth - imageWidth) / 2,
      });
      this.#canvas.setBackgroundImage(
        image,
        () => {
          this.#canvas.renderAll();
        },
        options
      );
    });
  }
  // 为画布添加元素
  addObject(object) {
    this.#canvas.add(object);
  }
  // 移除画布上给定元素
  removeObject(object) {
    this.#canvas.remove(object);
  }
  // 移除所有对象
  removeAllObject() {
    this.#canvas.getObjects().forEach((item) => {
      this.#canvas.remove(obj);
    });
  }
  // 获取画布上所有元素
  getObjects() {
    return this.#canvas.getObjects();
  }
  // 获取画布已激活对象
  getActiveObject() {
    return this.#canvas.getActiveObject();
  }
  // 设置激活对象
  setActiveObject(object) {
    this.#canvas.setActiveObject(object);
  }
  // 设置画布宽度
  setWidth(width) {
    this.#canvas.setWidth(width);
  }
  // 设置画布高度
  setHeight(height) {
    this.#canvas.setHeight(height);
  }
  // 设置绘图工具
  setDrawingTool(tool) {
    if (this.#drawingTool === tool) {
      return;
    }
    this.#canvas.isDrawingMode = false;
    this.#canvas.selection = false;
    this.#drawingTool = tool;
    switch (tool) {
      case "pencil":
        this.drawFreeDraw();
        break;
      case "eraser":
        this.eraser();
        break;
      case "select":
        this.#canvas.selection = true;
        this.#canvas.defaultCursor = "auto";
        break;
      default:
        break;
    }
  }
  setOptions(options) {
    this.#option = {
      ...this.#option,
      ...options,
    };
  }
  // 矩形绘制
  drawRect(options) {
    const rect = new fabric.Rect({ ...this.#option, ...options });
    this.#canvas.add(rect);
    this.#currentShape = rect;
    this.#canvas.defaultCursor = "crosshair";
  }
  // 三角形绘制
  drawTriangle(options) {
    const triangle = new fabric.Triangle({ ...this.#option, ...options });
    this.#canvas.add(triangle);
    this.#currentShape = triangle;
    this.#canvas.defaultCursor = "crosshair";
  }
  // 圆形绘制
  drawCircle(options) {
    const circle = new fabric.Circle({ ...this.#option, ...options });
    this.#canvas.add(circle);
    this.#currentShape = circle;
    this.#canvas.defaultCursor = "crosshair";
  }
  // 椭圆绘制
  drawEllipse(options) {
    const ellipse = new fabric.Ellipse({ ...this.#option, ...options });
    this.#canvas.add(ellipse);
    this.#currentShape = ellipse;
    this.#canvas.defaultCursor = "crosshair";
  }
  // 线条绘制
  drawLine(x1, y1, x2, y2, options = {}) {
    const line = new fabric.Line([x1, y1, x2, y2], { ...this.#option, ...options });
    this.#canvas.add(line);
    this.#currentShape = line;
    this.#canvas.defaultCursor = "crosshair";
    // this.setActiveObject(line);
  }
  // 箭头绘制
  drawArrow(x1, y1, x2, y2, options = {}) {
    const arrow = new Arrow([x1, y1, x2, y2], { ...this.#option, ...options });
    this.#canvas.add(arrow);
    this.#currentShape = arrow;
    this.#canvas.defaultCursor = "crosshair";
    // this.setActiveObject(arrow);
  }
  // 自由绘制
  drawFreeDraw() {
    this.#canvas.freeDrawingBrush = new fabric.PencilBrush(this.#canvas);
    this.#canvas.freeDrawingBrush.color = "#ff0000";
    this.#canvas.freeDrawingBrush.width = 5;
    this.#canvas.freeDrawingCursor = "default";
    this.#canvas.isDrawingMode = true;
  }
  // 文本绘制
  drawText(text, options = {}) {
    const textObj = new fabric.IText(text, {
      fontSize: 18,
      fill: "#ff0000",
      editingBorderColor: "#ff0000",
      padding: 5,
      ...options,
    });
    this.#canvas.add(textObj);
    this.#canvas.defaultCursor = "text";
    this.#currentShape = textObj;
    // 文本打开编辑模式
    textObj.enterEditing();
    // textObj.exitEditing();
    // 文本编辑框获取焦点
    textObj.hiddenTextarea.focus();
    this.setActiveObject(textObj);
  }
  // 插入图片
  insertImage(url, options = {}) {
    fabric.Image.fromURL(url, (img) => {
      if (options) {
        img.set(options);
      } else {
        // 计算图片居中的位置
        const canvasWidth = this.#canvas.getWidth();
        const canvasHeight = this.#canvas.getHeight();
        const imageWidth = img.width * img.scaleX;
        const imageHeight = img.height * img.scaleY;
        const left = (canvasWidth - imageWidth) / 2;
        const top = (canvasHeight - imageHeight) / 2;
        img.set({ left, top });
      }
      this.#canvas.add(img);
    });
  }
  // 插入ppt图片
  insertPPT(urls) {
    this.#images = urls;
    this.setCurrentScense(0);
    this.emit("insert:images", urls);
  }
  // 设置当前显示ppt图片
  setCurrentScense(index) {
    this.#curImageIndex = index;
    this.setBackgroundImage(this.#images[this.#curImageIndex]);
    this.emit("current:image", index);
  }
  // 添加橡皮擦
  eraser(options = {}) {
    this.#canvas.freeDrawingBrush = new fabric.EraserBrush(this.#canvas, options);
    this.#canvas.freeDrawingBrush.width = 10;
    this.#canvas.freeDrawingCursor = "default";
    this.#canvas.isDrawingMode = true;
    // 删除单个对象或者一组对象
    this.#canvas.on("erasing:end", (options) => {
      options.targets.forEach(
        (obj) => obj.group?.removeWithUpdate(obj) || this.#canvas.remove(obj)
      );
    });
    this.#canvas.renderAll();
  }
  // 初始化事件
  #initEvent() {
    // 绑定添加对象事件，将当前画布状态保存到撤销栈中
    this.#canvas.on("object:added", (e) => {
      this.emit("object:added", e);
    });

    this.#canvas.on("object:modified", (e) => {
      this.emit("object:modified", e);
    });

    this.#canvas.on("object:removed", (e) => {
      this.emit("object:removed", e);
    });

    // 画布重绘后同步到远程
    this.#canvas.on("after:render", (e) => {
      this.emit("after:render", e);
    });

    // 监听鼠标事件
    this.#canvas.on("mouse:down", this.#onMouseDown.bind(this));
    this.#canvas.on("mouse:move", this.#onMouseMove.bind(this));
    this.#canvas.on("mouse:up", this.#onMouseUp.bind(this));
  }
  // 鼠标按下事件处理函数
  #onMouseDown(event) {
    // 如果当前有活动的元素则不添加
    const activeObject = this.#canvas.getActiveObject();
    if (!event.pointer || activeObject) return;
    this.#isDrawing = true;
    const { x, y } = event.pointer;
    this.#startX = x;
    this.#startY = y;

    switch (this.#drawingTool) {
      case "rectangle":
        this.drawRect({
          left: x,
          top: y,
          width: 0,
          height: 0,
        });
        break;
      case "triangle":
        this.drawTriangle({
          left: x,
          top: y,
          width: 0,
          height: 0,
        });
        break;
      case "circle":
        this.drawCircle({
          left: x,
          top: y,
          radius: 0,
        });
        break;
      case "ellipse":
        this.drawEllipse({
          left: x,
          top: y,
          rx: 0,
          ry: 0,
        });
        break;
      case "line":
        this.drawLine(x, y, x, y);
        break;
      case "arrow":
        this.drawArrow(x, y, x, y);
        break;
      case "text":
        this.drawText("", { left: x, top: y });
        break;
      default:
        break;
    }
  }
  // 鼠标移动事件处理函数
  #onMouseMove(event) {
    if (!this.#isDrawing || !event.pointer || !this.#currentShape) {
      return;
    }
    const { x, y } = event.pointer;
    const width = Math.abs(x - this.#startX);
    const height = Math.abs(y - this.#startY);

    switch (this.#drawingTool) {
      case "rectangle":
        this.#currentShape.set({
          width,
          height,
        });
        break;
      case "triangle":
        this.#currentShape.set({
          width,
          height,
        });
        break;
      case "circle":
        const radius = Math.sqrt(width * width + height * height) / 2;
        this.#currentShape.set({
          radius,
        });
        break;
      case "ellipse":
        this.#currentShape.set({
          rx: Math.abs(width / 2),
          ry: Math.abs(height / 2),
        });
        break;
      case "line":
        this.#currentShape.set({
          x2: x,
          y2: y,
        });
        break;
      case "arrow":
        this.#currentShape.set({
          x2: x,
          y2: y,
        });
        break;
      default:
        break;
    }

    this.#canvas.renderAll();
  }
  // 鼠标抬起事件处理函数
  #onMouseUp() {
    this.#isDrawing = false;
    this.#currentShape = null;
    this.emit("mouse:up", null);
  }
  toDataURL(options = {}) {
    return this.#canvas.toDataURL(options);
  }

  toJSON() {
    return this.#canvas.toJSON();
  }

  loadFromJSON(json, callback = () => {}, reviver = () => {}) {
    return this.#canvas.loadFromJSON(json, callback, reviver);
  }

  renderAll() {
    return this.#canvas.renderAll();
  }

  /**
   * 缩放（以画布中心点放大）
   * @param ratio 缩放比例（0~1）
   */
  zoom(ratio = 1) {
    // 计算缩放中心
    const point = new fabric.Point(this.#canvas.width / 2, this.#canvas.height / 2);
    this.#canvas.zoomToPoint(point, ratio);
  }

  // 获取缩放比率
  getZoom() {
    return this.#canvas.getZoom();
  }

  // 放大（以画布中心点放大）
  zoomIn() {
    this.zoom(this.#canvas.getZoom() * 1.1);
  }

  // 缩小（以画布中心点缩小）
  zoomOut() {
    this.zoom(this.#canvas.getZoom() / 1.1);
  }

  // 销毁事件监听
  destroy() {
    this.removeAllListeners();
    // 销毁画布
    // this.canvas.removeListeners();
    this.#canvas.dispose();
  }
}

export default FabricCanvas;
