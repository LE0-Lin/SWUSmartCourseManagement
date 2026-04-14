<template>
  <el-row type="flex" justify="center">
    <el-col :span="20">
      <el-breadcrumb separator-class="el-icon-arrow-right" style="margin: 30px 0;color: #333">
        <el-breadcrumb-item>全部课程</el-breadcrumb-item>
        <el-breadcrumb-item v-for="(item,index) in subjectParent" :key="index">
          {{ item }}
        </el-breadcrumb-item>
      </el-breadcrumb>
      <v-course-list :course-list="courseList" style="margin-top: 40px" />
      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="total"
          :page-size.sync="searchParams.pageSize"
          :current-page.sync="searchParams.current"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </el-col>
  </el-row>
</template>

<script>
import { getCourses, getSubjectParent } from '@/api/content'
import { mapGetters } from 'vuex'

export default {
  name: 'SearchSubject',
  components: {
    'v-course-list': () => import('@/components/common/course_list')
  },
  computed: {
    ...mapGetters(['user'])
  },
  data() {
    return {
      searchParams: {
        current: 1,
        pageSize: 12,
        subjectId: null
      },
      total: 0,
      courseList: [],
      subjectParent: [],
      currentSemester: ''
    }
  },
  watch: {
    $route(to, from) {
      const subjectId = this.$route.params.subject
      this.searchParams.subjectId = subjectId
      this.searchParams.current = 1
      this.getSubjectParent(subjectId)
      this.getList()
    }
  },
  created() {
    this.getCurrentSemester()
    const subjectId = this.$route.params.subject
    this.searchParams.subjectId = subjectId
    this.getSubjectParent(subjectId)
    this.getList()
  },
  methods: {
    getCurrentSemester() {
      const now = new Date()
      const month = now.getMonth() + 1
      this.currentSemester = (month >= 2 && month <= 7) ? 'autumn' : 'spring'
    },
    getList() {
      getCourses(this.searchParams).then(resp => {
        this.courseList = resp.data.list.filter(course => {
          // 首先过滤当前学期的课程
          if (course.semester !== this.currentSemester) {
            return false
          }
          // 公共课对所有学生可见
          if (course.courseType === 'public') {
            return true
          }
          // 专业课需要检查专业限制
          if (course.courseType === 'major' && course.majors && course.majors.length > 0) {
            return this.user && this.user.major && course.majors.includes(this.user.major)
          }
          return false
        })
        this.total = this.courseList.length
      })
    },
    getSubjectParent(id) {
      getSubjectParent(id).then(resp => {
        let subject = []
        let parent = resp.data
        while (parent) {
          subject = [parent.title, ...subject]
          parent = parent.parent
        }
        this.subjectParent = subject
      })
    }
  }
}
</script>

<style lang="scss">

</style>
