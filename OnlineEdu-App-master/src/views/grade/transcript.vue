<template>
  <div class="app-container">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>我的成绩单</span>
          <el-button type="primary" @click="printTranscript" style="margin-left: 20px">
            <i class="el-icon-printer"></i> 打印成绩单
          </el-button>
        </div>
      </template>
      
      <div class="transcript-container">
        <div class="transcript-header">
          <h2>学生成绩单</h2>
          <div class="student-info">
            <p>姓名：{{ studentInfo.name }}</p>
            <p>学号：{{ studentInfo.studentId }}</p>
            <p>专业：{{ studentInfo.major }}</p>
            <p>院系：{{ studentInfo.department }}</p>
          </div>
        </div>
        
        <el-table :data="gradeList" stripe style="width: 100%" class="transcript-table">
          <el-table-column prop="courseName" label="课程名称" width="200" />
          <el-table-column prop="credit" label="学分" width="80" />
          <el-table-column prop="finalScore" label="最终成绩" width="120">
            <template slot-scope="scope">
              {{ scope.row.finalScore < 60 ? '--' : scope.row.finalScore }}
            </template>
          </el-table-column>
          <el-table-column prop="semester" label="学期" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template slot-scope="scope">
              {{ scope.row.isRetake ? '重修' : '正常' }}
            </template>
          </el-table-column>
        </el-table>
        
        <div class="transcript-footer">
          <div class="summary-item">
            <span>总学分：</span>
            <span>{{ totalCredit }}</span>
          </div>
          <div class="summary-item">
            <span>加权平均成绩：</span>
            <span>{{ weightedAverage.toFixed(2) }}</span>
          </div>
          <div class="summary-item">
            <span>绩点：</span>
            <span>{{ gpa.toFixed(3) }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>

export default {
  name: 'GradeTranscript',
  data() {
    return {
      studentInfo: {
        name: '张三',
        studentId: '20220001',
        major: '计算机科学与技术',
        department: '计算机信息与技术学院'
      },
      gradeList: [
        {
          id: 1,
          courseName: '高等数学',
          credit: 4,
          finalScore: 85,
          semester: '2022-2023上学期',
          isRetake: false
        },
        {
          id: 2,
          courseName: '大学英语',
          credit: 4,
          finalScore: 78,
          semester: '2022-2023上学期',
          isRetake: false
        },
        {
          id: 3,
          courseName: '数据结构',
          credit: 3,
          finalScore: 55,
          semester: '2022-2023上学期',
          isRetake: false
        },
        {
          id: 4,
          courseName: '数据结构',
          credit: 3,
          finalScore: 72,
          semester: '2022-2023下学期',
          isRetake: true
        },
        {
          id: 5,
          courseName: '操作系统',
          credit: 3,
          finalScore: 80,
          semester: '2022-2023下学期',
          isRetake: false
        }
      ]
    }
  },
  computed: {
    totalCredit() {
      return this.gradeList.reduce((sum, item) => sum + item.credit, 0)
    },
    weightedAverage() {
      const passedCourses = this.gradeList.filter(item => item.finalScore >= 60)
      if (passedCourses.length === 0) return 0
      const weightedSum = passedCourses.reduce((sum, item) => sum + item.finalScore * item.credit, 0)
      const totalPassedCredit = passedCourses.reduce((sum, item) => sum + item.credit, 0)
      return weightedSum / totalPassedCredit
    },
    gpa() {
      const weightedAvg = this.weightedAverage
      return Math.max(0, (weightedAvg - 60) / 10 + 1)
    }
  },
  created() {
    // 检查绩点是否低于2.000
    if (this.gpa < 2.0) {
      this.$alert(`请注意，你的绩点为${this.gpa.toFixed(3)}，绩点过低，请自主进行重修报名！`, '绩点预警', {
        confirmButtonText: '确定',
        callback: action => {
          // 关闭弹窗后可以继续操作
        }
      })
    }
  },
  methods: {
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
  
  .transcript-header {
    text-align: center;
    margin-bottom: 30px;
    
    h2 {
      margin-bottom: 20px;
      color: #303133;
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
  }
  
  .transcript-table {
    margin-bottom: 30px;
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
      
      span:first-child {
        font-weight: bold;
        margin-right: 10px;
      }
    }
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
