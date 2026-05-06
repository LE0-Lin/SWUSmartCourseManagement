<template>
  <div class="advisor-page">
    <section class="advisor-hero">
      <div>
        <p class="eyebrow">{{ dashboard.engineName || '培养方案数字孪生' }}</p>
        <h1>可解释选课推理引擎</h1>
        <p class="subline">
          系统把当前选课、成绩和课表映射为毕业路径模型，模拟“如果现在补选这些课程，是否能降低毕业风险”。
        </p>
      </div>
      <div class="risk-panel" :class="riskClass">
        <span>毕业风险</span>
        <strong>{{ summary.riskText || '--' }}</strong>
        <em>完成度 {{ summary.completionRate || 0 }}%</em>
      </div>
    </section>

    <section v-loading="loading" class="dashboard-grid">
      <div class="metric">
        <span>毕业要求</span>
        <strong>{{ formatCredit(summary.requiredCredits) }}</strong>
        <em>总学分</em>
      </div>
      <div class="metric">
        <span>已通过</span>
        <strong>{{ formatCredit(summary.completedCredits) }}</strong>
        <em>成绩合格学分</em>
      </div>
      <div class="metric">
        <span>本学期预计</span>
        <strong>{{ formatCredit(summary.projectedCredits) }}</strong>
        <em>含已选未录成绩</em>
      </div>
      <div class="metric">
        <span>预计缺口</span>
        <strong>{{ formatCredit(summary.projectedRemainingCredits) }}</strong>
        <em>仍需补齐</em>
      </div>
    </section>

    <section class="section-band">
      <div class="section-title">
        <div>
          <p class="eyebrow">Training Plan</p>
          <h2>培养方案完成度</h2>
        </div>
        <el-tag :type="riskTagType" effect="dark">{{ summary.riskText || '分析中' }}</el-tag>
      </div>
      <div class="requirement-grid">
        <div v-for="item in requirements" :key="item.typeCode" class="requirement">
          <div class="requirement-top">
            <span>{{ item.typeLabel }}</span>
            <strong>{{ formatCredit(item.completedCredits) }} / {{ formatCredit(item.requiredCredits) }}</strong>
          </div>
          <el-progress :percentage="Number(item.projectedProgress || 0)" :stroke-width="10" :color="progressColor(item)" />
          <div class="requirement-foot">
            <span>已选 {{ formatCredit(item.selectedCredits) }}</span>
            <span>预计缺 {{ formatCredit(item.remainingAfterCurrent) }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="split-layout">
      <div class="section-band">
        <div class="section-title compact">
          <div>
            <p class="eyebrow">What-if Simulation</p>
            <h2>一键补齐方案</h2>
          </div>
        </div>
        <div v-if="pathPlan.length" class="path-list">
          <div v-for="(course, index) in pathPlan" :key="course.courseId" class="path-item">
            <div class="step">{{ index + 1 }}</div>
            <div>
              <strong>{{ course.title }}</strong>
              <p>{{ course.reason }}</p>
              <span>{{ course.scheduleText }}</span>
            </div>
            <el-tag size="mini">{{ course.typeLabel }}</el-tag>
          </div>
        </div>
        <el-empty v-else description="当前暂无需要补齐的课程路径" />
      </div>

      <div class="section-band">
        <div class="section-title compact">
          <div>
            <p class="eyebrow">Explainable AI</p>
            <h2>推理依据</h2>
          </div>
        </div>
        <div class="note-list">
          <el-alert
            v-for="(note, index) in advisorNotes"
            :key="index"
            :type="note.level"
            :title="note.text"
            :closable="false"
            show-icon
          />
        </div>
      </div>
    </section>

    <section class="section-band">
      <div class="section-title">
        <div>
          <p class="eyebrow">Recommendations</p>
          <h2>智能推荐课程</h2>
        </div>
        <el-button icon="el-icon-refresh" size="small" @click="fetchDashboard">重新分析</el-button>
      </div>
      <div class="recommend-grid">
        <div
          v-for="course in recommendations"
          :key="course.courseId"
          class="recommend-card"
          :class="{ 'is-conflict': course.conflict }"
        >
          <div class="recommend-head">
            <el-tag size="mini" effect="dark">{{ course.typeLabel }}</el-tag>
            <el-tag v-if="course.conflict" size="mini" type="danger" effect="dark">课表冲突</el-tag>
            <el-tag v-else size="mini" type="success">可选</el-tag>
            <span>{{ formatCredit(course.credit) }} 学分</span>
          </div>
          <h3>{{ course.title }}</h3>
          <p>{{ course.reason }}</p>
          <div v-if="course.conflict" class="conflict-text">{{ course.conflictText }}</div>
          <div class="schedule">{{ course.scheduleText }}</div>
          <div class="fit">
            <span>匹配度</span>
            <el-progress :percentage="Number(course.fitScore || 0)" :stroke-width="8" :color="course.conflict ? '#d9480f' : '#2f9e8f'" />
          </div>
          <el-button
            :type="course.conflict ? 'danger' : 'primary'"
            size="small"
            plain
            :disabled="course.conflict"
            @click="diagnose(course.courseId)"
          >{{ course.conflict ? '存在冲突' : '选课诊断' }}</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && recommendations.length === 0" description="暂无推荐课程" />
    </section>
  </div>
</template>

<script>
import { getSmartAdvisorDashboard, diagnoseCourse } from '@/api/content'

export default {
  name: 'SmartAdvisor',
  data() {
    return {
      loading: false,
      dashboard: {},
      summary: {},
      requirements: [],
      recommendations: [],
      pathPlan: [],
      advisorNotes: []
    }
  },
  computed: {
    riskClass() {
      return `risk-${(this.summary.riskLevel || 'MEDIUM').toLowerCase()}`
    },
    riskTagType() {
      const level = this.summary.riskLevel
      if (level === 'LOW') return 'success'
      if (level === 'HIGH') return 'danger'
      return 'warning'
    }
  },
  created() {
    this.fetchDashboard()
  },
  methods: {
    fetchDashboard() {
      this.loading = true
      getSmartAdvisorDashboard().then(resp => {
        const data = resp.data || {}
        this.dashboard = data
        this.summary = data.summary || {}
        this.requirements = data.requirements || []
        this.recommendations = data.recommendations || []
        this.pathPlan = data.pathPlan || []
        this.advisorNotes = data.advisorNotes || []
      }).finally(() => {
        this.loading = false
      })
    },
    diagnose(courseId) {
      diagnoseCourse(courseId).then(resp => {
        const data = resp.data || {}
        this.$alert(data.message || '该课程可以选择', '智能选课诊断', {
          confirmButtonText: '知道了',
          type: data.selectable ? 'success' : 'warning'
        })
      })
    },
    formatCredit(value) {
      const number = Number(value || 0)
      return Number.isInteger(number) ? number : number.toFixed(1)
    },
    progressColor(item) {
      if (Number(item.remainingAfterCurrent || 0) <= 0) return '#2f9e8f'
      if (String(item.typeCode).indexOf('REQUIRED') > -1) return '#d9480f'
      return '#f59f00'
    }
  }
}
</script>

<style scoped lang="scss">
.advisor-page {
  background: #f4f7f9;
  min-height: 100%;
  padding-bottom: 36px;
  color: #1f2d3d;
}

.advisor-hero {
  min-height: 240px;
  padding: 44px 10%;
  background: #173b57;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;

  h1 {
    margin: 8px 0 12px;
    font-size: 40px;
    font-weight: 700;
    letter-spacing: 0;
  }

  .subline {
    max-width: 680px;
    margin: 0;
    line-height: 1.8;
    color: rgba(255, 255, 255, .78);
  }
}

.eyebrow {
  margin: 0;
  color: #2f9e8f;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.risk-panel {
  width: 230px;
  min-height: 132px;
  border: 1px solid rgba(255, 255, 255, .22);
  border-radius: 8px;
  padding: 20px;
  background: rgba(255, 255, 255, .08);
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  span,
  em {
    font-style: normal;
    color: rgba(255, 255, 255, .72);
  }

  strong {
    font-size: 24px;
  }
}

.risk-low {
  border-color: rgba(47, 158, 143, .8);
}

.risk-medium {
  border-color: rgba(245, 159, 0, .85);
}

.risk-high {
  border-color: rgba(217, 72, 15, .85);
}

.dashboard-grid,
.section-band,
.split-layout {
  width: 80%;
  max-width: 1180px;
  margin: 22px auto 0;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric,
.section-band {
  background: #fff;
  border: 1px solid #e4eaf0;
  border-radius: 8px;
}

.metric {
  padding: 18px;

  span,
  em {
    display: block;
    color: #7a8794;
    font-style: normal;
  }

  strong {
    display: block;
    margin: 10px 0 4px;
    font-size: 30px;
  }
}

.section-band {
  padding: 22px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  h2 {
    margin: 4px 0 0;
    font-size: 22px;
  }
}

.compact {
  margin-bottom: 14px;
}

.requirement-grid,
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.requirement {
  border: 1px solid #e7edf2;
  border-radius: 8px;
  padding: 16px;
}

.requirement-top,
.requirement-foot,
.recommend-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.recommend-head {
  flex-wrap: wrap;
  align-items: center;
}

.requirement-top {
  margin-bottom: 12px;
}

.requirement-foot {
  margin-top: 10px;
  color: #7a8794;
  font-size: 12px;
}

.split-layout {
  display: grid;
  grid-template-columns: 1.2fr .8fr;
  gap: 18px;
}

.path-list,
.note-list {
  display: grid;
  gap: 12px;
}

.path-item {
  display: grid;
  grid-template-columns: 34px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid #e7edf2;
  border-radius: 8px;

  .step {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: #173b57;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
  }

  p {
    margin: 4px 0;
    color: #5f6b76;
  }

  span {
    color: #87919b;
    font-size: 12px;
  }
}

.recommend-card {
  border: 1px solid #e7edf2;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  h3 {
    min-height: 44px;
    margin: 0;
    font-size: 18px;
    line-height: 1.35;
  }

  p {
    min-height: 42px;
    margin: 0;
    color: #5f6b76;
    line-height: 1.5;
  }
}

.recommend-card.is-conflict {
  border-color: #f1b0a0;
  background: #fff7f5;

  h3 {
    color: #9f2d12;
    text-decoration: line-through;
    text-decoration-thickness: 2px;
  }
}

.conflict-text {
  padding: 8px 10px;
  border-radius: 6px;
  background: #ffe8e0;
  color: #9f2d12;
  font-size: 12px;
  line-height: 1.5;
}

.schedule {
  min-height: 38px;
  color: #7a8794;
  font-size: 12px;
  line-height: 1.5;
}

.fit {
  span {
    display: block;
    margin-bottom: 6px;
    color: #7a8794;
    font-size: 12px;
  }
}

@media (max-width: 960px) {
  .advisor-hero,
  .dashboard-grid,
  .section-band,
  .split-layout {
    width: 92%;
  }

  .advisor-hero,
  .split-layout {
    grid-template-columns: 1fr;
    display: grid;
  }

  .dashboard-grid,
  .requirement-grid,
  .recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .advisor-hero {
    padding: 30px 4%;

    h1 {
      font-size: 30px;
    }
  }

  .risk-panel {
    width: auto;
  }

  .dashboard-grid,
  .requirement-grid,
  .recommend-grid {
    grid-template-columns: 1fr;
  }
}
</style>
