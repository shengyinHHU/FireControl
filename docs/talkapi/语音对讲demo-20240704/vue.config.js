const port = 8000;

module.exports = {
  publicPath: './',
  outputDir: 'dist',
  assetsDir: 'static',
  lintOnSave: false,
  productionSourceMap: false,
  devServer: {
    port: port,
    host: '0.0.0.0',
    overlay: {
      warnings: true,
      errors: true
    },
    proxy: {
      '/': {
        target: '本地请求地址',
        changeOrigin: true
      }
    },
    headers: {
      'Access-Control-Allow-Origin': '*'
    }
  },
  configureWebpack: {},
  chainWebpack() {},
  css: {}
};
