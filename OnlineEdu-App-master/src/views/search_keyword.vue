<template>
  <el-row type="flex" justify="center">
    <el-col :span="20">
      <div style="text-align: center;margin-top: 40px" class="search-box">
        <el-input v-model="searchParams.title" style="width: 280px;margin-right: 10px" @keyup.enter.native="search" />
        <el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button>
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
import { getCourses } from '@/api/content'
import { mapGetters } from 'vuex'

export default {
  name: 'SearchKeyword',
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
        title: ''
      },
      total: 0,
      courseList: [],
      currentSemester: ''
    }
  },
  watch: {
    $route(to, from) {
      this.searchParams.title = this.$route.params.title
      this.searchParams.current = 1
      this.getList()
    }
  },
  created() {
    this.getCurrentSemester()
    this.searchParams.title = this.$route.params.title
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
    search() {
      if (this.$route.params.title === this.searchParams.title) return
      this.$router.push({
        name: 'SearchByKeyword',
        params: { title: this.searchParams.title }
      })
    }
  }
}
</script>

<style lang="scss">
.search-box {
  .el-input__inner {
    border-width: 2px;
    border-color: #ccd0d7;

    &:focus {
      border-color: #409EFF;
      outline: 0;
    }
  }
}
</style>
