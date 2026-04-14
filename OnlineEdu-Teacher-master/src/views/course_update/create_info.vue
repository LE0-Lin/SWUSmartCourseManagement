<!-- 步骤1 ： 创建课程基本信息 -->
<template>
  <div>
    <el-form :model="data" size="small" label-width="100px">
      <el-form-item label="课程封面：" prop="cover">
        <el-upload
          action=""
          :auto-upload="false"
          :show-file-list="false"
          accept=".png, .jpg, .jpeg"
          :on-change="selectAvatar"
        >
          <el-tooltip effect="dark" content="点击上传海报，图片不能超过2MB" placement="right">
            <el-image :src="encodeOssFileUri(original.cover)" fit="cover" class="cover-img">
              <div slot="error" class="image-slot">
                <i class="el-icon-picture-outline" />
              </div>
            </el-image>
          </el-tooltip>
        </el-upload>
      </el-form-item>
      <el-form-item label="课程名称：" prop="title">
        <el-input v-model="data.title" placeholder="请输入课程名称" />
      </el-form-item>
      <el-form :model="data" :inline="true" size="small" label-width="100px">
        <el-form-item label="学　　分：" prop="credit">
          <el-input-number v-model="data.credit" :min="0" :max="10" :step="0.5" style="width: 200px" />
        </el-form-item>
        <el-form-item label="课程分类：" prop="subjectId">
          <el-cascader
            v-model="data.subjectId"
            :options="subjectOptions"
            :props="{ expandTrigger: 'hover', value: 'id', label: 'title' }"
            :clearable="true"
            :filterable="true"
            placeholder="请选择课程分类"
            style="width: 360px"
            @change="subjectOptionsChange"
          />
        </el-form-item>
      </el-form>
      
      <el-form :model="data" size="small" label-width="100px">
        <el-form-item label="成绩占比设置：">
          <el-form :model="data.gradeRatio" size="small" label-width="80px">
            <el-form-item label="平时分：" prop="gradeRatio.usual">
              <el-input-number v-model="data.gradeRatio.usual" :min="0" :max="100" :step="5" style="width: 120px" />
              <span style="margin-left: 10px">%</span>
            </el-form-item>
            <el-form-item label="实验分：" prop="gradeRatio.experiment">
              <el-input-number v-model="data.gradeRatio.experiment" :min="0" :max="100" :step="5" style="width: 120px" />
              <span style="margin-left: 10px">%</span>
            </el-form-item>
            <el-form-item label="期中：" prop="gradeRatio.midterm">
              <el-input-number v-model="data.gradeRatio.midterm" :min="0" :max="100" :step="5" style="width: 120px" />
              <span style="margin-left: 10px">%</span>
            </el-form-item>
            <el-form-item label="期末：" prop="gradeRatio.final">
              <el-input-number v-model="data.gradeRatio.final" :min="0" :max="100" :step="5" style="width: 120px" />
              <span style="margin-left: 10px">%</span>
            </el-form-item>
          </el-form>
        </el-form-item>
      </el-form>
      
      <el-form :model="data" size="small" label-width="100px">
        <el-form-item label="选课设置：">
          <el-form size="small" label-width="120px">
            <el-form-item label="课程类型：" prop="courseType">
              <el-radio-group v-model="data.courseType">
                <el-radio label="public">公共课</el-radio>
                <el-radio label="major">专业课</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="所属院系：" prop="department">
              <el-select v-model="data.department" placeholder="请选择所属院系" style="width: 100%" @change="handleDepartmentChange">
                <el-option label="计算机信息与技术学院" value="computer" />
                <el-option label="文学院" value="literature" />
                <el-option label="数学学院" value="mathematics" />
                <el-option label="体育学院" value="sports" />
                <el-option label="医学院" value="medical" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="所属专业：" prop="major">
              <el-select v-model="data.major" placeholder="请选择所属专业" style="width: 100%">
                <el-option v-for="major in majorOptions" :key="major.value" :label="major.label" :value="major.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="适用专业：" prop="majors" v-if="data.courseType === 'major'">
              <el-select v-model="data.majors" multiple placeholder="请选择适用专业" style="width: 100%">
                <el-option v-for="major in majorOptions" :key="major.value" :label="major.label" :value="major.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="开设学期：" prop="semester">
              <el-select v-model="data.semester" placeholder="请选择开设学期" style="width: 100%">
                <el-option label="上学期" value="spring" />
                <el-option label="下学期" value="autumn" />
              </el-select>
            </el-form-item>
            <el-form-item label="选课开始时间：" prop="selectStartTime">
              <el-date-picker
                v-model="data.selectStartTime"
                type="datetime"
                placeholder="选择开始时间"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="选课结束时间：" prop="selectEndTime">
              <el-date-picker
                v-model="data.selectEndTime"
                type="datetime"
                placeholder="选择结束时间"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="上课时间设置：" prop="classTimes">
              <el-button type="primary" size="small" @click="addClassTime" style="margin-bottom: 10px">
                <i class="el-icon-plus"></i> 添加上课时间
              </el-button>
              <div v-for="(time, index) in data.classTimes" :key="index" class="class-time-item">
                <el-row :gutter="20">
                  <el-col :span="6">
                    <el-form-item label="星期：" :prop="`classTimes[${index}].weekday`">
                      <el-select v-model="time.weekday" placeholder="选择星期" style="width: 100%">
                        <el-option label="周一" value="1" />
                        <el-option label="周二" value="2" />
                        <el-option label="周三" value="3" />
                        <el-option label="周四" value="4" />
                        <el-option label="周五" value="5" />
                        <el-option label="周六" value="6" />
                        <el-option label="周日" value="7" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-form-item label="课时：" :prop="`classTimes[${index}].period`">
                      <el-select v-model="time.period" placeholder="选择课时" style="width: 100%">
                        <el-option label="1-2节 (8:00-9:40)" value="12" />
                        <el-option label="3-4节 (10:00-11:40)" value="34" />
                        <el-option label="5-6节 (14:00-15:40)" value="56" />
                        <el-option label="7-8节 (16:00-17:40)" value="78" />
                        <el-option label="9-10节 (19:00-20:40)" value="910" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="4">
                    <el-form-item label="开始周：" :prop="`classTimes[${index}].startWeek`">
                      <el-input-number v-model="time.startWeek" :min="1" :max="16" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="4">
                    <el-form-item label="结束周：" :prop="`classTimes[${index}].endWeek`">
                      <el-input-number v-model="time.endWeek" :min="1" :max="16" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="3">
                    <el-form-item label="教学楼：" :prop="`classTimes[${index}].building`">
                      <el-select v-model="time.building" placeholder="选择教学楼" style="width: 100%" @change="onBuildingChange(index)">
                        <el-option label="A楼" value="A" />
                        <el-option label="B楼" value="B" />
                        <el-option label="C楼" value="C" />
                        <el-option label="D楼" value="D" />
                        <el-option label="E楼" value="E" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="3">
                    <el-form-item label="教室：" :prop="`classTimes[${index}].classroom`">
                      <el-select v-model="time.classroom" placeholder="选择教室" style="width: 100%">
                        <el-option v-for="classroom in getClassrooms(time.building)" :key="classroom" :label="classroom" :value="classroom" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="4">
                    <el-form-item label="操作：">
                      <el-button type="danger" size="small" @click="removeClassTime(index)">
                        <i class="el-icon-delete"></i> 删除
                      </el-button>
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>
            </el-form-item>
          </el-form>
        </el-form-item>
      </el-form>
      
      <div>
        <p class="into">课程简介：</p>
        <div id="editor" />
      </div>
    </el-form>
  </div>
