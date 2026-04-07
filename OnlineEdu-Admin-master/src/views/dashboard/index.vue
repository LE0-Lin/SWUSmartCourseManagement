<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :xs="12" :sm="8">
        <!-- 管理员 -->
        <el-col :xs="12" :sm="24" style="padding:0">
          <el-card class="box-card">
            <div slot="header" class="clearfix">
              <span class="cardName">管理员数量</span>
            </div>
            <div class="cardContent clearfix">
              <el-tooltip effect="dark" placement="top">
                <div slot="content">管理员总数量为：{{ stat.adminCount }}</div>
                <span>{{ stat.adminCount }}</span>
              </el-tooltip>
              <el-tooltip effect="dark" placement="top">
                <div slot="content">{{ stat.disabledAdminCount }} 名管理员被禁用</div>
                <span style="float: right;color: #F44336">{{ stat.disabledAdminCount }}</span>
              </el-tooltip>
            </div>
          </el-card>
        </el-col>
        <!-- 讲师 -->
        <el-col :xs="12" :sm="24" style="padding:0">
          <el-card class="box-card">
            <div slot="header" class="clearfix">
              <span class="cardName">讲师数量</span>
            </div>
            <div class="cardContent clearfix">
              <el-tooltip effect="dark" placement="top">
                <div slot="content">讲师总数量为：{{ stat.teacherCount }}</div>
                <span>{{ stat.teacherCount }}</span>
              </el-tooltip>
              <el-tooltip effect="dark" placement="top">
                <div slot="content">{{ stat.disabledTeacherCount }} 名讲师被禁用</div>
                <span style="float: right;color: #F44336">{{ stat.disabledTeacherCount }}</span>
              </el-tooltip>
            </div>
          </el-card>
        </el-col>
        <!-- 视频 -->
        <el-col :xs="12" :sm="24" style="padding:0">
          <el-card class="box-card">
            <div slot="header" class="clearfix">
              <span class="cardName">视频数量</span>
            </div>
            <div class="cardContent clearfix">
              <el-tooltip effect="dark" placement="top">
                <div slot="content">课程视频总数量为：{{ stat.videoCount }}</div>
                <span>{{ stat.videoCount }}</span>
              </el-tooltip>
            </div>
          </el-card>
        </el-col>
      </el-col>
      <!-- 学员 -->
      <el-col :xs="12" :sm="8">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span class="cardName">学员数量</span>
          </div>
          <div class="cardContent clearfix">
            <el-tooltip effect="dark" placement="top">
              <div slot="content">学员总数量为：{{ stat.studentCount }}</div>
              <span>{{ stat.studentCount }}</span>
            </el-tooltip>
            <el-tooltip effect="dark" placement="top">
              <div slot="content">{{ stat.disabledStudentCount }} 名学员被禁用</div>
              <span style="float: right;color: #F44336">{{ stat.disabledStudentCount }}</span>
            </el-tooltip>
          </div>
        </el-card>
      </el-col>
      <!-- 课程 -->
      <el-col :xs="12" :sm="8">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span class="cardName">课程数量</span>
          </div>
          <div class="cardContent clearfix">
            <el-tooltip effect="dark" placement="top">
              <div slot="content">课程总数量为：{{ stat.courseCount }}</div>
              <span>{{ stat.courseCount }}</span>
            </el-tooltip>
            <el-tooltip effect="dark" placement="top">
              <div slot="content">{{ stat.disabledCourseCount }} 门课程已被下架</div>
              <span style="float: right;color: #F44336">{{ stat.disabledCourseCount }}</span>
            </el-tooltip>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>

import { getCommon } from '@/api/stat'

export default {
  name: 'Dashboard',
  data() {
    return {
      stat: {
        adminCount: 0,
        disabledAdminCount: 0,
        teacherCount: 0,
        disabledTeacherCount: 0,
        studentCount: 0,
        disabledStudentCount: 0,
        femaleStudentCount: 0,
        courseCount: 0,
        enabledCourseCount: 0,
        disabledCourseCount: 0,
        editingCourseCount: 0,
        auditingCourseCount: 0,
        rejectedCourseCount: 0,
        videoCount: 0,
        orderCount: 0,
        orderPayByWechatCount: 0,
        orderPayByAlipayCount: 0,
        orderPayByNoneCount: 0,
        maleStudentCount: 0
      }
    }
  },
  created() {
    this.getCommonStat()
  },
  methods: {
    getCommonStat() {
      getCommon().then(resp => {
        this.stat = resp.data
      })
    }
  }
}
</script>

<style lang="scss">
.el-col {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.el-card {
  &__header, &__body {
    padding: 12px 16px;
    line-height: 16px;
  }
}
</style>

<style lang="scss" scoped>
.box-card {
  .cardName {
    color: #97a8be;
  }

  .cardLink {
    float: right;
    padding: 0;
  }

  .cardContent {
    font-size: 26px;
    padding: 12px 0;
  }
}
</style>
