<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="clearfix">
        <span>课表安排管理</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="handleAdd">添加安排</el-button>
      </div>

      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="6">
          <el-select v-model="currentCourseId" placeholder="选择课程" @change="fetchSchedule" style="width: 100%">
            <el-option v-for="item in courses" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-col>
      </el-row>

      <el-table :data="scheduleData" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="星期" width="120">
          <template slot-scope="scope">
            {{ getDayName(scope.row.dayOfWeek) }}
          </template>
        </el-table-column>
        <el-table-column label="节次" width="180">
          <template slot-scope="scope">
            第 {{ scope.row.sectionStart }} - {{ scope.row.sectionEnd }} 节
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地点" />
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="30%">
      <el-form :model="form" label-width="80px">
        <el-form-item label="星期">
          <el-select v-model="form.dayOfWeek" placeholder="请选择">
            <el-option v-for="i in 7" :key="i" :label="getDayName(i)" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="起始节次">
          <el-input-number v-model="form.sectionStart" :min="1" :max="12" />
        </el-form-item>
        <el-form-item label="结束节次">
          <el-input-number v-model="form.sectionEnd" :min="1" :max="12" />
        </el-form-item>
        <el-form-item label="上课地点">
          <el-input v-model="form.location" placeholder="请输入地点" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listAll as listAllCourses } from '@/api/course'
import { getCourseSchedule, saveSchedule, deleteSchedule } from '@/api/schedule'

export default {
  name: 'ScheduleManagement',
  data() {
    return {
      courses: [],
      currentCourseId: null,
      scheduleData: [],
      loading: false,
      dialogVisible: false,
      dialogTitle: '添加安排',
      form: {
        id: null,
        courseId: null,
        dayOfWeek: 1,
        sectionStart: 1,
        sectionEnd: 2,
        location: ''
      }
    }
  },
  created() {
    this.fetchCourses()
  },
  methods: {
    fetchCourses() {
      listAllCourses().then(resp => {
        this.courses = resp.data
        if (this.courses.length > 0) {
          this.currentCourseId = this.courses[0].id
          this.fetchSchedule()
        }
      })
    },
    fetchSchedule() {
      if (!this.currentCourseId) return
      this.loading = true
      getCourseSchedule(this.currentCourseId).then(resp => {
        this.scheduleData = resp.data
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    getDayName(day) {
      const names = { 1: '星期一', 2: '星期二', 3: '星期三', 4: '星期四', 5: '星期五', 6: '星期六', 7: '星期日' }
      return names[day] || '未知'
    },
    handleAdd() {
      if (!this.currentCourseId) {
        this.$message.warning('请先选择一个课程')
        return
      }
      this.dialogTitle = '添加安排'
      this.form = {
        id: null,
        courseId: this.currentCourseId,
        dayOfWeek: 1,
        sectionStart: 1,
        sectionEnd: 2,
        location: ''
      }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑安排'
      this.form = { ...row }
      this.dialogVisible = true
    },
    handleDelete(id) {
      this.$confirm('确定删除该安排吗?', '提示', { type: 'warning' }).then(() => {
        deleteSchedule(id).then(resp => {
          this.$message.success(resp.message)
          this.fetchSchedule()
        })
      })
    },
    submitForm() {
      if (this.form.sectionStart > this.form.sectionEnd) {
        this.$message.error('起始节次不能大于结束节次')
        return
      }
      saveSchedule(this.form).then(resp => {
        this.$message.success(resp.message)
        this.dialogVisible = false
        this.fetchSchedule()
      })
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>
