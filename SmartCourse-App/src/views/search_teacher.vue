<template>
  <el-row type="flex" justify="center">
    <el-col :span="20">
      <div class="tinfo clearfix">
        <el-avatar fit="contain" :src="teacher.avatar" :size="70" style="margin-right: 12px" />
        <div class="intro">
          <div class="name">{{ teacher.name }}</div>
          {{ teacher.intro }}
        </div>
      </div>
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
import { getCourses, getTeacher } from '@/api/content'
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
        teacherId: null

      },
      total: 0,
      courseList: [],
      teacher: {},
      currentSemester: ''
    }
  },
  watch: {
    $route(to, from) {
      const teacher = this.$route.params.teacher
      this.searchParams.teacherId = teacher
      this.searchParams.current = 1
      this.getList()
      this.getTeacher(teacher)
    }
  },
  created() {
    this.getCurrentSemester()
    const teacher = this.$route.params.teacher
    this.searchParams.teacherId = teacher
    this.getList()
    this.getTeacher(teacher)
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
    getTeacher(id) {
      getTeacher(id).then(resp => {
        this.teacher = resp.data
      })
    }
  }
}
</script>

<style scoped lang="scss">

.tinfo {
  margin-top: 30px;
  position: relative;

  .intro {
    width: calc(100% - 90px);
    line-height: 24px;
    font-size: 14px;
    color: #666;
    float: left;
    margin: -60px 0 0 90px;

    .name {
      font-size: 18px;
      color: #20a0ff;
    }
  }
}
</style>
