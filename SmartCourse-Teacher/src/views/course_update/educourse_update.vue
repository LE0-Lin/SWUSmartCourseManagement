<template>
  <div class="app-container">
    <div style="font-size: 24px; text-align: center; margin-bottom: 36px">
      <div v-if="stepActive === 1">更新课程信息</div>
      <div v-else>审核中</div>
    </div>
    <div class="step-container">
      <div v-if="stepActive === 1">
        <v-create-info ref="CreateInfo" />
        <div class="step-ctrl">
          <el-button class="btn" type="primary" @click="submitCourseDialogVisible = true">
            提交审核
          </el-button>
        </div>
      </div>
      <div v-else>
        <div class="success-text">
          课程《{{ courseData.title }}》已提交管理员审核
        </div>
        <router-link :to="{ name: 'CourseList' }" class="step-ctrl">
          <el-button class="btn" type="primary">返回课程列表</el-button>
        </router-link>
      </div>
    </div>

    <el-dialog
      title="提示"
      :visible.sync="submitCourseDialogVisible"
      destroy-on-close
      width="30vw"
    >
      <div style="line-height: 24px">
        确认后将仅提交课程基础信息和封面，章节、视频和资源目录功能已停用。
      </div>
      <span slot="footer">
        <el-button type="primary" size="small" @click="submit">确认</el-button>
        <el-button size="small" @click="submitCourseDialogVisible = false">取消</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="警告"
      :visible.sync="warningDialogVisible"
      destroy-on-close
      width="30vw"
      @closed="$router.push({ name: 'CourseList' })"
    >
      <div style="font-size: 16px; line-height: 32px">
        <div>课程《{{ courseData.title }}》正在<span style="color: #21BA45">审核中</span></div>
        <div style="color: #F44336">请不要重复修改</div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getInfo, updateIt } from '@/api/course'

export default {
  name: 'EducourseUpdate',
  components: {
    'v-create-info': () => import('@/views/course_update/create_info')
  },
  data() {
    return {
      stepActive: 1,
      courseData: {},
      updateCourseData: {},
      submitCourseDialogVisible: false,
      warningDialogVisible: false
    }
  },
  watch: {
    $route() {
      this.getCourseInfo(this.$route.params.id)
    }
  },
  created() {
    this.getCourseInfo(this.$route.params.id)
  },
  methods: {
    getCourseInfo(id) {
      getInfo(id).then(resp => {
        this.courseData = { ...(resp.data || {}) }
        if (this.courseData.status && this.courseData.status.indexOf('AUDITING') !== -1) {
          this.warningDialogVisible = true
        }
        setTimeout(() => {
          if (this.$refs.CreateInfo) {
            this.$refs.CreateInfo.setData(this.courseData)
          }
        }, 100)
      })
    },
    submit() {
      this.updateCourseData = this.$refs.CreateInfo.getData()
      updateIt(this.updateCourseData).then(resp => {
        this.submitCourseDialogVisible = false
        this.$message.success(resp.message)
        this.stepActive = 2
      })
    }
  }
}
</script>

<style scoped lang="scss">
.step-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;

  > div {
    width: 70%;
  }
}

.step-ctrl {
  margin-top: 64px;
  text-align: center;
  display: block;
}

.btn {
  width: 300px;
}

.success-text {
  font-size: 20px;
  margin: 16vh 0 10vh 0;
  text-align: center;
}
</style>
