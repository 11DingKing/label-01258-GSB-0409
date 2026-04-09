<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon :size="28" color="#409EFF"><Reading /></el-icon>
        <span>AI 考试系统</span>
      </div>

      <el-menu
        :default-active="route.path"
        router
        class="sidebar-menu"
      >
        <template v-if="userStore.isAdmin">
          <el-menu-item index="/llm">
            <el-icon><Setting /></el-icon>
            <span>大模型管理</span>
          </el-menu-item>
          <el-menu-item index="/paper">
            <el-icon><Document /></el-icon>
            <span>试卷管理</span>
          </el-menu-item>
          <el-menu-item index="/grading">
            <el-icon><Edit /></el-icon>
            <span>批卷管理</span>
          </el-menu-item>
        </template>

        <el-menu-item index="/exam">
          <el-icon><Notebook /></el-icon>
          <span>考试中心</span>
        </el-menu-item>
        <el-menu-item index="/score">
          <el-icon><DataLine /></el-icon>
          <span>我的成绩</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ route.meta.title }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" class="avatar">
                {{ userStore.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="nickname">{{ userStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-tag size="small" :type="userStore.isAdmin ? 'danger' : 'info'">
                    {{ userStore.isAdmin ? '管理员' : '学生' }}
                  </el-tag>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background: #fff;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border-bottom: 1px solid #EBEEF5;

    span {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .sidebar-menu {
    flex: 1;
    border-right: none;
    padding: 8px;

    .el-menu-item {
      border-radius: 8px;
      margin-bottom: 4px;

      &.is-active {
        background: #ECF5FF;
        color: #409EFF;
      }
    }
  }
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;

  .page-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    .avatar {
      background: #409EFF;
      color: #fff;
    }

    .nickname {
      color: #606266;
    }
  }
}

.main-content {
  background: #F5F7FA;
  padding: 24px;
  overflow-y: auto;
}
</style>
