<template>
  <div class="app-container">
    <el-steps :active="stepActive" align-center>
      <el-step title="创建课程基本信息" />
      <el-step title="提交审核" />
    </el-steps>
    <div class="step-container">
      <div v-if="stepActive === 1">
        <v-create-info ref="CreateInfo" />
        <div class="step-ctrl">
          <el-button class="btn" type="primary" @click="createCourseDialogVisible = true">提交课程</el-button>
        </div>
      </div>
      <div v-else>
        <div class="success-text">
          课程《{{ courseData.title }}》已提交管理员审核
        </div>
        <div class="step-ctrl">
          <router-link :to="{ name: 'CourseList' }" style="margin-right: 20px">
            <el-button type="primary" style="width: 160px">返回课程列表</el-button>
          </router-link>
          <el-button type="warning" style="width: 160px" @click="reEnter">继续发布课程</el-button>
        </div>
      </div>
    </div>

    <el-dialog
      title="是否提交课程"
      :visible.sync="createCourseDialogVisible"
      destroy-on-close
      width="30vw"
    >
      <div>确认后将直接保存课程基础信息并提交审核，章节、视频和资源目录功能已停用。</div>
      <span slot="footer">
        <el-button type="primary" size="small" @click="createCourse">确认</el-button>
        <el-button size="small" @click="createCourseDialogVisible = false">取消</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { createIt, submitIt } from '@/api/course'

export default {
  name: 'EducourseAdd',
  components: {
    'v-create-info': () => import('@/views/course_add/create_info')
  },
  data() {
    return {
      stepActive: 1,
      courseData: {},
      createCourseDialogVisible: false
    }
  },
  methods: {
    createCourse() {
      const course = this.$refs.CreateInfo.getData()
      createIt(course).then(resp => {
        this.courseData = resp.data || {}
        return submitIt(this.courseData.id)
      }).then(() => {
        this.createCourseDialogVisible = false
        this.stepActive = 2
      })
    },
    reEnter() {
      this.$router.replace({
        name: 'Refresh',
        query: {
          t: Date.now() + ''
        }
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
  margin-top: 36px;
  text-align: center;
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
