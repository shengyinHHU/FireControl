import request from '@/utils/request'
export const getGateway = id => request({url:`/fire/assets/gateways/${encodeURIComponent(id)}`})
export const saveAsset = (kind,data) => request({url:`/fire/assets/${kind}${data.id?'/'+encodeURIComponent(data.id):''}`,method:data.id?'put':'post',data})
export const removeAsset = (kind,row) => request({url:`/fire/assets/${kind}/${encodeURIComponent(row.id)}`,method:'delete',params:{revision:row.revision,...(kind==='hosts'?{gatewayId:row.gatewayId}:{})}})

export const unregisteredDevices = () => request({url:'/fire/assets/unregistered'})
