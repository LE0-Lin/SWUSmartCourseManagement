<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="card-header">
        <span>成绩管理与分析</span>
        <el-tag v-if="currentCourse" type="info">
          平时 {{ currentCourse.usualScoreWeight || 30 }}% / 考试 {{ currentCourse.examScoreWeight || 70 }}%
        </el-tag>
      </div>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-select v-model="currentCourseId" placeholder="选择课程" class="course-select" @change="fetchData">
            <el-option v-for="item in courses" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-col>
      </el-row>

      <el-tabs v-model="activeTab" class="grade-tabs">
        <el-tab-pane label="学生成绩" name="list">
          <el-table :data="grades" v-loading="loading" stripe>
            <el-table-column prop="mobile" label="学号" width="150" />
            <el-table-column prop="nickname" label="姓名" width="140" />
            <el-table-column label="平时分" width="180">
              <template slot-scope="scope">
                <el-input-number
                  v-model="scope.row.usualScore"
                  :precision="2"
                  :step="1"
                  :min="0"
                  :max="100"
                  size="small"
                  @change="updatePreview(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="考试分" width="180">
              <template slot-scope="scope">
                <el-input-number
                  v-model="scope.row.examScore"
                  :precision="2"
                  :step="1"
                  :min="0"
                  :max="100"
                  size="small"
                  @change="updatePreview(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="总评" width="120">
              <template slot-scope="scope">
                <el-tag :type="scoreTag(scope.row.score)" effect="plain">
                  {{ formatScore(scope.row.score) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" min-width="170" />
            <el-table-column label="操作" width="120" fixed="right">
              <template slot-scope="scope">
                <el-button type="primary" size="mini" @click="handleSave(scope.row)">保存</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="成绩分布分析" name="analysis" lazy>
          <div id="gradeChart" class="grade-chart" />
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
  computed: {
    currentCourse() {
      return this.courses.find(item => item.id === this.currentCourseId)
    },
    usualWeight() {
      return Number((this.currentCourse && this.currentCourse.usualScoreWeight) || 30)
    },
    examWeight() {
      return Number((this.currentCourse && this.currentCourse.examScoreWeight) || 70)
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
        this.courses = (resp.data || []).map(item => ({
          ...item,
          usualScoreWeight: Number(item.usualScoreWeight || 30),
          examScoreWeight: Number(item.examScoreWeight || 70)
        }))
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
        this.grades = (resp.data || []).map(row => {
          const grade = {
            ...row,
            usualScore: row.usualScore == null ? null : Number(row.usualScore),
            examScore: row.examScore == null ? null : Number(row.examScore),
            score: row.score == null ? null : Number(row.score)
          }
          if (grade.usualScore != null || grade.examScore != null) {
            this.updatePreview(grade)
          }
          return grade
        })
        this.loading = false
      }).catch(() => { this.loading = false })

      if (this.activeTab === 'analysis') {
        this.fetchDistribution()
      }
    },
    updatePreview(row) {
      const usual = row.usualScore == null ? 0 : Number(row.usualScore)
      const exam = row.examScore == null ? 0 : Number(row.examScore)
      if (row.usualScore == null && row.examScore == null) {
        row.score = null
      } else {
        row.score = Math.round((usual * this.usualWeight + exam * this.examWeight)) / 100
      }
    },
    handleSave(row) {
      this.updatePreview(row)
      const data = {
        courseId: this.currentCourseId,
        memberId: row.memberId,
        usualScore: row.usualScore,
        examScore: row.examScore
      }
      saveGrade(data).then(resp => {
        this.$message.success(resp.message)
        if (this.activeTab === 'analysis') {
          this.fetchDistribution()
        }
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
      if (!chartDom || !this.distributionData) return
      const myChart = echarts.init(chartDom)
      myChart.setOption({
        title: { text: '成绩区间分布', left: 'center' },
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        xAxis: { type: 'category', data: this.distributionData.labels },
        yAxis: { type: 'value', name: '人数' },
        series: [{
          data: this.distributionData.counts,
          type: 'bar',
          showBackground: true,
          backgroundStyle: { color: 'rgba(180, 180, 180, 0.2)' },
          itemStyle: { color: '#3478f6' }
        }]
      })
    },
    formatScore(score) {
      return score == null ? '未评分' : Number(score).toFixed(2)
    },
    scoreTag(score) {
      if (score == null) return 'info'
      if (score < 60) return 'danger'
      if (score < 80) return 'warning'
      return 'success'
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.course-select {
  width: 100%;
}

.grade-tabs {
  margin-top: 20px;
}

.grade-chart {
  width: 100%;
  height: 400px;
  margin-top: 20px;
}
</style>
