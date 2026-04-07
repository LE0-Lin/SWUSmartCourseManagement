<template>
  <div class="grade-container" v-loading="loading">
    <div v-if="!isBuy" class="not-buy-tip">
      <el-empty description="请先加入学习后查看成绩" />
    </div>
    <div v-else-if="grade && grade.score !== undefined" class="grade-card">
      <el-card shadow="never">
        <div slot="header">我的课程成绩</div>
        <div class="score-display">
          <span class="score-label">最终得分：</span>
          <span class="score-value" :class="getScoreClass(grade.score)">{{ grade.score }}</span>
        </div>
        <div class="score-info">
          更新时间：{{ grade.updateTime || '暂无数据' }}
        </div>
      </el-card>
    </div>
    <div v-else class="no-grade">
      <el-empty description="老师尚未录入成绩，请耐心等待" />
    </div>
  </div>
</template>

<script>
import { getCourseGrade } from '@/api/content'

export default {
  name: 'CourseGrade',
  props: {
    courseId: {
      type: [Number, String],
      required: true
    },
    isBuy: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      grade: null,
      loading: false
    }
  },
  watch: {
    isBuy: {
      immediate: true,
      handler(newVal) {
        if (newVal && this.courseId) {
          this.getGrade(this.courseId)
        }
      }
    }
  },
  methods: {
    getGrade(courseId) {
      this.loading = true
      getCourseGrade(courseId).then(resp => {
        this.grade = resp.data
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    getScoreClass(score) {
      if (score >= 90) return 'excellent'
      if (score >= 60) return 'pass'
      return 'fail'
    }
  }
}
</script>

<style scoped lang="scss">
.grade-container {
  padding: 20px 0;
  min-height: 200px;
}

.score-display {
  text-align: center;
  margin: 30px 0;

  .score-label {
    font-size: 20px;
    color: #606266;
  }

  .score-value {
    font-size: 48px;
    font-weight: bold;

    &.excellent { color: #67c23a; }
    &.pass { color: #409eff; }
    &.fail { color: #f56c6c; }
  }
}

.score-info {
  text-align: right;
  font-size: 13px;
  color: #909399;
}
</style>
