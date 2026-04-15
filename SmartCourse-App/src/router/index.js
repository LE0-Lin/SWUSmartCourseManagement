import Vue from 'vue'
import Router from 'vue-router'
import Layout from '@/components/Layout'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/404',
      component: () => import('@/views/404'),
      meta: { index: 0, title: '404' },
      hidden: true
    },
    {
      path: '/login',
      component: () => import('@/views/login/index'),
      meta: { index: 0, title: 'Login' },
      hidden: true
    },
    {
      path: '/refresh',
      name: 'Refresh',
      component: () => import('@/components/common/refresh'),
      meta: { title: 'refresh' },
      hidden: true
    },
    {
      path: '/',
      component: Layout,
      children: [
        {
          path: '',
          name: 'Index',
          meta: { title: 'Home' },
          component: () => import('@/views/Index')
        }
      ]
    },
    {
      path: '/course',
      component: Layout,
      children: [
        {
          path: '/course/detail/:id',
          name: 'Course',
          meta: { title: 'Course' },
          component: () => import('@/views/course')
        },
        {
          path: '/course/s/:title',
          name: 'SearchByKeyword',
          meta: { title: 'Search' },
          component: () => import('@/views/search_keyword')
        },
        {
          path: '/course/sub/:subject',
          name: 'SearchBySubject',
          meta: { title: 'Category' },
          component: () => import('@/views/search_subject')
        },
        {
          path: '/course/tch/:teacher',
          name: 'SearchByTeacher',
          meta: { title: 'Teacher' },
          component: () => import('@/views/search_teacher')
        }
      ]
    },
    {
      path: '/user',
      component: Layout,
      redirect: '/user/profile',
      children: [
        {
          path: '/user/profile',
          name: 'Profile',
          meta: { title: 'Profile' },
          component: () => import('@/views/profile/index')
        },
        {
          path: '/user/schedule',
          name: 'Schedule',
          meta: { title: 'Schedule' },
          component: () => import('@/views/schedule/index')
        },
        {
          path: '/user/transcript',
          name: 'Transcript',
          meta: { title: 'Transcript' },
          component: () => import('@/views/grade/transcript')
        }
      ]
    },
    { path: '*', redirect: '/404', hidden: true }
  ]
})
