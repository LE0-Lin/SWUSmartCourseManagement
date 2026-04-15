<template>
  <el-row type="flex" justify="center">
    <el-col :span="20">
      <el-breadcrumb separator-class="el-icon-arrow-right" style="margin: 20px 0; color: #333">
        <el-breadcrumb-item>全部课程</el-breadcrumb-item>
        <el-breadcrumb-item v-for="(item, index) in getDetailsSubject(course.subjectParent)" :key="index">
          {{ item }}
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ course.title }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="course-hero">
        <el-image fit="cover" :src="encodeOssFileUri(course.cover)" class="course-cover">
          <div slot="error" class="image-slot">
            <i class="el-icon-picture-outline" />
          </div>
        </el-image>
        <div class="course-mask">
          <div>
            <div class="course-title">{{ course.title }}</div>
            <div class="course-meta">
              <span>{{ course.lessonNum || 0 }} 课时</span>
              <span>{{ course.buyCount || 0 }} 人选课</span>
              <span>{{ course.viewCount || 0 }} 次浏览</span>
            </div>
          </div>
          <div class="course-actions">
            <el-button
              v-if="!isSelectTheCourse"
              type="primary"
              icon="el-icon-plus"
              @click="handleSelectCourse"
            >
              选择课程
            </el-button>
            <el-tag v-else type="success">已选课</el-tag>
          </div>
        </div>
      </div>

      <div class="clearfix" style="margin-top: 20px">
        <div class="main-panel">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="课程概述" name="desc" class="course-description" v-html="course.description" />
            <el-tab-pane label="上课安排" name="schedule" lazy>
              <v-course-schedule :course-id="course.id" />
            </el-tab-pane>
            <el-tab-pane label="课程成绩" name="grade" lazy>
              <v-course-grade :course-id="course.id" :is-buy="isSelectTheCourse" />
            </el-tab-pane>
            <el-tab-pane label="课程评价" name="comment" lazy>
              <v-course-comment :course="course" />
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="side-panel">
          <el-card>
            <div slot="header">讲师简介</div>
            <div style="display: flex; align-items: center">
              <router-link :to="{ name: 'SearchByTeacher', params: { teacher: teacher.id || 0 } }">
                <el-avatar fit="contain" :src="teacher.avatar" :size="70" style="float: left; margin-right: 12px" />
              </router-link>
              <div>
                <div style="color: #20a0ff; font-size: 18px; cursor: pointer" @click="linkToTeacher(teacher.id)">
                  {{ teacher.name }}
                </div>
                <div style="margin-top: 10px; font-size: 14px" :title="teacher.email">
                  <i class="el-icon-message" />
                  {{ teacher.email }}
                </div>
              </div>
            </div>
            <div style="margin-top: 12px; font-size: 15px; line-height: 24px; color: #666">
              {{ teacher.intro }}
            </div>
          </el-card>
        </div>
      </div>
    </el-col>
  </el-row>
</template>

<script>
import {
  getCourseDetail,
  getTeacher as fetchTeacher,
  getIsSelectCourse as fetchIsSelectCourse,
  selectCourse as requestSelectCourse
} from '@/api/content'
import { mapGetters } from 'vuex'
import { encodeOssFileUri } from '@/utils'

export default {
  name: 'Course',
  components: {
    'v-course-comment': () => import('@/components/course/course_comment'),
    'v-course-schedule': () => import('@/components/course/course_schedule'),
    'v-course-grade': () => import('@/components/course/course_grade')
  },
  data() {
    return {
      course: { id: 0 },
      teacher: {},
      isSelectTheCourse: false,
      activeTab: 'desc'
    }
  },
  computed: {
    ...mapGetters(['user'])
  },
  created() {
    const courseId = this.$route.params.id
    this.getCourseData(courseId)
    this.getIsSelectCourse(courseId)
  },
  methods: {
    encodeOssFileUri(ossUri) {
      return encodeOssFileUri(ossUri)
    },
    getCourseData(id) {
      getCourseDetail(id).then(resp => {
        this.course = resp.data || { id: 0 }
        if (this.course.teacherId) {
          this.getTeacher(this.course.teacherId)
        }
      })
    },
    getTeacher(id) {
      fetchTeacher(id).then(resp => {
        this.teacher = resp.data || {}
      })
    },
    getIsSelectCourse(id) {
      if (!this.user || Object.keys(this.user).length === 0) {
        this.isSelectTheCourse = false
        return
      }
      fetchIsSelectCourse(id).then(resp => {
        this.isSelectTheCourse = !!resp.data
      }).catch(() => {
        this.isSelectTheCourse = false
      })
    },
    handleSelectCourse() {
      if (!this.user || Object.keys(this.user).length === 0) {
        this.$login()
        return
      }
      requestSelectCourse(this.course.id).then(resp => {
        this.$message.success(resp.message)
        this.isSelectTheCourse = true
        this.getCourseData(this.course.id)
      })
    },
    getDetailsSubject(subjectParent) {
      const subject = []
      let parent = subjectParent
      while (parent) {
        subject.unshift(parent.title)
        parent = parent.parent
      }
      return subject
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
</style>

<style lang="scss" scoped>
.course-hero {
  position: relative;
  height: 360px;
  overflow: hidden;
  border-radius: 8px;
  background: #f5f7fa;
}

.course-cover {
  width: 100%;
  height: 100%;
}

.image-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 32px;
  color: #909399;
}

.course-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 24px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.75));
  color: #fff;
}

.course-title {
  font-size: 28px;
  font-weight: 600;
}

.course-meta {
  display: flex;
  gap: 20px;
  margin-top: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.course-actions {
  display: flex;
  align-items: center;
}

.main-panel {
  width: 73%;
  float: left;
  background-color: #fff;
  padding: 10px 20px;
}

.side-panel {
  width: 26%;
  float: right;
  background-color: #fff;
}

.course-description {
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

  blockquote {
    display: block;
    border-left: 8px solid #d0e5f2;
    padding: 5px 10px;
    margin: 10px 0;
    line-height: 1.4;
    font-size: 100%;
    background-color: #f1f1f1;
  }

  code {
    display: inline-block;
    background-color: #f1f1f1;
    border-radius: 3px;
    padding: 3px 5px;
    margin: 0 3px;
  }

  pre code {
    display: block;
  }

  ul, ol {
    margin: 10px 0 10px 20px;
  }
}
</style>
