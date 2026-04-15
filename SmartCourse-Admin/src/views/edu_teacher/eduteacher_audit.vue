<!-- 讲师审核 -->
<template>
  <div class="app-container">
    <div class="search-box">
      <el-form ref="searchParamsForm" :inline="true" :model="searchParams" size="small">
        <el-form-item label="讲师名称：" prop="name">
          <el-input v-model="searchParams.name" placeholder="输入讲师名称搜索" />
        </el-form-item>
        <el-form-item label="手机号：" prop="mobile">
          <el-input v-model="searchParams.mobile" placeholder="输入手机号搜索" />
        </el-form-item>
        <el-form-item label="审核状态：" prop="status">
          <el-select v-model="searchParams.status" style="width: 130px">
            <el-option label="审核中" :value="'AUDITING'" />
            <el-option label="不通过" :value="'NOT_PASS'" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">查 询</el-button>
          <el-button @click="resetSearchParams('searchParamsForm')">重 置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="blog-list">
      <el-table
        ref="listTable"
        v-loading="tableDataLoading"
        :data="listData"
        tooltip-effect="dark"
        :header-cell-style="{fontWeight:'normal', color:'#666'}"
      >
        <el-table-column type="index" />
        <el-table-column prop="name" label="讲师名称" />
        <el-table-column prop="mobile" label="手机号" />
        <el-table-column prop="email" label="邮箱地址" />
        <el-table-column prop="division" label="分成比例" width="80" align="center" />
        <el-table-column prop="enable" label="审核状态" width="180" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status==='AUDITING'" type="" size="small">待审核</el-tag>
            <el-tag v-if="scope.row.status==='NOT_PASS'" type="danger" size="small">不通过</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template slot-scope="scope">
            <el-button type="primary" size="mini" @click="openUpdateProfileDialog(scope.$index)">修改</el-button>
            <el-popconfirm
              style="margin-left: 10px"
              placement="top-end"
              confirm-button-text="确定"
              cancel-button-text="取消"
              icon="el-icon-info"
              icon-color="red"
              :title="`你要通过讲师[${scope.row.name}]的审核吗？`"
              @confirm="pass(scope.$index)"
            >
              <el-button slot="reference" type="success" size="mini">通过</el-button>
            </el-popconfirm>
            <el-popconfirm
              style="margin-left: 10px"
              placement="top-end"
              confirm-button-text="确定"
              cancel-button-text="取消"
              icon="el-icon-info"
              icon-color="red"
              :title="`你要永久删除讲师[${scope.row.name}]吗？`"
              @confirm="deleteUser(scope.$index)"
            >
              <el-button slot="reference" type="danger" size="mini">删除</el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="pagination">
      <el-pagination
        background
        :page-sizes="[10, 20, 30, 40,50]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size.sync="searchParams.pageSize"
        :current-page.sync="searchParams.current"
        @size-change="getList"
        @current-change="getList"
      />
    </div>
    <!-- 修改账户信息 -->
    <el-dialog
      :title.sync="updateProfileDialogTitle"
      :visible.sync="updateProfileDialogVisible"
      destroy-on-close
    >
      <v-eduteacher-add-update ref="UpdateDialog" />
      <span slot="footer">
        <el-button type="primary" size="small" @click="updateProfile">更 新</el-button>
        <el-button size="small" @click="updateProfileDialogVisible = false">取 消</el-button>
      </span>
    </el-dialog>

  </div>
</template>

<script>

import { list, deleteUser, updateProfile, pass } from '@/api/teacher'

export default {
  name: 'EduTeacherAudit',
  components: {
    'v-eduteacher-add-update': () => import('@/views/edu_teacher/eduteacher_add_update')
  },
  data() {
    return {
      searchParams: {
        current: 1,
        pageSize: 10,
        name: '',
        mobile: '',
        status: 'AUDITING'
      },
      total: 0,
      listData: [{}],
      tableDataLoading: false,
      currentOperationIndex: 0,
      // 修改
      updateProfileDialogVisible: false,
      updateProfileDialogTitle: '',

    }
  },

  created() {
    this.getList()
  },
  methods: {
    resetSearchParams(formName) {
      this.$refs[formName].resetFields()
      this.searchParams.current = 1
      this.getList()
    },
    setIndexAndGetListData(index) {
      this.currentOperationIndex = index
      return this.listData[this.currentOperationIndex]
    },
    openUpdateProfileDialog(index) {
      this.updateProfileDialogVisible = true
      const user = this.setIndexAndGetListData(index)
      this.updateProfileDialogTitle = `更改讲师[${user.name}]信息`
      setTimeout(function() {
        this.$refs.UpdateDialog.setData(user)
      }.bind(this), 100)
    },

    // 获取列表信息
    getList() {
      this.tableDataLoading = true
      list(this.searchParams).then(resp => {
        this.total = resp.data.total
        this.listData = resp.data.list
        this.tableDataLoading = false
      })
      this.currentOperationIndex = 0
    },
    // 修改信息
    updateProfile() {
      const params = this.$refs.UpdateDialog.getData()
      updateProfile(params).then(resp => {
        this.$message.success(resp.message)
        this.updateProfileDialogVisible = false
        this.getList()
      })
    },
    // 通过审核
    pass(index) {
      const user = this.setIndexAndGetListData(index)
      pass(user.id).then(resp => {
        this.$message.success(`讲师[${user.name}]已通过审核`)
        this.getList()
      })
    },
    // 删除用户
    deleteUser(index) {
      const user = this.setIndexAndGetListData(index)
      deleteUser(user.id).then(resp => {
        this.$message.success(resp.message)
        this.getList()
      })
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
