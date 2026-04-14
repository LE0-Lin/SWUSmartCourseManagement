<template>
  <el-row type="flex" justify="center">
    <el-col :span="20">
      <v-carousel style="margin-top: 20px" />
      <div class="course-list">课程列表</div>
      <v-course-list :course-list="courseList" style="margin-top: 20px" />
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

<script type="module">
import { getCourses } from '@/api/content'
import { mapGetters } from 'vuex'
export default {
  name: 'Index',
  components: {
    'v-carousel': () => import('@/components/home/carousel'),
    'v-course-list': () => import('@/components/common/course_list')
  },
  computed: {
    ...mapGetters(['user'])
  },
  data() {
    return {
      searchParams: {
        current: 1,
        pageSize: 12
      },
      total: 0,
      courseList: [],
      currentSemester: ''
    }
  },
  created() {
    this.getCurrentSemester()
    this.getList()
  },
  methods: {
    getCurrentSemester() {
      const now = new Date()
      const month = now.getMonth() + 1
      this.currentSemester = (month >= 2 && month <= 7) ? 'autumn' : 'spring'
    },
    getList() {
      // 使用模拟数据实现课程列表
      setTimeout(() => {
        const mockCourses = [
          {
            id: 1,
            title: '高等数学',
            teacher: '张老师',
            subject: '数学',
            semester: this.currentSemester,
            courseType: 'public',
            majors: [],
            cover: 'https://c-ssl.duitang.com/uploads/item/201912/05/20191205152830_ULrYx.thumb.300_0.jpeg',
            description: '高等数学是大学数学的基础课程，主要包括微积分、线性代数等内容。',
            credit: 4,
            duration: '16周',
            status: 'ongoing'
          },
          {
            id: 2,
            title: '大学物理',
            teacher: '李老师',
            subject: '物理',
            semester: this.currentSemester,
            courseType: 'public',
            majors: [],
            cover: 'https://c-ssl.duitang.com/uploads/item/201912/05/20191205152830_ULrYx.thumb.300_0.jpeg',
            description: '大学物理是研究物质运动基本规律的学科，包括力学、热学、电磁学等内容。',
            credit: 4,
            duration: '16周',
            status: 'ongoing'
          },
          {
            id: 3,
            title: '计算机基础',
            teacher: '王老师',
            subject: '计算机',
            semester: this.currentSemester,
            courseType: 'public',
            majors: [],
            cover: 'https://c-ssl.duitang.com/uploads/item/201912/05/20191205152830_ULrYx.thumb.300_0.jpeg',
            description: '计算机基础是介绍计算机基本原理和操作的课程，包括计算机硬件、软件、网络等内容。',
            credit: 3,
            duration: '12周',
            status: 'ongoing'
          },
          {
            id: 4,
            title: '数据结构',
            teacher: '刘老师',
            subject: '计算机',
            semester: this.currentSemester,
            courseType: 'major',
            majors: ['计算机科学与技术', '软件工程', '数据科学与大数据技术'],
            cover: 'https://c-ssl.duitang.com/uploads/item/201912/05/20191205152830_ULrYx.thumb.300_0.jpeg',
            description: '数据结构是计算机科学的核心课程，主要介绍各种数据结构的设计和实现。',
            credit: 4,
            duration: '16周',
            status: 'ongoing'
          },
          {
            id: 5,
            title: '操作系统',
            teacher: '陈老师',
            subject: '计算机',
            semester: this.currentSemester,
            courseType: 'major',
            majors: ['计算机科学与技术', '软件工程'],
            cover: 'https://c-ssl.duitang.com/uploads/item/201912/05/20191205152830_ULrYx.thumb.300_0.jpeg',
            description: '操作系统是管理计算机硬件和软件资源的系统软件，主要包括进程管理、内存管理、文件系统等内容。',
            credit: 4,
            duration: '16周',
            status: 'ongoing'
          },
          {
            id: 6,
            title: '数据库原理',
            teacher: '赵老师',
            subject: '计算机',
            semester: this.currentSemester,
            courseType: 'major',
            majors: ['计算机科学与技术', '软件工程', '数据科学与大数据技术'],
            cover: 'https://c-ssl.duitang.com/uploads/item/201912/05/20191205152830_ULrYx.thumb.300_0.jpeg',
            description: '数据库原理是介绍数据库设计和管理的课程，包括关系数据库、SQL语言、数据库设计等内容。',
            credit: 4,
            duration: '16周',
            status: 'ongoing'
          }
        ]
        
        this.courseList = mockCourses.filter(course => {
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
      }, 500)
    }
  }
}
</script>

<style scoped lang="scss">

.course-list{
  margin-top: 30px;
  font-size: 24px;
  padding-left: 10px;
  border-left: #409eff solid 5px;
}

</style>
