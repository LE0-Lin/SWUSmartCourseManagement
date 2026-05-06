<template>
  <div class="app-container">
    <el-card class="schedule-card">
      <template slot="header">
        <div class="card-header">
          <span>我的课表</span>
          <el-select v-model="currentWeek" size="small" class="week-select">
            <el-option
              v-for="week in weekOptions"
              :key="week"
              :label="`第 ${week} 周`"
              :value="week"
            />
          </el-select>
        </div>
      </template>

      <div v-loading="loading" class="schedule-container">
        <div class="time-axis">
          <div v-for="period in periods" :key="period.key" class="time-slot">
            <strong>{{ period.label }}</strong>
            <span>{{ period.time }}</span>
          </div>
        </div>
        <div class="schedule-grid">
          <div class="weekday-header">
            <div v-for="weekday in weekdays" :key="weekday.value" class="weekday">{{ weekday.label }}</div>
          </div>
          <div class="schedule-body">
            <div v-for="period in periods" :key="period.key" class="period-row">
              <div v-for="weekday in weekdays" :key="weekday.value" class="schedule-cell">
                <div
                  v-for="course in getCourses(weekday.value, period)"
                  :key="course.id"
                  class="course-item"
                  :style="{ backgroundColor: getCourseColor(course.courseId) }"
                >
                  <div class="course-title">{{ course.title }}</div>
                  <div class="course-info">{{ course.location || '地点待定' }}</div>
                  <div class="course-sections">第 {{ course.sectionStart }}-{{ course.sectionEnd }} 节</div>
                  <div class="course-weeks">第 {{ course.startWeek || 1 }}-{{ course.endWeek || 21 }} 周</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && courses.length === 0" description="当前没有课程安排" />
    </el-card>
  </div>
</template>

<script>
import { getMySchedule } from '@/api/schedule'

export default {
  name: 'TeacherSchedule',
  data() {
    return {
      loading: false,
      weekdays: [
        { label: '周一', value: 1 },
        { label: '周二', value: 2 },
        { label: '周三', value: 3 },
        { label: '周四', value: 4 },
        { label: '周五', value: 5 },
        { label: '周六', value: 6 },
        { label: '周日', value: 7 }
      ],
      periods: [
        { key: '12', label: '1-2 节', start: 1, end: 2, time: '8:00-9:40' },
        { key: '34', label: '3-4 节', start: 3, end: 4, time: '10:00-11:40' },
        { key: '56', label: '5-6 节', start: 5, end: 6, time: '14:00-15:40' },
        { key: '78', label: '7-8 节', start: 7, end: 8, time: '16:00-17:40' },
        { key: '910', label: '9-10 节', start: 9, end: 10, time: '19:00-20:40' }
      ],
      courses: [],
      currentWeek: 1,
      weekOptions: Array.from({ length: 21 }, (_, index) => index + 1)
    }
  },
  created() {
    this.fetchSchedule()
  },
  methods: {
    fetchSchedule() {
      this.loading = true
      getMySchedule().then(resp => {
        this.courses = resp.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    getCourses(weekday, period) {
      return this.courses.filter(course => {
        const startWeek = Number(course.startWeek || 1)
        const endWeek = Number(course.endWeek || 21)
        return course.dayOfWeek === weekday &&
          course.sectionStart === period.start &&
          course.sectionEnd === period.end &&
          this.currentWeek >= startWeek &&
          this.currentWeek <= endWeek
      })
    },
    getCourseColor(courseId) {
      const colors = ['#3478f6', '#1d9a6c', '#d47c16', '#c23b4b', '#6c5ce7', '#008c9e']
      return colors[courseId % colors.length]
    }
  }
}
</script>

<style scoped lang="scss">
.schedule-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.week-select {
  width: 128px;
}

.schedule-container {
  display: flex;
  margin-top: 20px;
  overflow-x: auto;
}

.time-axis {
  width: 100px;
  margin-right: 10px;
  flex-shrink: 0;

  .time-slot {
    height: 120px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    font-size: 12px;
    color: #606266;

    span {
      margin-top: 4px;
      color: #909399;
    }
  }
}

.schedule-grid {
  min-width: 820px;
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.weekday-header {
  display: flex;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;

  .weekday {
    flex: 1;
    height: 42px;
    line-height: 42px;
    text-align: center;
    font-weight: 600;
    color: #303133;
  }
}

.schedule-body {
  .period-row {
    display: flex;
  }

  .schedule-cell {
    flex: 1;
    min-height: 120px;
    border-right: 1px solid #e4e7ed;
    border-bottom: 1px solid #e4e7ed;
    padding: 6px;
    background: #fff;

    &:last-child {
      border-right: none;
    }
  }
}

.course-item {
  min-height: 96px;
  padding: 9px;
  border-radius: 6px;
  color: #fff;
  box-shadow: 0 8px 18px rgba(40, 58, 90, 0.16);

  .course-title {
    font-size: 13px;
    font-weight: 700;
    margin-bottom: 6px;
    line-height: 1.35;
  }

  .course-info,
  .course-sections,
  .course-weeks {
    font-size: 11px;
    line-height: 1.6;
  }
}
</style>
