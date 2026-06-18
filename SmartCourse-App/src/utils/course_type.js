const COURSE_TYPE_META = Object.freeze({
  PUBLIC_REQUIRED: { label: '通识教育必修', color: '#2274a5' },
  GENERAL_ELECTIVE: { label: '通识教育选修', color: '#268473' },
  DISCIPLINE_REQUIRED: { label: '学科基础', color: '#714c9f' },
  MAJOR_REQUIRED: { label: '专业发展必修', color: '#ba582d' },
  MAJOR_ELECTIVE: { label: '专业发展选修', color: '#286ab0' },
  PRACTICE_REQUIRED: { label: '综合实践', color: '#ab4853' }
})

const DEFAULT_META = { label: '课程分类', color: '#566b7f' }

export function getCourseTypeMeta(courseType) {
  return COURSE_TYPE_META[courseType] || DEFAULT_META
}
