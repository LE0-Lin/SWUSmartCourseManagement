import request from '@/utils/request'

const api_prefix = '/api/admin/schedule'

export function getCourseSchedule(courseId) {
  return request({
    url: `${api_prefix}/list/${courseId}`,
    method: 'get'
  })
}

export function saveSchedule(data) {
  return request({
    url: `${api_prefix}/save`,
    method: 'post',
    data
  })
}

export function deleteSchedule(id) {
  return request({
    url: `${api_prefix}/delete/${id}`,
    method: 'post'
  })
}
