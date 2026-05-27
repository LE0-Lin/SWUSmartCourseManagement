<template>
  <div class="smart-admin">
    <div class="hero">
      <div>
        <p>Explainable Decision Support</p>
        <h1>智能毕业风险预警</h1>
        <span>系统自动汇总学生培养方案完成度，识别高风险学生并给出可解释预警依据。</span>
      </div>
      <el-button type="primary" icon="el-icon-refresh" @click="fetchOverview">重新分析</el-button>
    </div>

    <el-row :gutter="16" class="metrics" v-loading="loading">
      <el-col :xs="24" :sm="6">
        <div class="metric">
          <span>学生总数</span>
          <strong>{{ overview.studentCount || 0 }}</strong>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="metric danger">
          <span>高风险</span>
          <strong>{{ overview.highRiskCount || 0 }}</strong>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="metric warning">
          <span>中风险</span>
          <strong>{{ overview.mediumRiskCount || 0 }}</strong>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="metric success">
          <span>低风险</span>
          <strong>{{ overview.lowRiskCount || 0 }}</strong>
        </div>
      </el-col>
    </el-row>

    <el-card class="risk-table" shadow="never">
      <div slot="header" class="table-title">
        <span>学生毕业风险列表</span>
        <el-tag type="info">规则推理 + 学分缺口分析</el-tag>
      </div>
      <el-table :data="riskRows" stripe>
        <el-table-column prop="nickname" label="学生" min-width="120" />
        <el-table-column prop="mobile" label="手机号" min-width="130" />
        <el-table-column label="风险等级" width="120">
          <template slot-scope="{ row }">
            <el-tag :type="riskType(row.riskLevel)" effect="dark">{{ riskText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已通过完成度" width="180">
          <template slot-scope="{ row }">
            <el-progress :percentage="Number(row.completionRate || 0)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="remainingCredits" label="预计分类缺口" width="120" />
        <el-table-column label="预警依据" min-width="280">
          <template slot-scope="{ row }">
            <div class="notes">
              <span v-for="(note, index) in firstNotes(row)" :key="index">{{ note.text }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getSmartOverview } from '@/api/smart'

export default {
  name: 'SmartGraduation',
  data() {
    return {
      loading: false,
      overview: {},
      riskRows: []
    }
  },
  created() {
    this.fetchOverview()
  },
  methods: {
    fetchOverview() {
      this.loading = true
      getSmartOverview().then(resp => {
        this.overview = resp.data || {}
        this.riskRows = this.overview.riskRows || []
      }).finally(() => {
        this.loading = false
      })
    },
    riskType(level) {
      if (level === 'LOW') return 'success'
      if (level === 'HIGH') return 'danger'
      return 'warning'
    },
    riskText(level) {
      if (level === 'LOW') return '低风险'
      if (level === 'HIGH') return '高风险'
      return '中风险'
    },
    firstNotes(row) {
      return (row.advisorNotes || []).slice(0, 2)
    }
  }
}
</script>

<style scoped lang="scss">
.smart-admin {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.hero {
  min-height: 150px;
  background: #173b57;
  color: #fff;
  border-radius: 8px;
  padding: 26px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;

  p {
    margin: 0 0 8px;
    color: #5dd0bd;
    font-weight: 700;
    font-size: 12px;
    text-transform: uppercase;
  }

  h1 {
    margin: 0 0 12px;
    font-size: 30px;
  }

  span {
    color: rgba(255, 255, 255, .78);
  }
}

.metrics {
  margin-top: 16px;
}

.metric {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 18px;

  span {
    color: #7a8794;
  }

  strong {
    display: block;
    margin-top: 10px;
    font-size: 32px;
  }
}

.danger {
  border-top: 4px solid #f56c6c;
}

.warning {
  border-top: 4px solid #e6a23c;
}

.success {
  border-top: 4px solid #67c23a;
}

.risk-table {
  margin-top: 16px;
  border-radius: 8px;
}

.table-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notes {
  display: grid;
  gap: 4px;

  span {
    color: #5f6b76;
    line-height: 1.5;
  }
}
</style>
