// Only imported by the Vite server. Never import this module from src/.
export function createAmapProxy(securityCode) {
  if (!securityCode) return {}
  const options = target => ({
    target,
    changeOrigin: true,
    rewrite: path => path.replace(/^\/_AMapService/, ''),
    configure(proxy) {
      proxy.on('proxyReq', request => {
        const url = new URL(request.path, target)
        url.searchParams.set('jscode', securityCode)
        request.path = url.pathname + url.search
      })
    }
  })
  return {
    '/_AMapService/v4/map/styles': options('https://webapi.amap.com'),
    '/_AMapService': options('https://restapi.amap.com')
  }
}
