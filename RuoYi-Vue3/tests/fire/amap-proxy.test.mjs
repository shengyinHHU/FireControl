import test from 'node:test'
import assert from 'node:assert/strict'
import { createAmapProxy } from '../../vite/amapProxy.js'
test('AMap proxy injects security code server-side and routes custom styles separately', () => {
  const routes = createAmapProxy('test-server-secret')
  assert.equal(routes['/_AMapService/v4/map/styles'].target, 'https://webapi.amap.com')
  const proxy = routes['/_AMapService']
  assert.equal(proxy.target, 'https://restapi.amap.com')
  assert.equal(proxy.rewrite('/_AMapService/v3/config?key=public'), '/v3/config?key=public')
  let handler
  proxy.configure({ on(event, fn) { assert.equal(event,'proxyReq'); handler=fn } })
  const request = { path:'/v3/config?key=public&jscode=invalid' }
  handler(request)
  const url = new URL(request.path,proxy.target)
  assert.equal(url.searchParams.get('jscode'),'test-server-secret')
  assert.equal(url.searchParams.getAll('jscode').length,1)
  assert.deepEqual(createAmapProxy(''),{})
})