</template>

<script>

import { uploadPic } from '@/api/course'
import { getSubject } from '@/api/subject'
import { encodeOssFileUri, jsonObj2FormData } from '@/utils'
import E from 'wangeditor'
import store from '@/store'

export default {
  name: 'CreateInfo',
  data() {
    return {
      original: {
        cover: ''
      },
      data: {
        id: 0,
        title: '',
        description: '',
        credit: 0,
        subjectId: null,
        gradeRatio: {
          usual: 30,
          experiment: 0,
          midterm: 20,
          final: 50
        },
        courseType: 'public',
        department: '',
        major: '',
        majors: [],
        semester: '',
        selectStartTime: null,
        selectEndTime: null,
        classTimes: []
      },
      editor: null,
      // 分类选项
      subjectOptions: [],
      // 院系专业关系
      departmentMajorMap: {
        computer: [
          { label: '计算机科学与技术', value: 'computer_science' },
          { label: '软件工程', value: 'software_engineering' },
          { label: '网络工程', value: 'network_engineering' },
          { label: '信息安全', value: 'information_security' },
          { label: '物联网工程', value: 'iot_engineering' }
        ],
        literature: [
          { label: '汉语言文学', value: 'chinese_language' },
          { label: '新闻学', value: 'journalism' },
          { label: '广告学', value: 'advertising' },
          { label: '历史学', value: 'history' }
        ],
        mathematics: [
          { label: '数学与应用数学', value: 'mathematics' },
          { label: '统计学', value: 'statistics' },
          { label: '信息与计算科学', value: 'information_computing' }
        ],
        sports: [
          { label: '体育教育', value: 'physical_education' },
          { label: '运动训练', value: 'sports_training' },
          { label: '社会体育指导', value: 'social_sports' }
        ],
        medical: [
          { label: '临床医学', value: 'clinical_medicine' },
          { label: '护理学', value: 'nursing' },
          { label: '药学', value: 'pharmacy' },
          { label: '医学影像学', value: 'medical_imaging' }
        ],
        other: [
          { label: '其他', value: 'other' }
        ]
      },
      majorOptions: []
    }
  },
  created() {
    this.$nextTick(() => {
      this.initEditor()
    })
    this.getSubject()
  },
  methods: {
    setData(data) {
      this.original = data
      Object.keys(this.data).forEach((key) => {
          if (key === 'gradeRatio' && this.original[key]) {
            Object.keys(this.data.gradeRatio).forEach((ratioKey) => {
              this.data.gradeRatio[ratioKey] = this.original[key][ratioKey] || 0
            })
          } else if (key === 'classTimes' && this.original[key]) {
            this.data.classTimes = this.original[key]
          } else {
            this.data[key] = this.original[key]
          }
        })
      this.editor.txt.html(this.data.description)
      // 根据院系更新专业选项
      if (this.data.department) {
        this.handleDepartmentChange(this.data.department)
      }
    },
    getData() {
      this.data.teacherId = store.getters.user.id
      return jsonObj2FormData(this.data)
    },
    encodeOssFileUri(ossUri) {
      return encodeOssFileUri(ossUri)
    },
    // 选择图片事件
    selectAvatar(file) {
      this.original.cover = URL.createObjectURL(file.raw)
      this.data.file = file.raw
    },
    getSubject() {
      getSubject().then(resp => {
        this.subjectOptions = resp.data
      })
    },
    subjectOptionsChange(val) {
      const arr = val
      if (!!arr || arr.length > 0) {
        this.data.subjectId = arr[arr.length - 1]
      }
    },
    // 院系变化时更新专业选项
    handleDepartmentChange(department) {
      this.data.major = ''
      this.data.majors = []
      this.majorOptions = this.departmentMajorMap[department] || []
    },
    // 添加上课时间
    addClassTime() {
      this.data.classTimes.push({
        weekday: '1',
        period: '12',
        startWeek: 1,
        endWeek: 16,
        building: '',
        classroom: ''
      })
    },
    // 获取教室列表
    getClassrooms(building) {
      if (!building) return []
      const classrooms = []
      for (let i = 1; i <= 5; i++) {
        for (let j = 1; j <= 20; j++) {
          classrooms.push(`${building}${i}${String(j).padStart(2, '0')}`)
        }
      }
      return classrooms
    },
    // 教学楼变化时清空教室
    onBuildingChange(index) {
      this.data.classTimes[index].classroom = ''
    },
    // 删除上课时间
    removeClassTime(index) {
      this.data.classTimes.splice(index, 1)
    },
    // 初始化编辑器
    initEditor() {
      const editor = this.editor = new E('#editor')
      editor.config.height = 200
      editor.config.zIndex = 1600
      editor.config.onchange = this.contentChange
      // 菜单
      editor.config.excludeMenus = ['emoticon', 'video', 'fontName', 'todo', 'code']
      // 图片
      editor.config.uploadImgMaxSize = 2 * 1024 * 1024 // 2M
      editor.config.uploadImgAccept = ['jpg', 'jpeg', 'png']
      editor.config.uploadImgMaxLength = 1
      editor.config.showLinkImg = false
      editor.config.customUploadImg = this.insertPicToEditor
      // 创建
      editor.create()
    },
    contentChange(html) {
      this.data.description = html
    },
    insertPicToEditor(files, insertImgFn) {
      for (const file of files) {
        uploadPic(jsonObj2FormData({ file: file })).then(resp => {
          insertPicFn(resp.data)
        })
      }
    }
  }
}
</script>

<style scoped lang="scss">
.block {
  display: block;
}

.inliblock {
  display: inline-block;
}

.cover-img {
  width: 360px;
  height: 120px;
  background-color: #f5f7fa;
  line-height: 120px;
}

.into {
  width: 100px;
  text-align: right;
  padding-right: 12px;
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #606266;
}

</style>
