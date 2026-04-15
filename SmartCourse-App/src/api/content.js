import request from '@/utils/request'

const api_prefix = '/api/app/pub/content'

// 获取所有分类
export function getSubjects() {
  return request({
    url: `${api_prefix}/get/subjects`,
    method: 'get'
  })
}

// 获取分类父分类
export function getSubjectParent(subjectId) {
  return request({
    url: `${api_prefix}/get/subject/parent/${subjectId}`,
    method: 'get'
  })
}

// 获取所有banner
export function getBanners() {
  return request({
    url: `${api_prefix}/get/banners`,
    method: 'get'
  })
}

// 获取所有课程
export function getCourses(data) {
  return request({
    url: `${api_prefix}/get/courses`,
    method: 'post',
    data
  })
}

// 获取课程详情
export function getCourseDetail(id) {
  return request({
    url: `${api_prefix}/get/courses/${id}`,
    method: 'get'
  })
}

// 获取讲师信息
export function getTeacher(id) {
  return request({
    url: `${api_prefix}/get/teacher/${id}`,
    method: 'get'
  })
}

// 获取课程订阅订单列表
export function getCourseOrders(data) {
  return request({
    url: `${api_prefix}/get/course/orders`,
    method: 'post',
    data
  })
}

// 判断学员是否已经订阅课程
export function getIsBuyCourse(id) {
  return request({
    url: `${api_prefix}/get/course/isbuy/${id}`,
    method: 'get'
  })
}

// 获取课程章节/目录
export function getChapters(courseId) {
  return request({
    url: `${api_prefix}/get/chapters/${courseId}`,
    method: 'get'
  })
}

// 获取章节资源（原视频列表）
export function getChapterResources(chapterId) {
  return request({
    url: `${api_prefix}/get/resources/${chapterId}`,
    method: 'get'
  })
}

// 获取课程成绩
export function getCourseGrade(courseId) {
  return request({
    url: `${api_prefix}/get/grade/${courseId}`,
    method: 'get'
  })
}

export function getTranscript() {
  return request({
    url: `${api_prefix}/get/transcript`,
    method: 'get'
  })
}

// 获取课程安排/课表
export function getCourseSchedule(courseId) {
  return request({
    url: `${api_prefix}/get/schedule/${courseId}`,
    method: 'get'
  })
}

export function getMySchedule() {
  return request({
    url: `${api_prefix}/get/schedule/me`,
    method: 'get'
  })
}

// 获取课程评价
export function getCourseComment(data) {
  return request({
    url: `${api_prefix}/get/course/comment`,
    method: 'post',
    data
  })
}

// 发表评论
export function publishComment(data) {
  return request({
    url: `${api_prefix}/publish/comment`,
    method: 'post',
    data
  })
}

// 申请入驻
export function teacherApply(data) {
  return request({
    url: `${api_prefix}/teacher/apply`,
    method: 'post',
    data
  })
}

// 创建课程支付订单
export function createOrder(data) {
  return request({
    url: `${api_prefix}/course/pay/create`,
    method: 'post',
    data
  })
}

// 课程支付成功
export function orderPaySucceed(orderNo) {
  return request({
    url: `${api_prefix}/course/pay/succeed/${orderNo}`,
    method: 'post'
  })
}

// 选课相关API

// 选择课程
export function selectCourse(courseId) {
  return request({
    url: `${api_prefix}/course/select/${courseId}`,
    method: 'post'
  })
}

// 判断学员是否已经选择课程
export function getIsSelectCourse(courseId) {
  return request({
    url: `${api_prefix}/course/isselect/${courseId}`,
    method: 'get'
  })
}
