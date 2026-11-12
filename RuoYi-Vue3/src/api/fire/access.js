import { isFireMock } from './config'
import auth from '@/plugins/auth'
import { canWriteResource } from './sourcePolicy'
export const canFire = (resource, action = 'list') =>
  (!['add', 'edit', 'remove'].includes(action) || canWriteResource(resource)) &&
  ((isFireMock && !['places','cameras','inspections','video','gateways','hosts','alarms','protocolMessages','sms'].includes(resource)) || auth.hasPermi(`fire:${resource === 'cameras' ? 'video' : resource}:${action}`))

export function filterFireMenus(routes) {
  return routes.flatMap((route) => {
    const resource = route.meta?.fireResource
    if (
      resource &&
      !(resource === 'sms'
        ? canFire('sms') || canFire('smsRules')
        : canFire(resource))
    )
      return []
    if (!route.children) return [route]
    const children = filterFireMenus(route.children)
    return children.length ? [{ ...route, children }] : []
  })
}
