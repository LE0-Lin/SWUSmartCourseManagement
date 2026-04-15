<template>
  <div class="app-container">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>我的课表</span>
          <el-select v-model="currentWeek" size="small" style="width: 120px">
            <el-option v-for="week in weekOptions" :key="week" :label="`第${week}周`" :value="week" />
          </el-select>
        </div>
      </template>

      <div v-loading="loading" class="schedule-container">
        <div class="time-axis">
          <div v-for="period in periods" :key="period.key" class="time-slot">
            {{ period.time }}
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
                  <div class="course-teacher">{{ course.teacherName }}</div>
                  <div class="course-info">{{ course.location || '待定' }}</div>
                  <div class="course-weeks">第{{ course.startWeek || 1 }}-{{ course.endWeek || 21 }}周</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && courses.length === 0" description="当前没有已选课程课表" />
    </el-card>
  </div>
</template>

<script>
import { getMySchedule } from '@/api/content'

export default {
  name: 'StudentSchedule',
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
        { key: '12', start: 1, end: 2, time: '8:00-9:40' },
        { key: '34', start: 3, end: 4, time: '10:00-11:40' },
        { key: '56', start: 5, end: 6, time: '14:00-15:40' },
        { key: '78', start: 7, end: 8, time: '16:00-17:40' },
        { key: '910', start: 9, end: 10, time: '19:00-20:40' }
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
      return this.courses.filter(course => course.dayOfWeek === weekday &&
        course.sectionStart === period.start &&
        course.sectionEnd === period.end &&
        this.currentWeek >= Number(course.startWeek || 1) &&
        this.currentWeek <= Number(course.endWeek || 21))
    },
    getCourseColor(courseId) {
      const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
      return colors[courseId % colors.length]
    }
  }
}
</script>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.schedule-container {
  display: flex;
  margin-top: 20px;
}

.time-axis {
  width: 95px;
  margin-right: 10px;

  .time-slot {
    height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    text-align: center;
    font-size: 12px;
    color: #909399;
  }
}

.schedule-grid {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.weekday-header {
  display: flex;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;

  .weekday {
    flex: 1;
    height: 40px;
    line-height: 40px;
    text-align: center;
    font-weight: bold;
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
    padding: 5px;
  }
}

.course-item {
  height: 100%;
  padding: 8px;
  border-radius: 4px;
  color: white;
  overflow: hidden;

  .course-title {
    font-size: 12px;
    font-weight: bold;
    margin-bottom: 4px;
  }

  .course-teacher,
  .course-info,
  .course-weeks {
    font-size: 10px;
    line-height: 1.5;
  }
}
</style>
