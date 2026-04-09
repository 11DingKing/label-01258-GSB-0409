import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/exam',
    children: [
      {
        path: 'llm',
        name: 'LlmConfig',
        component: () => import('@/views/LlmConfig.vue'),
        meta: { title: '大模型管理', admin: true }
      },
      {
        path: 'paper',
        name: 'PaperList',
        component: () => import('@/views/PaperList.vue'),
        meta: { title: '试卷管理', admin: true }
      },
      {
        path: 'paper/edit/:id?',
        name: 'PaperEdit',
        component: () => import('@/views/PaperEdit.vue'),
        meta: { title: '编辑试卷', admin: true }
      },
      {
        path: 'grading',
        name: 'Grading',
        component: () => import('@/views/Grading.vue'),
        meta: { title: '批卷管理', admin: true }
      },
      {
        path: 'exam',
        name: 'ExamList',
        component: () => import('@/views/ExamList.vue'),
        meta: { title: '考试中心' }
      },
      {
        path: 'exam/take/:recordId',
        name: 'ExamTake',
        component: () => import('@/views/ExamTake.vue'),
        meta: { title: '答题' }
      },
      {
        path: 'score',
        name: 'ScoreList',
        component: () => import('@/views/ScoreList.vue'),
        meta: { title: '我的成绩' }
      },
      {
        path: 'score/detail/:recordId',
        name: 'ScoreDetail',
        component: () => import('@/views/ScoreDetail.vue'),
        meta: { title: '成绩详情' }
      },
      {
        path: 'ranking/:paperId',
        name: 'Ranking',
        component: () => import('@/views/Ranking.vue'),
        meta: { title: '排行榜' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.public) {
    next()
    return
  }
  
  if (!userStore.token) {
    next('/login')
    return
  }
  
  if (to.meta.admin && userStore.role !== 'ADMIN') {
    next('/exam')
    return
  }
  
  next()
})

export default router
