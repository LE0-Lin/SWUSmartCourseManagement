import { login, logout, getInfo } from '@/api/user'
import { resetRouter } from '@/router'
import store from '@/store'

const getDefaultState = () => {
  return {
    user: {},
    token: ''
  }
}

const state = getDefaultState()

const mutations = {
  RESET_STATE: (state) => {
    Object.assign(state, getDefaultState())
  },
  SET_USER: (state, user) => {
    state.user = user
  },
  SET_TOKEN: (state, token) => {
    state.token = token
  }
}

const actions = {
  // user login
  login({ commit, dispatch }, userInfo) {
    const { username, password } = userInfo
    return new Promise((resolve, reject) => {
      login({ username: username.trim(), password: password.trim() }).then(response => {
        const { data } = response
        commit('SET_USER', data)
        commit('SET_TOKEN', data.token)
        localStorage.setItem('token', data.token)
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // get user info
  getInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      getInfo().then(response => {
        const { data } = response
        if (!data) {
          return reject('验证失败，请重新登录')
        }
        commit('SET_USER', data)
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  setInfo({ commit }, userInfo) {
    commit('SET_USER', userInfo)
  },

  // user logout
  logout({ commit }) {
    return new Promise((resolve, reject) => {
      logout().then(() => {
        localStorage.removeItem('token')
        store.dispatch('resetRouters')
        resetRouter()
        commit('RESET_STATE')
        resolve()
      }).catch(error => {
        // 即使后端登出失败，前端也要清理状态
        localStorage.removeItem('token')
        store.dispatch('resetRouters')
        resetRouter()
        commit('RESET_STATE')
        resolve()
      })
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
