<template>
  <div class="app-container">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>课程排课</span>
          <div>
            <el-button size="small" type="primary" @click="getList">刷新</el-button>
            <el-button size="small" type="success" :loading="autoScheduling" @click="autoSchedule">
              自动补全空课表
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchParams" size="small" class="filter-form">
        <el-form-item label="课程名称">
          <el-input v-model="searchParams.title" placeholder="输入课程名称" clearable @keyup.enter.native="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="courseList" stripe style="width: 100%">
        <el-table-column prop="title" label="课程名称" min-width="220" />
        <el-table-column prop="teacherName" label="讲师" width="160" />
        <el-table-column label="课表" min-width="420">
          <template slot-scope="scope">
            <div v-if="scope.row.classTimes.length">
              <div
                v-for="item in scope.row.classTimes"
                :key="item.id || `${item.dayOfWeek}-${item.sectionStart}-${item.sectionEnd}-${item.startWeek}-${item.endWeek}`"
                class="schedule-line"
              >
                {{ formatSchedule(item) }}
              </div>
            </div>
            <span v-else class="empty-text">暂未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="scope">
            <el-button size="small" type="primary" @click="editSchedule(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" :disabled="!scope.row.classTimes.length" @click="clearSchedule(scope.row)">
              清空
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="total"
          :page-size.sync="searchParams.pageSize"
          :current-page.sync="searchParams.current"
          @current-change="getList"
        />
      </div>
    </el-card>

    <el-dialog title="编辑排课" :visible.sync="scheduleDialogVisible" width="860px">
      <div class="dialog-header">
        <div class="dialog-course">{{ currentCourse.title || '-' }}</div>
        <div class="dialog-teacher">{{ currentCourse.teacherName || '-' }}</div>
      </div>
      <div v-for="(item, index) in currentCourse.classTimes" :key="item.id || index" class="class-time-item">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-select v-model="item.dayOfWeek" placeholder="星期" style="width: 100%">
              <el-option v-for="day in weekdays" :key="day.value" :label="day.label" :value="day.value" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-select v-model="item.periodKey" placeholder="节次" style="width: 100%" @change="syncPeriod(item)">
              <el-option v-for="period in periods" :key="period.key" :label="period.label" :value="period.key" />
            </el-select>
          </el-col>
          <el-col :span="5">
            <el-select v-model="item.startWeek" placeholder="开始周" style="width: 100%">
              <el-option v-for="week in weekOptions" :key="`start-${index}-${week}`" :label="`第${week}周`" :value="week" />
            </el-select>
          </el-col>
          <el-col :span="5">
            <el-select v-model="item.endWeek" placeholder="结束周" style="width: 100%">
              <el-option v-for="week in weekOptions" :key="`end-${index}-${week}`" :label="`第${week}周`" :value="week" />
            </el-select>
          </el-col>
        </el-row>
        <el-row :gutter="12" style="margin-top: 12px">
          <el-col :span="20">
            <el-input v-model="item.location" placeholder="上课地点，例如 A101" />
          </el-col>
          <el-col :span="4">
            <el-button type="danger" icon="el-icon-delete" style="width: 100%" @click="removeClassTime(index)">删除</el-button>
          </el-col>
        </el-row>
      </div>
      <el-empty v-if="!currentCourse.classTimes.length" description="暂无排课" />
      <div class="add-row">
        <el-button type="primary" plain icon="el-icon-plus" @click="addClassTime">新增时间</el-button>
      </div>
      <span slot="footer">
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCurrentSchedule">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { list as listCourses } from '@/api/course'
import { deleteSchedule, getCourseSchedule, saveSchedule } from '@/api/schedule'

