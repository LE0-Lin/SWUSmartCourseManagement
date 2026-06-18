<template>
  <el-row type="flex" justify="center" class="header">
    <el-col :span="20" class="nav-content">
      <router-link to="/" class="logo">
        <span class="logo-full">西大智能课程管理系统</span>
        <span class="logo-short">西大课程系统</span>
      </router-link>
      <el-input
        ref="SearchInput"
        v-model.trim="searchInput"
        size="small"
        class="nav-search"
        placeholder="搜索课程..."
        @keyup.enter.native="search"
      >
        <el-button slot="append" icon="el-icon-search" style="color: #fff" @click="search" />
      </el-input>
      <div class="nav-right">
        <a class="portal-entry" href="http://localhost:9527">
          <i class="el-icon-s-home" />
          <span class="portal-label">统一门户</span>
        </a>
        <el-link
          v-if="user===null || Object.keys(user).length===0"
          :underline="false"
          class="nav-item"
          @click="$login"
        >登录/注册</el-link>
        <el-dropdown v-else class="avatar-container" trigger="hover">
          <div class="avatar-wrapper">
            <el-avatar :size="32" :src="user.avatar" fit="contain" />
            <span class="name">{{ user.nickname || '' }}</span>
            <i class="el-icon-caret-bottom" />
          </div>
          <el-dropdown-menu slot="dropdown" class="user-dropdown">
            <router-link :to="{name: 'Profile'}">
              <el-dropdown-item icon="el-icon-user">
                个人中心
              </el-dropdown-item>
            </router-link>
            <router-link :to="{name: 'Schedule'}">
              <el-dropdown-item icon="el-icon-date">
                我的课表
              </el-dropdown-item>
            </router-link>
            <router-link :to="{name: 'Transcript'}">
              <el-dropdown-item icon="el-icon-notebook-2">
                我的成绩单
              </el-dropdown-item>
            </router-link>
            <router-link :to="{name: 'SmartAdvisor'}">
              <el-dropdown-item icon="el-icon-data-analysis">
                智能毕业路径
              </el-dropdown-item>
            </router-link>
            <el-dropdown-item divided icon="el-icon-switch-button" @click.native="logout">
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </el-col>
  </el-row>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'Header',
  data() {
    return {
      loginFormVisible: false,
      searchInput: ''
    }
  },
  computed: {
    ...mapGetters(['user'])
  },
  methods: {
    async logout() {
      try {
        await this.$store.dispatch('user/logout')
        await this.$router.push({ name: 'Index' })
        // eslint-disable-next-line no-empty
      } catch (ignore) { }
    },
    search() {
      if (!this.searchInput) {
        this.$refs.SearchInput.focus()
        return
      }
      // const { href } = this.$router.resolve({
      //   name: 'SearchByKeyword',
      //   params: { title: this.searchInput }
      // })
      // window.open(href, '_blank')
      this.$router.push(
        {
          name: 'SearchByKeyword',
          params: { title: this.searchInput }
        }
      )
      this.searchInput = ''
    }
  }
}
</script>

<style lang="scss">
.nav-search {
  width: 240px;

  .el-input-group__append {
    background-color: #409eff;
    border: none;
    color: #fff;
  }
}
</style>

<style scoped lang="scss">
.header {
  min-height: 60px;
  background-color: #fff;

  .nav-content {
    display: grid;
    grid-template-columns: minmax(220px, 1fr) 240px minmax(220px, 1fr);
    gap: 24px;
    align-items: center;
    width: calc(100% - 48px);
    max-width: 1440px;
    min-height: 60px;
  }

  .nav-item {
    margin: 0 20px;
    font-size: 16px;

    &:first-child {
      margin-left: 0;
    }

    &:last-child {
      margin-right: 0;
    }
  }

  .nav-right {
    display: flex;
    align-items: center;
    justify-self: end;
  }

  .portal-entry {
    margin-right: 22px;
    color: #606266;
    font-size: 14px;
    text-decoration: none;
    transition: color .25s;

    &:hover {
      color: #409eff;
    }

    i {
      margin-right: 5px;
    }
  }

  .logo {
    justify-self: start;
    color: #409eff;
    font-size: 26px;
    line-height: 1;
    text-decoration: none;
    white-space: nowrap;
  }

  .logo-short {
    display: none;
  }

  .avatar-container {
    .avatar-wrapper {
      cursor: pointer;
      display: flex;
      align-items: center;

      &:hover {
        .el-icon-caret-bottom {
          transform: rotate(180deg);
        }
      }

      .name {
        margin: 0 5px 0 10px;
      }

      .el-icon-caret-bottom {
        transition: all .7s;
        font-size: 14px;
      }
    }
  }
}

@media (max-width: 1000px) {
  .header {
    .nav-content {
      grid-template-columns: minmax(150px, 1fr) 200px auto;
      gap: 16px;
    }

    .logo {
      font-size: 22px;
    }

    .logo-full,
    .portal-label,
    .avatar-container .avatar-wrapper .name {
      display: none;
    }

    .logo-short {
      display: inline;
    }

    .portal-entry {
      margin-right: 14px;
      font-size: 18px;

      i {
        margin-right: 0;
      }
    }
  }

  .nav-search {
    width: 200px;
  }
}

@media (max-width: 600px) {
  .header {
    .nav-content {
      grid-template-areas:
        "logo right"
        "search search";
      grid-template-columns: 1fr auto;
      gap: 8px 12px;
      width: calc(100% - 24px);
      padding: 10px 0;
    }

    .logo {
      grid-area: logo;
      font-size: 20px;
    }

    .nav-search {
      grid-area: search;
      width: 100%;
    }

    .nav-right {
      grid-area: right;
    }
  }
}

</style>
