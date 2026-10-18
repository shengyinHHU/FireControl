import { isFireMock } from './config'
import auth from '@/plugins/auth'
export const canFire = (resource, action = 'list') =>
  isFireMock || auth.hasPermi(`fire:${resource}:${action}`)

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
