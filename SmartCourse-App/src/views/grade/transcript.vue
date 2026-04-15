<template>
  <div class="app-container">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>Transcript</span>
          <el-button style="margin-left: 20px" type="primary" @click="printTranscript">
            <i class="el-icon-printer" /> Print
          </el-button>
        </div>
      </template>

      <div v-loading="loading" class="transcript-container">
        <div class="transcript-header">
          <h2>Student Transcript</h2>
          <div class="student-info">
            <p>Name: {{ user.nickname || '-' }}</p>
            <p>Mobile: {{ user.mobile || '-' }}</p>
            <p>Email: {{ user.email || '-' }}</p>
          </div>
        </div>

        <el-table :data="gradeList" stripe style="width: 100%" class="transcript-table">
          <el-table-column prop="courseName" label="Course" />
          <el-table-column label="Score" width="120">
            <template slot-scope="scope">
              {{ scope.row.score === null || scope.row.score === undefined ? '--' : scope.row.score }}
            </template>
          </el-table-column>
          <el-table-column label="Status" width="120">
            <template slot-scope="scope">
              {{ scope.row.status === 'graded' ? 'Graded' : 'Pending' }}
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="Updated At" />
        </el-table>

        <div class="transcript-footer">
          <div class="summary-item">
            <span>Courses:</span>
            <span>{{ gradeList.length }}</span>
          </div>
          <div class="summary-item">
            <span>Graded:</span>
            <span>{{ gradedCount }}</span>
          </div>
          <div class="summary-item">
            <span>Average:</span>
            <span>{{ averageScore }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getTranscript } from '@/api/content'

export default {
  name: 'GradeTranscript',
  data() {
    return {
      loading: false,
      gradeList: []
    }
  },
  computed: {
    ...mapGetters(['user']),
    gradedCount() {
      return this.gradeList.filter(item => item.score !== null && item.score !== undefined).length
    },
    averageScore() {
      const graded = this.gradeList.filter(item => item.score !== null && item.score !== undefined)
      if (graded.length === 0) {
        return '--'
      }
      const total = graded.reduce((sum, item) => sum + item.score, 0)
      return (total / graded.length).toFixed(2)
    }
  },
  created() {
    this.fetchTranscript()
  },
  methods: {
    fetchTranscript() {
      this.loading = true
      getTranscript().then(resp => {
        this.gradeList = resp.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    printTranscript() {
      window.print()
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

.transcript-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.transcript-header {
  text-align: center;
  margin-bottom: 30px;

  h2 {
    margin-bottom: 20px;
    color: #303133;
  }
}

.student-info {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;

  p {
    margin: 0;
    color: #606266;
  }
}

.transcript-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;

  .summary-item {
    margin-left: 30px;
    font-size: 16px;
  }
}

@media print {
  .app-container {
    padding: 0;
  }

  .el-card {
    box-shadow: none;
  }

  .card-header {
    display: none;
  }

  .transcript-container {
    box-shadow: none;
  }
}
</style>
