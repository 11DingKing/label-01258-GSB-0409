<template>
  <div class="paper-list">
    <div class="page-card">
      <div class="page-header">
        <h2>试卷列表</h2>
        <el-button type="primary" @click="router.push('/paper/edit')">
          <el-icon><Plus /></el-icon>
          创建试卷
        </el-button>
      </div>

      <el-table :data="paperList" v-loading="loading" stripe>
        <el-table-column prop="title" label="试卷名称" min-width="180" />
        <el-table-column prop="totalScore" label="总分" width="80" align="center">
          <template #default="{ row }">
            <span class="score-text">{{ row.totalScore || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长" width="80" align="center">
          <template #default="{ row }">
            {{ row.durationMinutes }}分钟
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="100" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="router.push(`/paper/edit/${row.id}`)">
              编辑
            </el-button>
            <el-button 
              v-if="row.status !== 'PUBLISHED'"
              size="small" 
              type="success"
              link
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button 
              v-else
              size="small" 
              type="warning"
              link
              @click="handleUnpublish(row)"
            >
              取消发布
            </el-button>
            <el-button 
              size="small" 
              type="danger"
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-button size="small" type="info" link @click="router.push(`/ranking/${row.id}`)">
              <el-icon><Trophy /></el-icon>
              排行榜
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPaperList, publishPaper, deletePaper, unpublishPaper } from '@/api'
import { Trophy } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const paperList = ref([])

// 格式化时间（24小时制）
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPaperList()
    paperList.value = res.data
  } finally {
    loading.value = false
  }
}

const handlePublish = async (row) => {
  await ElMessageBox.confirm('发布后学生可以参加考试，确定发布？', '提示', { 
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await publishPaper(row.id)
  ElMessage.success('发布成功')
  loadData()
}

const handleUnpublish = async (row) => {
  await ElMessageBox.confirm('取消发布后学生将无法参加考试，确定取消发布？', '提示', { 
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await unpublishPaper(row.id)
  ElMessage.success('已取消发布')
  loadData()
}

const handleDelete = async (row) => {
  const message = row.status === 'PUBLISHED' 
    ? '该试卷已发布，删除后相关的考试记录也会被删除，确定删除？'
    : '确定删除该试卷？'
  await ElMessageBox.confirm(message, '提示', { 
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await deletePaper(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.paper-list {
  .score-text {
    font-weight: 600;
    color: #409EFF;
  }
  
  :deep(.el-table) {
    .el-button + .el-button {
      margin-left: 8px;
    }
  }
}
</style>
