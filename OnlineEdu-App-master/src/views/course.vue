<template>
  <el-row type="flex" justify="center">
    <el-col :span="20">
      <!-- 面包屑 -->
      <el-breadcrumb separator-class="el-icon-arrow-right" style="margin: 20px 0;color: #333">
        <el-breadcrumb-item>全部课程</el-breadcrumb-item>
        <el-breadcrumb-item v-for="(item,index) in getDetailsSubject(course.subjectParent)" :key="index">
          {{ item }}
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ course.title }}</el-breadcrumb-item>
      </el-breadcrumb>
      <!-- 课程资源展示区 (替代播放器) -->
      <div style="position: relative;width: 100%">
        <v-course-resource ref="CourseResource" :teacher="teacher" />
        <!-- 课程选课栏 -->
        <div v-if="showSelectBanner" class="sub-course">
          <div>
            <div style="color: #fff;font-size: 20px;">
              {{ course.title }}
            </div>
            <div style="color: #999;font-size: 13px;margin-top: 5px">
              <span style="margin-right: 26px">{{ course.lessonNum }} 总课时</span>
              <span style="margin-right: 26px">{{ course.credit }} 学分</span>
              <span style="margin-right: 26px">{{ course.buyCount }} 人选修过</span>
              <span>{{ course.viewCount }} 人查看过</span>
            </div>
          </div>
          <div style="margin-left: auto">
            <div style="width: 22vw;text-align: center">
              <el-button
                type="primary"
                style="width: 18vw;font-size: 18px"
                icon="el-icon-plus"
                @click="openSelectCourseDialog"
              >
                选择课程
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 课程简介、评价、课表、成绩 -->
      <div class="clearfix" style="margin-top: 20px">
        <div style="width: 73%;background-color: #fff;padding: 10px 20px;float: left;">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="课程概述" name="desc" class="course-descriptiont" v-html="course.description" />
            <el-tab-pane label="上课安排" name="schedule" lazy>
              <v-course-schedule :course-id="course.id" />
            </el-tab-pane>
            <el-tab-pane label="课程成绩" name="grade" lazy>
              <v-course-grade :course-id="course.id" :is-buy="!showBuyBanner" />
            </el-tab-pane>
            <el-tab-pane label="课程评价" name="comment" lazy>
              <v-course-comment :course="course" />
            </el-tab-pane>
          </el-tabs>
        </div>
        <!-- 讲师简介 -->
        <div style="width: 26%;background-color: #fff;float: right">
          <el-card>
            <div slot="header">讲师简介</div>
            <div style="display: flex;align-items: center;">
              <router-link :to="{name: 'SearchByTeacher', params: { teacher: teacher.id || 0 }}">
                <el-avatar fit="contain" :src="teacher.avatar" :size="70" style="float: left;margin-right: 12px" />
              </router-link>
              <div>
                <div style="color: #20a0ff;font-size: 18px;cursor: pointer" @click="linkToTeacher(teacher.id)">
                  {{ teacher.name }}
                </div>
                <div style="margin-top: 10px;font-size: 14px" :title="teacher.email">
                  <i class="el-icon-message" />
                  {{ teacher.email }}
                </div>
              </div>
            </div>
            <div style="margin-top: 12px;font-size: 15px;line-height: 24px;color: #666">
              {{ teacher.intro }}
            </div>
          </el-card>
        </div>
      </div>
    </el-col>

  </el-row>
</template>

<script>
import { getCourseDetail, getTeacher, getIsSelectCourse, selectCourse } from '@/api/content'
import MvCountDown from 'mv-count-down'
import { mapGetters } from 'vuex'

