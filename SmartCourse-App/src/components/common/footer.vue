<template>
  <el-row type="flex" justify="center" class="footer">
    <el-col :span="20">
      <div>Copyright © 2026 西大智能课程管理系统</div>
      <div style="margin-top:5px;">作者：
        <el-link :underline="false" href="#" target="_blank">
          智能课程管理系统开发小组
        </el-link>
      </div>
    </el-col>
  </el-row>
</template>

<script>
export default {
  name: 'Footer'
}
</script>

<style scoped lang="scss">
.footer {
  margin-top: 36px;
  padding: 20px;
  text-align: center;
  background-color:#545c64;

  * {
    color: #dcdcdc;
    font-size: 13px;
  }
}
</style>
