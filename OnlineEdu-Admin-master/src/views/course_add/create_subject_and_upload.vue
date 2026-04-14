<template>
  <div>
    <el-tabs v-model="activeName" type="border-card">
      <el-tab-pane label="章节管理" name="first">
        <div class="subject-box">
          <el-button type="primary" @click="addSubjectVisible = true" style="margin-bottom: 15px">
            <i class="el-icon-plus" />
            新增章节
          </el-button>
          <el-tree
            :data="subjects"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
            :render-content="renderContent"
          >
            <template slot="append" @click="() => append(subjects)">
              <el-button
                size="small"
                type="primary"
                class="append-btn"
                @click.stop="addSubjectVisible = true"
              >
                <i class="el-icon-plus" />
                新增章节
              </el-button>
            </template>
          </el-tree>
        </div>
      </el-tab-pane>
      <el-tab-pane label="视频管理" name="second">
        <div class="video-box">
          <el-upload
            class="upload-demo"
            action=""
            :auto-upload="false"
            :on-change="handleVideoChange"
            :file-list="videoFileList"
            accept=".mp4, .avi, .wmv, .mov, .flv"
            :limit="1"
          >
            <el-button size="small" type="primary">选择视频</el-button>
            <div slot="tip" class="el-upload__tip">只能上传视频文件，且不超过500MB</div>
          </el-upload>
          <div v-if="videoFileList.length > 0" class="video-preview">
            <video :src="videoFileList[0].url" controls style="width: 100%; height: 400px"></video>
            <div class="video-info">
              <el-button type="danger" size="small" @click="removeVideo">删除视频</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增章节对话框 -->
    <el-dialog
      title="新增章节"
      :visible.sync="addSubjectVisible"
      destroy-on-close
      width="30vw"
    >
      <el-form :model="subjectForm" size="small" label-width="80px">
        <el-form-item label="章节名称：" prop="title">
          <el-input v-model="subjectForm.title" placeholder="请输入章节名称" />
        </el-form-item>
        <el-form-item label="章节排序：" prop="sort">
          <el-input-number v-model="subjectForm.sort" style="width: 100%" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button type="primary" size="small" @click="addSubject">确 认</el-button>
        <el-button size="small" @click="addSubjectVisible = false">取 消</el-button>
      </span>
    </el-dialog>

    <!-- 编辑章节对话框 -->
    <el-dialog
      title="编辑章节"
      :visible.sync="editSubjectVisible"
      destroy-on-close
      width="30vw"
    >
      <el-form :model="subjectForm" size="small" label-width="80px">
        <el-form-item label="章节名称：" prop="title">
          <el-input v-model="subjectForm.title" placeholder="请输入章节名称" />
        </el-form-item>
        <el-form-item label="章节排序：" prop="sort">
          <el-input-number v-model="subjectForm.sort" style="width: 100%" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button type="primary" size="small" @click="editSubject">确 认</el-button>
        <el-button size="small" @click="editSubjectVisible = false">取 消</el-button>
      </span>
    </el-dialog>

    <!-- 新增课时对话框 -->
    <el-dialog
      title="新增课时"
      :visible.sync="addLessonVisible"
      destroy-on-close
      width="30vw"
    >
      <el-form :model="lessonForm" size="small" label-width="80px">
        <el-form-item label="课时名称：" prop="title">
          <el-input v-model="lessonForm.title" placeholder="请输入课时名称" />
        </el-form-item>
        <el-form-item label="课时排序：" prop="sort">
          <el-input-number v-model="lessonForm.sort" style="width: 100%" />
        </el-form-item>
        <el-form-item label="课时类型：" prop="type">
          <el-radio-group v-model="lessonForm.type">
            <el-radio label="video">视频</el-radio>
            <el-radio label="document">文档</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button type="primary" size="small" @click="addLesson">确 认</el-button>
        <el-button size="small" @click="addLessonVisible = false">取 消</el-button>
      </span>
    </el-dialog>

    <!-- 编辑课时对话框 -->
    <el-dialog
      title="编辑课时"
      :visible.sync="editLessonVisible"
      destroy-on-close
      width="30vw"
    >
      <el-form :model="lessonForm" size="small" label-width="80px">
        <el-form-item label="课时名称：" prop="title">
          <el-input v-model="lessonForm.title" placeholder="请输入课时名称" />
        </el-form-item>
        <el-form-item label="课时排序：" prop="sort">
          <el-input-number v-model="lessonForm.sort" style="width: 100%" />
        </el-form-item>
        <el-form-item label="课时类型：" prop="type">
          <el-radio-group v-model="lessonForm.type">
            <el-radio label="video">视频</el-radio>
            <el-radio label="document">文档</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button type="primary" size="small" @click="editLesson">确 认</el-button>
        <el-button size="small" @click="editLessonVisible = false">取 消</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>

