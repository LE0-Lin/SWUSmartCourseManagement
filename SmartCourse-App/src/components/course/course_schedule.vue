<template>
  <div class="schedule-container">
    <div class="schedule-toolbar">
      <span class="week-label">当前周</span>
      <el-select v-model="currentWeek" size="small" style="width: 120px">
        <el-option v-for="week in weekOptions" :key="week" :label="`第${week}周`" :value="week" />
      </el-select>
    </div>
    <el-table :data="visibleScheduleData" stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="dayOfWeek" label="星期" width="120">
        <template slot-scope="scope">
          {{ getDayName(scope.row.dayOfWeek) }}
        </template>
      </el-table-column>
      <el-table-column label="周次" width="140">
        <template slot-scope="scope">
          第{{ scope.row.startWeek || 1 }}-{{ scope.row.endWeek || 21 }}周
        </template>
      </el-table-column>
      <el-table-column label="节次" width="180">
        <template slot-scope="scope">
          第{{ scope.row.sectionStart }} - {{ scope.row.sectionEnd }}节
        </template>
      </el-table-column>
      <el-table-column prop="location" label="地点">
        <template slot-scope="scope">
          <i class="el-icon-location-outline" /> {{ scope.row.location || '待定' }}
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && visibleScheduleData.length === 0" description="当前周暂无课程安排" />
  </div>
</template>

<script>
import { getCourseSchedule } from '@/api/content'

export default {
  name: 'CourseSchedule',
  props: {
    courseId: {
      type: [Number, String],
      required: true
    }
  },
  data() {
    return {
      scheduleData: [],
      loading: false,
      currentWeek: 1,
      weekOptions: Array.from({ length: 21 }, (_, index) => index + 1)
    }
  },
  computed: {
    visibleScheduleData() {
      return this.scheduleData.filter(item => {
        const startWeek = Number(item.startWeek || 1)
        const endWeek = Number(item.endWeek || 21)
        return this.currentWeek >= startWeek && this.currentWeek <= endWeek
      })
    }
  },
  watch: {
    courseId: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.getSchedule(newVal)
        }
      }
    }
  },
  methods: {
    getSchedule(courseId) {
      this.loading = true
      getCourseSchedule(courseId).then(resp => {
        this.scheduleData = resp.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    getDayName(day) {
      const names = {
        1: '星期一',
        2: '星期二',
        3: '星期三',
        4: '星期四',
        5: '星期五',
        6: '星期六',
        7: '星期日'
      }
      return names[day] || '未知'
    }
  }
}
</script>

<style scoped>
.schedule-container {
  padding: 10px 0;
}

.schedule-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.week-label {
  color: #606266;
  font-size: 14px;
}
</style>
