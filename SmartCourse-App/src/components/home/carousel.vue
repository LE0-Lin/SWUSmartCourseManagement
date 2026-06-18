<!-- 分类与轮播 -->
<template>
  <el-row class="home-carousel">
    <el-col :xs="24" :sm="6" :md="5" :lg="4">
      <el-cascader-panel
        v-model="selectionValue"
        :options="subjectData"
        class="subject-selection"
        :props="{ expandTrigger: 'hover', value: 'id', label: 'title' }"
        @change="selected"
      />
    </el-col>
    <el-col :xs="24" :sm="18" :md="19" :lg="20">
      <el-carousel
        trigger="click"
        height="var(--home-hero-height)"
        :interval="5000"
        class="course-carousel"
      >
        <el-carousel-item v-for="course in carouselCourses" :key="course.id">
          <router-link
            :to="{ name: 'Course', params: { id: course.id } }"
            :title="course.title"
            class="banner-link"
          >
            <el-image :src="encodeOssFileUri(course.cover)" fit="cover" class="banner-img" />
            <div class="banner-caption">{{ course.title }}</div>
            <div
              v-if="course.courseType"
              class="banner-category"
              :style="{ backgroundColor: courseTypeMeta(course.courseType).color }"
            >
              {{ courseTypeMeta(course.courseType).label }}
            </div>
          </router-link>
        </el-carousel-item>
      </el-carousel>
    </el-col>
  </el-row>
</template>

<script>
import { getSubjects, getSelectableCourses } from '@/api/content'
import { encodeOssFileUri } from '@/utils'
import { getCourseTypeMeta } from '@/utils/course_type'
export default {
  name: 'Carousel',
  data() {
    return {
      selectionValue: null,
      subjectData: [],
      carouselCourses: []
    }
  },
  created() {
    this.getSubjects()
    this.getCarouselCourses()
  },
  methods: {
    selected(val) {
      const arr = val
      if (!!arr || arr.length > 0) {
        const subjectId = arr[arr.length - 1]
        // const { href } = this.$router.resolve({
        //   name: 'SearchBySubject',
        //   params: { subject: subjectId }
        // })
        // window.open(href, '_blank')
        this.$router.push({
          name: 'SearchBySubject',
          params: { subject: subjectId }
        })
      }
    },
    encodeOssFileUri(ossUri) {
      return encodeOssFileUri(ossUri)
    },
    courseTypeMeta(courseType) {
      return getCourseTypeMeta(courseType)
    },
    getSubjects() {
      getSubjects().then(resp => {
        this.subjectData = resp.data
      })
    },
    getCarouselCourses() {
      getSelectableCourses().then(resp => {
        const courses = resp.data || []
        this.carouselCourses = this.shuffle(courses.filter(course => course.cover)).slice(0, 5)
      })
    },
    shuffle(courses) {
      const result = [...courses]
      for (let index = result.length - 1; index > 0; index--) {
        const randomIndex = Math.floor(Math.random() * (index + 1))
        const current = result[index]
        result[index] = result[randomIndex]
        result[randomIndex] = current
      }
      return result
    }
  }
}
</script>

<style lang="scss">
.home-carousel {
  --home-hero-height: clamp(260px, 22vw, 340px);
}

.subject-selection {
  position: relative;
  height: var(--home-hero-height);
  z-index: 1000;
  border-radius: 5px;

  .el-scrollbar {
    background-color: #fff;
    color: #333;
    min-width: 100%;
    width: 100%;
    border-radius: 5px;

    .el-scrollbar__wrap{
      overflow: auto;
    }

    .el-cascader-node__postfix{
      right: 20px;
    }

    &:first-child {
      background-color: rgba(0, 0, 0, .678431);
      color: #cccccc;
    }

    &:nth-child(2), &:nth-child(3), &:nth-child(4), &:nth-child(5) {
      visibility: hidden;
    }
  }
}

.subject-selection:hover {
  > .el-scrollbar {
    visibility: visible;
  }
}
</style>

<style scoped lang="scss">
.banner-link {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
}

.banner-img{
  width: 100%;
  height: 100%;
}

.banner-caption {
  position: absolute;
  right: 0;
  bottom: 28px;
  left: 0;
  padding: 14px 22px 10px;
  color: #fff;
  font-size: 18px;
  background: linear-gradient(transparent, rgba(0, 0, 0, .62));
}

.banner-category {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  height: 28px;
  padding: 0 22px;
  box-sizing: border-box;
  color: #fff;
  font-size: 13px;
  line-height: 28px;
}

.course-carousel {
  overflow: hidden;
  border-radius: 5px;
}

@media (max-width: 600px) {
  .home-carousel,
  .subject-selection {
    overflow: hidden;
  }
}
</style>
