<template>
  <div class="app-container">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>我的课表</span>
          <el-select v-model="currentWeek" placeholder="选择周次" style="width: 120px; margin-left: 20px">
            <el-option v-for="week in 16" :key="week" :label="`第${week}周`" :value="week" />
          </el-select>
        </div>
      </template>
      
      <div class="schedule-container">
        <div class="time-axis">
          <div class="time-slot">8:00</div>
          <div class="time-slot">9:40</div>
          <div class="time-slot">10:00</div>
          <div class="time-slot">11:40</div>
          <div class="time-slot">14:00</div>
          <div class="time-slot">15:40</div>
          <div class="time-slot">16:00</div>
          <div class="time-slot">17:40</div>
          <div class="time-slot">19:00</div>
          <div class="time-slot">20:40</div>
        </div>
        <div class="schedule-grid">
          <div class="weekday-header">
            <div class="weekday">周一</div>
            <div class="weekday">周二</div>
            <div class="weekday">周三</div>
            <div class="weekday">周四</div>
            <div class="weekday">周五</div>
            <div class="weekday">周六</div>
            <div class="weekday">周日</div>
          </div>
          <div class="schedule-body">
            <div v-for="(period, periodIndex) in periods" :key="periodIndex" class="period-row">
              <div v-for="(weekday, weekdayIndex) in weekdays" :key="weekdayIndex" class="schedule-cell">
                <div v-for="course in getCourses(weekday, period)" :key="course.id" class="course-item" :style="{ backgroundColor: getCourseColor(course.id) }">
                  <div class="course-title">{{ course.title }}</div>
                  <div class="course-info">{{ course.classroom || '无教室' }}</div>
                  <div class="course-weeks">第{{ course.startWeek }}-{{ course.endWeek }}周</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>

export default {
  name: 'TeacherSchedule',
  data() {
    return {
      currentWeek: 1,
      periods: ['12', '34', '56', '78', '910'],
      weekdays: ['1', '2', '3', '4', '5', '6', '7'],
      courses: [
        {
          id: 1,
          title: '高等数学',
          classroom: 'A101',
          startWeek: 1,
          endWeek: 16,
          classTimes: [
            { weekday: '1', period: '12', startWeek: 1, endWeek: 16 },
            { weekday: '3', period: '34', startWeek: 1, endWeek: 16 }
          ]
        },
        {
          id: 2,
          title: '大学英语',
          classroom: 'B202',
          startWeek: 1,
          endWeek: 16,
          classTimes: [
            { weekday: '2', period: '56', startWeek: 1, endWeek: 16 },
            { weekday: '4', period: '78', startWeek: 1, endWeek: 16 }
          ]
        },
        {
          id: 3,
          title: '数据结构',
          classroom: 'C303',
          startWeek: 1,
          endWeek: 16,
          classTimes: [
            { weekday: '1', period: '56', startWeek: 1, endWeek: 16 },
            { weekday: '3', period: '78', startWeek: 1, endWeek: 16 }
          ]
        }
      ]
    }
  },
  methods: {
    getCourses(weekday, period) {
      return this.courses.filter(course => {
        return course.classTimes.some(time => {
          return time.weekday === weekday && time.period === period && this.currentWeek >= time.startWeek && this.currentWeek <= time.endWeek
        })
      })
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
  width: 60px;
  margin-right: 10px;
  .time-slot {
    height: 60px;
    line-height: 60px;
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
    .schedule-cell {
      flex: 1;
      height: 120px;
      border-right: 1px solid #e4e7ed;
      border-bottom: 1px solid #e4e7ed;
      padding: 5px;
      position: relative;
      &:last-child {
        border-right: none;
      }
    }
  }
}

.course-item {
  height: 100%;
  padding: 5px;
  border-radius: 4px;
  color: white;
  overflow: hidden;
  .course-title {
    font-size: 12px;
    font-weight: bold;
    margin-bottom: 2px;
  }
  .course-info {
    font-size: 10px;
    margin-bottom: 2px;
  }
  .course-weeks {
    font-size: 10px;
  }
}
</style>
