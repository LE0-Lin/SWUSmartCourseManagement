<template>
  <el-row style="position: relative" class="course">
    <el-col :span="17" style="position: relative">
      <div style="padding-top: 56.25%" />
      <div class="cover-container">
        <el-image fit="cover" :src="encodeOssFileUri(course.cover)" class="img">
          <div slot="error" class="image-slot">
            <i class="el-icon-picture-outline" />
          </div>
        </el-image>
        <div class="course-info-overlay">
          <h2>{{ course.title }}</h2>
          <p>讲师：{{ teacher.name }}</p>
        </div>
      </div>
    </el-col>
    <el-col :key="refreshKey" :span="7" class="selection">
      <div class="resource-header">
        <i class="el-icon-folder" /> 课程资源目录
      </div>
      <div v-for="chapter of chapterData" :key="chapter.id" class="chapter">
        <div class="chapter-title ellipse" :title="chapter.title">
          <i class="el-icon-notebook-2" /> {{ chapter.title }}
        </div>
        <div
          v-for="res of resourceData[chapter.id]"
          :key="res.id"
          class="resource-item"
        >
          <div class="res-title ellipse" :title="res.title">
            <i class="el-icon-document" /> {{ res.title }}
          </div>
          <div class="res-action">
            <el-button type="text" size="mini" icon="el-icon-download" @click="downloadResource(res)">下载资源</el-button>
            <span v-if="course.price > 0 && res.free" class="free-tag">免费</span>
          </div>
        </div>
        <div v-if="!resourceData[chapter.id] || resourceData[chapter.id].length === 0" class="no-resource">
          暂无资源
        </div>
      </div>
      <div v-if="chapterData.length === 0" class="no-resource" style="margin-top: 20px">
        暂无课程章节
      </div>
    </el-col>
  </el-row>
</template>

<script>
import { getChapters, getChapterResources } from '@/api/content'
import { mapGetters } from 'vuex'
import { encodeOssFileUri } from '@/utils'

export default {
  name: 'CourseResource',
  props: {
    teacher: {
      type: Object,
      default: () => ({ name: '' })
    }
  },
  data() {
    return {
      course: { cover: '', price: 0, title: '' },
      chapterData: [],
      // <chapterId, resourceList>
      resourceData: {},
      refreshKey: false
    }
  },
  computed: {
    ...mapGetters(['user'])
  },
  methods: {
    setData(course) {
      this.course = course
      this.listChaptersAndResources(course.id)
    },
    encodeOssFileUri(ossUri) {
      if (!ossUri) return ''
      return encodeOssFileUri(ossUri)
    },
    listChaptersAndResources(courseId) {
      getChapters(courseId).then(resp => {
        this.chapterData = resp.data
        let len = resp.data.length
        if (len === 0) {
          this.refreshKey = !this.refreshKey
          return
        }
        for (const c of resp.data) {
          getChapterResources(c.id).then(resp => {
            this.$set(this.resourceData, c.id, resp.data)
            if (--len <= 0) {
              this.refreshKey = !this.refreshKey
            }
          })
        }
      })
    },
    downloadResource(res) {
      if (this.user === null || Object.keys(this.user).length === 0) {
        this.$login()
        return
      }
      this.$message.info(`正在准备下载资源: ${res.title}。如果是实际项目，此处应调用下载接口。`)
      // window.open(res.url) // 如果有直接的资源URL
    }
  }
}
</script>

<style scoped lang="scss">
.cover-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;

  .img {
    width: 100%;
    height: 100%;
    z-index: 1;
  }

  .image-slot {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    background: #f5f7fa;
    color: #909399;
    font-size: 30px;
  }

  .course-info-overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    padding: 20px;
    background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
    color: #fff;
    z-index: 2;
    h2 { margin: 0 0 5px 0; }
    p { margin: 0; font-size: 14px; opacity: 0.9; }
  }
}

.course {
  .selection {
    position: absolute;
    right: 0;
    height: 100%;
    padding: 20px 16px;
    overflow: auto;
    background-color: #ffffff;
    border-left: 1px solid #e6e6e6;

    .resource-header {
      font-size: 18px;
      font-weight: bold;
      margin-bottom: 20px;
      padding-bottom: 10px;
      border-bottom: 2px solid #409eff;
      color: #303133;
    }

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: #c0c4cc;
    }

    &::-webkit-scrollbar-track {
      background: #f5f7fa;
    }
  }
}

.chapter {
  margin-bottom: 20px;

  .chapter-title {
    font-size: 15px;
    font-weight: bold;
    color: #333;
    padding: 8px 0;
    border-bottom: 1px dashed #ebeef5;
    margin-bottom: 10px;
  }

  .resource-item {
    padding: 10px 10px 10px 20px;
    border-radius: 4px;
    transition: background-color 0.3s;

    &:hover {
      background-color: #f0f7ff;
    }

    .res-title {
      font-size: 14px;
      color: #606266;
      margin-bottom: 5px;
    }

    .res-action {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .free-tag {
        font-size: 12px;
        color: #ff4f23;
        border: 1px solid #ff4f23;
        padding: 0 4px;
        border-radius: 2px;
      }
    }
  }

  .no-resource {
    font-size: 12px;
    color: #999;
    padding-left: 20px;
    margin-top: 5px;
  }
}
</style>
