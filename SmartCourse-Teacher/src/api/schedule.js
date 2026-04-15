import request from '@/utils/request'

const api_prefix = '/api/teacher/schedule'

// 获取某课程的课表安排
export function getCourseSchedule(courseId) {
  return request({
    url: `${api_prefix}/list/${courseId}`,
    method: 'get'
  })
}

export function getMySchedule() {
  return request({
    url: `${api_prefix}/mine`,
    method: 'get'
  })
}

// 保存/更新课表安排
export function saveSchedule(data) {
  return request({
    url: `${api_prefix}/save`,
    method: 'post',
    data
  })
}

// 删除某节课程安排
export function deleteSchedule(id) {
  return request({
    url: `${api_prefix}/delete/${id}`,
    method: 'post'
  })
}
