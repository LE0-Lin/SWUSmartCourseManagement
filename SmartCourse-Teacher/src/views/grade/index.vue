<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header">成绩管理与分析</div>
      
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="currentCourseId" placeholder="选择课程" @change="fetchData" style="width: 100%">
            <el-option v-for="item in courses" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-col>
      </el-row>

      <el-tabs v-model="activeTab" style="margin-top: 20px">
        <el-tab-pane label="学生成绩" name="list">
          <el-table :data="grades" v-loading="loading" stripe style="width: 100%">
            <el-table-column prop="memberId" label="学员ID" width="100" />
            <el-table-column label="分数" width="200">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.score" :precision="2" :step="1" :min="0" :max="100" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" />
            <el-table-column label="操作" width="120">
              <template slot-scope="scope">
                <el-button type="primary" size="mini" @click="handleSave(scope.row)">保存</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="成绩分布分析" name="analysis" lazy>
          <div id="gradeChart" style="width: 100%; height: 400px; margin-top: 20px" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import { listAll as listAllCourses } from '@/api/course'
import { getCourseGrades, saveGrade, getGradeDistribution } from '@/api/grade'
import * as echarts from 'echarts'

export default {
  name: 'GradeManagement',
  data() {
    return {
      courses: [],
      currentCourseId: null,
      grades: [],
      loading: false,
      activeTab: 'list',
      distributionData: null
    }
  },
  watch: {
    activeTab(val) {
      if (val === 'analysis' && this.currentCourseId) {
        this.fetchDistribution()
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
          this.fetchData()
        }
      })
    },
    fetchData() {
      if (!this.currentCourseId) return
      this.loading = true
      getCourseGrades(this.currentCourseId).then(resp => {
        this.grades = resp.data
        this.loading = false
      }).catch(() => { this.loading = false })
      
      if (this.activeTab === 'analysis') {
        this.fetchDistribution()
      }
    },
    handleSave(row) {
      const data = {
        courseId: this.currentCourseId,
        memberId: row.memberId,
        score: row.score
      }
      saveGrade(data).then(resp => {
        this.$message.success(resp.message)
      })
    },
    fetchDistribution() {
      if (!this.currentCourseId) return
      getGradeDistribution(this.currentCourseId).then(resp => {
        this.distributionData = resp.data
        this.$nextTick(() => {
          this.initChart()
        })
      })
    },
    initChart() {
      const chartDom = document.getElementById('gradeChart')
      if (!chartDom) return
      const myChart = echarts.init(chartDom)
      const option = {
        title: { text: '成绩区间分布图', left: 'center' },
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        xAxis: { type: 'category', data: this.distributionData.labels },
        yAxis: { type: 'value', name: '人数' },
        series: [{
          data: this.distributionData.counts,
          type: 'bar',
          showBackground: true,
          backgroundStyle: { color: 'rgba(180, 180, 180, 0.2)' },
          itemStyle: { color: '#409EFF' }
        }]
      }
      myChart.setOption(option)
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>