export default {
  name: 'Course',
  components: {
    'v-course-resource': () => import('@/components/course/course_resource'),
    'v-course-comment': () => import('@/components/course/course_comment'),
    'v-course-schedule': () => import('@/components/course/course_schedule'),
    'v-course-grade': () => import('@/components/course/course_grade'),
    MvCountDown
  },
  data() {
    return {
      course: { id: 0 },
      teacher: {},
      selectCourseDialogVisible: false,
      isSelectTheCourse: false,
      activeTab: 'desc',
      currentSemester: ''
    }
  },
  computed: {
    ...mapGetters(['user']),
    showSelectBanner: function() {
      return !this.isSelectTheCourse && this.course.semester === this.currentSemester
    }
  },
  created() {
    this.getCurrentSemester()
    const courseId = this.$route.params.id
    this.getCourseData(courseId)
    this.getIsSelectCourse(courseId)
  },
  methods: {
    getCurrentSemester() {
      const now = new Date()
      const month = now.getMonth() + 1
      this.currentSemester = (month >= 2 && month <= 7) ? 'autumn' : 'spring'
    },
    getCourseData(id) {
      // 使用模拟数据实现课程详情
      setTimeout(() => {
        this.course = {
          id: parseInt(id),
          title: '高等数学',
          teacherId: 1,
          teacher: '张老师',
          subject: '数学',
          subjectParent: { title: '公共课', parent: null },
          semester: this.currentSemester,
          courseType: 'public',
          majors: [],
          cover: 'https://c-ssl.duitang.com/uploads/item/201912/05/20191205152830_ULrYx.thumb.300_0.jpeg',
          description: '<h3>课程简介</h3><p>高等数学是大学数学的基础课程，主要包括微积分、线性代数等内容。本课程旨在培养学生的数学思维能力和解决实际问题的能力。</p><h3>课程目标</h3><ul><li>掌握微积分的基本概念和方法</li><li>培养数学建模和问题解决能力</li><li>为后续专业课程学习打下基础</li></ul>',
          credit: 4,
          lessonNum: 64,
          duration: '16周',
          status: 'ongoing',
          buyCount: 120,
          viewCount: 350,
          selectStartTime: new Date().toISOString(),
          selectEndTime: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString()
        }
        // 获取讲师信息
        this.getTeacher(this.course.teacherId)
        // 初始化资源展示
        setTimeout(function() {
          if (this.$refs.CourseResource) {
            this.$refs.CourseResource.setData(this.course)
          }
        }.bind(this), 100)
      }, 500)
    },
    getTeacher(id) {
      // 使用模拟数据实现教师信息
      setTimeout(() => {
        this.teacher = {
          id: 1,
          name: '张老师',
          avatar: 'https://c-ssl.duitang.com/uploads/item/201912/05/20191205152830_ULrYx.thumb.300_0.jpeg',
          email: 'zhang@example.com',
          intro: '张老师拥有数学博士学位，从事高等数学教学工作10余年，教学经验丰富，曾获校级优秀教师称号。'
        }
      }, 300)
    },
    getIsSelectCourse(id) {
      if (this.user === null || Object.keys(this.user).length === 0) {
        return
      }
      // 使用模拟数据实现是否已选课
      setTimeout(() => {
        this.isSelectTheCourse = false
      }, 300)
    },
    getDetailsSubject(subjectParent) {
      let subject = []
      let parent = subjectParent
      while (parent) {
        subject = [parent.title, ...subject]
        parent = parent.parent
      }
      return subject
    },
    openSelectCourseDialog() {
      if (this.user === null || Object.keys(this.user).length === 0) {
        this.$login()
        return
      }
      
      if (this.course.semester !== this.currentSemester) {
        this.$message.warning('该课程不在当前学期开设，无法选择')
        return
      }
      
      // 检查选课时间限制
      const now = new Date()
      if (this.course.selectStartTime && new Date(this.course.selectStartTime) > now) {
        this.$message.warning('选课尚未开始，请在规定时间内选课')
        return
      }
      if (this.course.selectEndTime && new Date(this.course.selectEndTime) < now) {
        this.$message.warning('选课已结束，请等待下次选课时间')
        return
      }
      
      // 检查专业限制
      if (this.course.courseType === 'major' && this.course.majors && this.course.majors.length > 0) {
        if (!this.user.major || !this.course.majors.includes(this.user.major)) {
          this.$message.warning('该课程仅限特定专业学生选择')
          return
        }
      }
      
      // 使用模拟数据实现选课
      setTimeout(() => {
        this.$message.success('选课成功！')
        this.isSelectTheCourse = true
      }, 500)
    },
    linkToTeacher(tid) {
      this.$router.push({
        name: 'SearchByTeacher',
        params: { teacher: tid }
      })
    }
  }
}
</script>

<style lang="scss">
.el-tabs__item {
  font-size: 16px;
  height: 50px;
  line-height: 50px;
}

.elmessage {
  z-index: 9999 !important;
}
</style>

<style lang="scss" scoped>
.sub-course {
  background-color: #333;
  position: absolute;
  bottom: 0;
  width: 100%;
  min-height: 80px;
  z-index: 3;
  padding: 14px 16px;
  display: flex;
  align-items: center;
}

// 课程介绍颜色
.course-descriptiont {
  /* table 样式 */
  table {
    border-top: 1px solid #ccc;
    border-left: 1px solid #ccc;
  }

  table td,
  table th {
    border-bottom: 1px solid #ccc;
    border-right: 1px solid #ccc;
    padding: 3px 5px;
  }

  table th {
    border-bottom: 2px solid #ccc;
    text-align: center;
  }

  /* blockquote 样式 */
  blockquote {
    display: block;
    border-left: 8px solid #d0e5f2;
    padding: 5px 10px;
    margin: 10px 0;
    line-height: 1.4;
    font-size: 100%;
    background-color: #f1f1f1;
  }

  /* code 样式 */
  code {
    display: inline-block;
    *display: inline;
    *zoom: 1;
    background-color: #f1f1f1;
    border-radius: 3px;
    padding: 3px 5px;
    margin: 0 3px;
  }

  pre code {
    display: block;
  }

  /* ul ol 样式 */
  ul, ol {
    margin: 10px 0 10px 20px;
  }
}
</style>
