import request from '@/utils/request'

export function getSmartOverview() {
  return request({
    url: '/api/admin/smart/advisor/overview',
    method: 'get'
  })
}