export default {
  name: 'CreateSubjectAndUpload',
  props: {
    course: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      activeName: 'first',
      subjects: [],
      addSubjectVisible: false,
      editSubjectVisible: false,
      addLessonVisible: false,
      editLessonVisible: false,
      subjectForm: {
        title: '',
        sort: 0
      },
      lessonForm: {
        title: '',
        sort: 0,
        type: 'video'
      },
      currentSubject: null,
      currentLesson: null,
      videoFileList: []
    }
  },
  created() {
    this.getSubjects()
  },
  methods: {
    getSubjects() {
      // 这里应该从后端获取章节列表，暂时使用模拟数据
      this.subjects = [
        {
          id: 1,
          title: '第一章 课程介绍',
          sort: 1,
          children: [
            {
              id: 11,
              title: '1.1 课程概述',
              sort: 1,
              type: 'video'
            },
            {
              id: 12,
              title: '1.2 学习目标',
              sort: 2,
              type: 'document'
            }
          ]
        },
        {
          id: 2,
          title: '第二章 基础理论',
          sort: 2,
          children: [
            {
              id: 21,
              title: '2.1 基本概念',
              sort: 1,
              type: 'video'
            },
            {
              id: 22,
              title: '2.2 核心原理',
              sort: 2,
              type: 'video'
            }
          ]
        }
      ]
    },
    renderContent(h, { node, data, store }) {
      return h('span', {
        class: 'custom-tree-node'
      }, [
        h('span', data.title),
        h('span', {
          class: 'custom-tree-node-actions'
        }, [
          h('el-button', {
            attrs: {
              size: 'mini',
              type: 'primary'
            },
            on: {
              click: (e) => {
                e.stopPropagation()
                this.addLesson(data)
              }
            }
          }, '新增课时'),
          h('el-button', {
            attrs: {
              size: 'mini',
              type: 'warning',
              style: 'margin-left: 5px'
            },
            on: {
              click: (e) => {
                e.stopPropagation()
                this.editSubject(data)
              }
            }
          }, '编辑'),
          h('el-button', {
            attrs: {
              size: 'mini',
              type: 'danger',
              style: 'margin-left: 5px'
            },
            on: {
              click: (e) => {
                e.stopPropagation()
                this.deleteSubject(data, store)
              }
            }
          }, '删除')
        ]),
        node.children && node.children.length > 0 ? '' : h('span', {
          class: 'custom-tree-node-actions'
        }, [
          h('el-button', {
            attrs: {
              size: 'mini',
              type: 'primary'
            },
            on: {
              click: (e) => {
                e.stopPropagation()
                this.addLesson(data)
              }
            }
          }, '新增课时')
        ])
      ])
    },
    addSubject() {
      const newSubject = {
        id: Date.now(),
        title: this.subjectForm.title,
        sort: this.subjectForm.sort,
        children: []
      }
      this.subjects.push(newSubject)
      this.addSubjectVisible = false
      this.subjectForm = {
        title: '',
        sort: 0
      }
    },
    editSubject(data) {
      this.currentSubject = data
      this.subjectForm = {
        title: data.title,
        sort: data.sort
      }
      this.editSubjectVisible = true
    },
    deleteSubject(data, store) {
      this.$confirm('确定要删除这个章节吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        store.remove(data)
        this.$message.success('删除成功')
      }).catch(() => {
        this.$message.info('取消删除')
      })
    },
    addLesson(data) {
      this.currentSubject = data
      this.lessonForm = {
        title: '',
        sort: 0,
        type: 'video'
      }
      this.addLessonVisible = true
    },
    editLesson(data) {
      this.currentLesson = data
      this.lessonForm = {
        title: data.title,
        sort: data.sort,
        type: data.type
      }
      this.editLessonVisible = true
    },
    handleVideoChange(file, fileList) {
      this.videoFileList = fileList
      if (file.raw) {
        this.videoFileList[0].url = URL.createObjectURL(file.raw)
      }
    },
    removeVideo() {
      this.videoFileList = []
    }
  }
}
</script>

<style scoped lang="scss">
.subject-box {
  margin: 20px 0;

  .custom-tree-node {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;

    .custom-tree-node-actions {
      display: flex;
      align-items: center;
    }
  }
}

.video-box {
  margin: 20px 0;

  .video-preview {
    margin-top: 20px;

    .video-info {
      margin-top: 10px;
      text-align: right;
    }
  }
}

</style>