<template>
  <div>
    <el-row :gutter="20" class="course">
      <el-col
        v-for="course of courseList"
        :key="course.id"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
        :xl="6"
      >
        <router-link :to="{name:'Course', params: {id:course.id}}">
          <div class="course-item">
            <div class="cover-wrap">
              <el-image fit="cover" :src="encodeOssFileUri(course.cover)" class="img" />
              <div
                v-if="course.courseType"
                class="category-strip"
                :style="{ backgroundColor: courseTypeMeta(course.courseType).color }"
              >
                {{ courseTypeMeta(course.courseType).label }}
              </div>
            </div>
            <div class="info">
              <div class="title ellipse" :title="course.title">{{ course.title }}</div>
              <div class="middle">
                <span class="course-num">共{{ course.lessonNum }}节</span>
                <span class="credit">{{ course.credit }}学分</span>
                &nbsp;|&nbsp;
                <span class="tch">{{ course.teacherName }}</span>
              </div>
              <div class="curriculum">
                <span v-if="course.curriculumSemester">建议第{{ course.curriculumSemester }}学期开设</span>
                <span v-if="course.assessmentMethod"> · {{ course.assessmentMethod }}</span>
              </div>
              <div class="bottom">
                <span class="sub-num">共{{ course.buyCount }}人选修</span>
              </div>
            </div>
          </div>
        </router-link>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { encodeOssFileUri } from '@/utils'
import { getCourseTypeMeta } from '@/utils/course_type'

export default {
  name: 'CourseList',
  props: {
    courseList: {
      type: Array,
      required: true
    }
  },
  data() {
    return {
    }
  },
  methods: {
    encodeOssFileUri(ossUri) {
      return encodeOssFileUri(ossUri)
    },
    courseTypeMeta(courseType) {
      return getCourseTypeMeta(courseType)
    }
  }
}
</script>

<style scoped lang="scss">
.course-item {
  background-color: #FFFFFF;
  margin-bottom: 20px;
  border-radius: 5px;

  &:hover {
    cursor: pointer;
    box-shadow: 0 3px 18px rgba(0, 0, 0, .2);
    transform: translateY(-2px);
    transition: all .3s;
  }

  .cover-wrap {
    position: relative;
    width: 100%;
    aspect-ratio: 16 / 9;
    overflow: hidden;
    border-radius: 5px 5px 0 0;

    .img {
      display: block;
      height: 100%;
      width: 100%;
    }

    .category-strip {
      position: absolute;
      right: 0;
      bottom: 0;
      left: 0;
      z-index: 2;
      display: flex;
      align-items: center;
      height: 28px;
      padding: 0 12px;
      box-sizing: border-box;
      color: #fff;
      font-size: 12px;
      line-height: 28px;
    }
  }

  .info {
    padding: 5px 10px 12px 10px;
    color: #333;

    .title {
      font-size: 14px;
    }

    .middle {
      color: #999;
      font-size: 12px;
      margin: 3px 0;
    }

    .curriculum {
      min-height: 17px;
      color: #7f8c8d;
      font-size: 12px;
      line-height: 17px;
    }

    .bottom {
      .sub-num {
        font-size: 12px;
        color: #999;
      }
    }
  }
}
</style>
