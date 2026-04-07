import request from '@/utils/request'

const api_prefix = '/api/teacher/grade'

// 获取某课程的所有学生及其成绩
export function getCourseGrades(courseId) {
  return request({
    url: `${api_prefix}/list/${courseId}`,
    method: 'get'
  })
}

// 保存/更新成绩
export function saveGrade(data) {
  return request({
    url: `${api_prefix}/save`,
    method: 'post',
    data
  })
}

// 获取成绩分布数据
export function getGradeDistribution(courseId) {
  return request({
    url: `${api_prefix}/distribution/${courseId}`,
    method: 'get'
  })
}
