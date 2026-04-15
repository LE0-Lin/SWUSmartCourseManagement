<template>
  <div class="app-container">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>智能排课系统</span>
        </div>
      </template>

      <el-form :model="searchParams" size="small" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="学期：">
              <el-select v-model="searchParams.semester" placeholder="选择学期" style="width: 100%">
                <el-option label="上学期" value="spring" />
                <el-option label="下学期" value="autumn" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="院系：">
              <el-select v-model="searchParams.department" placeholder="选择院系" style="width: 100%">
                <el-option label="计算机信息与技术学院" value="computer" />
                <el-option label="文学院" value="literature" />
                <el-option label="数学学院" value="mathematics" />
                <el-option label="体育学院" value="sports" />
                <el-option label="医学院" value="medical" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="专业：">
              <el-select v-model="searchParams.major" placeholder="选择专业" style="width: 100%">
                <el-option v-for="major in majorOptions" :key="major.value" :label="major.label" :value="major.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item>
              <el-button type="primary" style="margin-right: 10px" @click="search">
                <i class="el-icon-search" />
                搜索
              </el-button>
              <el-button type="success" @click="autoSchedule">
                <i class="el-icon-magic-stick" />
                智能排课
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-table :data="courseList" stripe style="width: 100%">
        <el-table-column prop="title" label="课程名称" width="200" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="teacherName" label="教师" width="120" />
        <el-table-column prop="department" label="院系" width="150" />
        <el-table-column prop="major" label="专业" width="150" />
        <el-table-column prop="courseType" label="课程类型" width="100">
          <template slot-scope="scope">
            {{ scope.row.courseType === 'public' ? '公共课' : '专业课' }}
          </template>
        </el-table-column>
        <el-table-column prop="classTimes" label="上课时间" min-width="300">
          <template slot-scope="scope">
            <div v-if="scope.row.classTimes && scope.row.classTimes.length > 0">
              <div v-for="(time, index) in scope.row.classTimes" :key="index">
                周{{ time.weekday }} {{ getPeriodLabel(time.period) }} (第{{ time.startWeek }}-{{ time.endWeek }}周) {{ time.building || '' }}{{ time.classroom || '无' }}
              </div>
            </div>
            <div v-else>未设置</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="primary" size="small" style="margin-right: 5px" @click="editSchedule(scope.row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="clearSchedule(scope.row)">
              清空
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" style="margin-top: 20px">
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
    </el-card>

    <!-- 排课编辑对话框 -->
    <el-dialog title="编辑排课" :visible.sync="scheduleDialogVisible" width="800px">
      <el-form :model="currentCourse" size="small" label-width="100px">
        <el-form-item label="课程名称：">
          <el-input v-model="currentCourse.title" disabled />
        </el-form-item>
        <el-form-item label="教师：">
          <el-input v-model="currentCourse.teacherName" disabled />
        </el-form-item>
        <el-form-item label="上课时间：">
          <el-button type="primary" size="small" style="margin-bottom: 10px" @click="addClassTime">
            <i class="el-icon-plus" />
            添加上课时间
          </el-button>
          <div v-for="(time, index) in currentCourse.classTimes" :key="index" class="class-time-item">
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
                    <i class="el-icon-delete" />
                    删除
                  </el-button>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSchedule">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>

export default {
  name: 'EduScheduleList',
  data() {
    return {
      searchParams: {
        current: 1,
        pageSize: 10,
        semester: '',
        department: '',
        major: ''
      },
      total: 0,
      courseList: [],
      majorOptions: [],
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
      scheduleDialogVisible: false,
      currentCourse: {
        id: 0,
        title: '',
        teacherName: '',
        classTimes: []
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      // 模拟数据
      this.courseList = [
        {
          id: 1,
          title: '高等数学',
          credit: 4,
          teacherName: '张老师',
          department: '数学学院',
          major: '数学与应用数学',
          courseType: 'public',
          classTimes: [
            { weekday: '1', period: '12', startWeek: 1, endWeek: 16 },
            { weekday: '3', period: '34', startWeek: 1, endWeek: 16 }
          ]
        },
        {
          id: 2,
          title: '大学英语',
          credit: 4,
          teacherName: '李老师',
          department: '文学院',
          major: '汉语言文学',
          courseType: 'public',
          classTimes: [
            { weekday: '2', period: '56', startWeek: 1, endWeek: 16 },
            { weekday: '4', period: '78', startWeek: 1, endWeek: 16 }
          ]
        },
        {
          id: 3,
          title: '数据结构',
          credit: 3,
          teacherName: '王老师',
          department: '计算机信息与技术学院',
          major: '计算机科学与技术',
          courseType: 'major',
          classTimes: []
        }
      ]
      this.total = this.courseList.length
    },
    search() {
      this.searchParams.current = 1
      this.getList()
    },
    autoSchedule() {
      this.$message.success('智能排课已完成')
      // 模拟智能排课
      this.courseList.forEach(course => {
        if (!course.classTimes || course.classTimes.length === 0) {
          course.classTimes = [
            { weekday: '1', period: '12', startWeek: 1, endWeek: 16 }
          ]
        }
      })
    },
    editSchedule(course) {
      this.currentCourse = JSON.parse(JSON.stringify(course))
      this.scheduleDialogVisible = true
    },
    saveSchedule() {
      // 保存排课信息
      this.$message.success('保存成功')
      this.scheduleDialogVisible = false
      this.getList()
    },
    clearSchedule(course) {
      this.$confirm('确定要清空该课程的上课时间吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        course.classTimes = []
        this.$message.success('清空成功')
      })
    },
    addClassTime() {
      this.currentCourse.classTimes.push({
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
      this.currentCourse.classTimes[index].classroom = ''
    },
    removeClassTime(index) {
      this.currentCourse.classTimes.splice(index, 1)
    },
    getPeriodLabel(period) {
      const periodMap = {
        '12': '1-2节 (8:00-9:40)',
        '34': '3-4节 (10:00-11:40)',
        '56': '5-6节 (14:00-15:40)',
        '78': '7-8节 (16:00-17:40)',
        '910': '9-10节 (19:00-20:40)'
      }
      return periodMap[period] || period
    }
  },
  watch: {
    'searchParams.department'(val) {
      this.majorOptions = this.departmentMajorMap[val] || []
      this.searchParams.major = ''
    }
  }
}
</script>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.class-time-item {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}
</style>