export default {
  name: 'EduScheduleList',
  data() {
    return {
      loading: false,
      saving: false,
      autoScheduling: false,
      total: 0,
      searchParams: {
        current: 1,
        pageSize: 10,
        title: ''
      },
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
        { key: '12', label: '1-2节 (8:00-9:40)', start: 1, end: 2 },
        { key: '34', label: '3-4节 (10:00-11:40)', start: 3, end: 4 },
        { key: '56', label: '5-6节 (14:00-15:40)', start: 5, end: 6 },
        { key: '78', label: '7-8节 (16:00-17:40)', start: 7, end: 8 },
        { key: '910', label: '9-10节 (19:00-20:40)', start: 9, end: 10 }
      ],
      weekOptions: Array.from({ length: 21 }, (_, index) => index + 1),
      courseList: [],
      scheduleDialogVisible: false,
      currentCourse: {
        id: null,
        title: '',
        teacherName: '',
        classTimes: []
      },
      originalScheduleIds: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const resp = await listCourses({
          current: this.searchParams.current,
          pageSize: this.searchParams.pageSize,
          title: this.searchParams.title
        })
        const page = resp.data || {}
        const rawCourses = page.list || []
        const realCourses = rawCourses.filter(course => course && Number(course.status) !== 0)
        const schedules = await Promise.all(realCourses.map(course => getCourseSchedule(course.id)))
        this.courseList = realCourses.map((course, index) => ({
          ...course,
          classTimes: this.normalizeSchedules(schedules[index].data || [])
        }))
        this.total = page.total || this.courseList.length
      } finally {
        this.loading = false
      }
    },
    search() {
      this.searchParams.current = 1
      this.getList()
    },
    resetSearch() {
      this.searchParams.title = ''
      this.search()
    },
    normalizeSchedules(list) {
      return (list || []).map(item => ({
        id: item.id,
        courseId: item.courseId,
        dayOfWeek: Number(item.dayOfWeek),
        sectionStart: Number(item.sectionStart),
        sectionEnd: Number(item.sectionEnd),
        startWeek: Number(item.startWeek || 1),
        endWeek: Number(item.endWeek || 21),
        periodKey: this.toPeriodKey(item.sectionStart, item.sectionEnd),
        location: item.location || ''
      }))
    },
    toPeriodKey(start, end) {
      const found = this.periods.find(period => period.start === Number(start) && period.end === Number(end))
      return found ? found.key : ''
    },
    formatSchedule(item) {
      const weekday = this.weekdays.find(day => day.value === Number(item.dayOfWeek))
      const period = this.periods.find(p => p.start === Number(item.sectionStart) && p.end === Number(item.sectionEnd))
      const location = item.location || '待定'
      const periodText = period ? period.label : `${item.sectionStart}-${item.sectionEnd}节`
      return `${weekday ? weekday.label : '未知'} ${periodText} 第${item.startWeek || 1}-${item.endWeek || 21}周 ${location}`
    },
    editSchedule(course) {
      this.currentCourse = {
        id: course.id,
        title: course.title,
        teacherName: course.teacherName,
        classTimes: this.normalizeSchedules(course.classTimes || [])
      }
      this.originalScheduleIds = this.currentCourse.classTimes.map(item => item.id).filter(Boolean)
      this.scheduleDialogVisible = true
    },
    addClassTime() {
      const period = this.periods[0]
      this.currentCourse.classTimes.push({
        courseId: this.currentCourse.id,
        dayOfWeek: 1,
        sectionStart: period.start,
        sectionEnd: period.end,
        startWeek: 1,
        endWeek: 21,
        periodKey: period.key,
        location: ''
      })
    },
    removeClassTime(index) {
      this.currentCourse.classTimes.splice(index, 1)
    },
    syncPeriod(item) {
      const period = this.periods.find(p => p.key === item.periodKey)
      if (!period) {
        return
      }
      item.sectionStart = period.start
      item.sectionEnd = period.end
    },
    async saveCurrentSchedule() {
      if (!this.currentCourse.id) {
        return
      }
      this.saving = true
      try {
        const currentIds = this.currentCourse.classTimes.map(item => item.id).filter(Boolean)
        const removedIds = this.originalScheduleIds.filter(id => !currentIds.includes(id))
        for (const id of removedIds) {
          await deleteSchedule(id)
        }
        for (const item of this.currentCourse.classTimes) {
          if (!item.dayOfWeek || !item.sectionStart || !item.sectionEnd) {
            continue
          }
          await saveSchedule({
            id: item.id,
            courseId: this.currentCourse.id,
            dayOfWeek: Number(item.dayOfWeek),
            sectionStart: Number(item.sectionStart),
            sectionEnd: Number(item.sectionEnd),
            startWeek: Number(item.startWeek || 1),
            endWeek: Number(item.endWeek || 21),
            location: item.location || ''
          })
        }
        this.$message.success('排课已保存')
        this.scheduleDialogVisible = false
        await this.getList()
      } finally {
        this.saving = false
      }
    },
    async clearSchedule(course) {
      await this.$confirm(`确定清空课程“${course.title}”的所有上课时间吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      for (const item of course.classTimes) {
        if (item.id) {
          await deleteSchedule(item.id)
        }
      }
      this.$message.success('已清空课表')
      await this.getList()
    },
    async autoSchedule() {
      const emptyCourses = this.courseList.filter(course => !course.classTimes.length)
      if (!emptyCourses.length) {
        this.$message.info('当前课程都有课表，无需补全')
        return
      }
      this.autoScheduling = true
      try {
        for (let index = 0; index < emptyCourses.length; index++) {
          const course = emptyCourses[index]
          const weekday = (index % 5) + 1
          const period = this.periods[index % this.periods.length]
          await saveSchedule({
            courseId: course.id,
            dayOfWeek: weekday,
            sectionStart: period.start,
            sectionEnd: period.end,
            startWeek: 1,
            endWeek: 16,
            location: `自动排课教室${index + 1}`
          })
        }
        this.$message.success('自动排课完成')
        await this.getList()
      } finally {
        this.autoScheduling = false
      }
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

.filter-form {
  margin-bottom: 16px;
}

.schedule-line {
  line-height: 1.8;
}

.empty-text {
  color: #909399;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  font-weight: 600;
}

.class-time-item {
  margin-bottom: 12px;
  padding: 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.add-row {
  margin-top: 12px;
}
</style>
