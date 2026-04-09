<template>
  <div class="llm-config">
    <div class="page-card">
      <div class="page-header">
        <h2>大模型配置</h2>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>
          添加配置
        </el-button>
      </div>

      <div class="tip-box">
        <el-icon><InfoFilled /></el-icon>
        <span>提示：如果没有有效的 API Key，系统将自动使用模拟模式进行出题和批卷。</span>
      </div>

      <el-table :data="configList" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无配置，请添加大模型配置" />
        </template>
        <el-table-column prop="name" label="配置名称" width="150" />
        <el-table-column prop="provider" label="服务商" width="120">
          <template #default="{ row }">
            <el-tag>{{ getProviderName(row.provider) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="模型" width="180" />
        <el-table-column prop="isDefault" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success">默认</el-tag>
            <el-tag v-else type="info">备用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="testConnection(row.id)" :loading="testingId === row.id">
              测试连接
            </el-button>
            <el-button size="small" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="success" @click="setDefault(row.id)" :disabled="row.isDefault">
              设为默认
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" :disabled="row.isDefault">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑配置' : '添加配置'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="left">
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="form.name" placeholder="给这个配置起个名字，如：我的GPT" />
        </el-form-item>
        
        <el-form-item label="服务商" prop="provider">
          <el-radio-group v-model="form.provider" @change="onProviderChange">
            <el-radio-button value="MOCK">模拟模式（免费）</el-radio-button>
            <el-radio-button value="DMXAPI">DMXAPI</el-radio-button>
            <el-radio-button value="DEEPSEEK">DeepSeek</el-radio-button>
            <el-radio-button value="OPENAI">OpenAI</el-radio-button>
            <el-radio-button value="QWEN">通义千问</el-radio-button>
            <el-radio-button value="ZHIPU">智谱AI</el-radio-button>
            <el-radio-button value="OTHER">其他</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.provider !== 'MOCK'" label="API 密钥" prop="apiKey">
          <el-input v-model="form.apiKey" placeholder="输入从服务商获取的 API Key" show-password />
          <div class="form-tip">{{ getApiKeyTip(form.provider) }}</div>
          <el-alert v-if="isApiKeyInvalid" type="warning" :closable="false" style="margin-top: 8px">
            <template #title>
              <span>⚠️ 未填写有效的 API Key，保存后系统将自动使用模拟模式</span>
            </template>
          </el-alert>
        </el-form-item>

        <el-alert v-if="form.provider === 'MOCK'" type="success" :closable="false" style="margin-bottom: 16px">
          <template #title>
            <strong>模拟模式</strong> - 无需 API Key，系统将自动生成模拟题目和批改结果，适合功能测试。
          </template>
        </el-alert>

        <el-form-item v-if="form.provider !== 'MOCK'" label="模型名称" prop="modelName">
          <el-select v-model="form.modelName" placeholder="选择或输入模型名称" filterable allow-create style="width: 100%">
            <el-option v-for="model in getModelOptions(form.provider)" :key="model" :label="model" :value="model" />
          </el-select>
        </el-form-item>

        <el-collapse v-if="form.provider !== 'MOCK'" v-model="showAdvanced">
          <el-collapse-item title="高级设置（可选）" name="advanced">
            <el-form-item label="API 地址">
              <el-input v-model="form.baseUrl" :placeholder="getDefaultUrl(form.provider)" />
              <div class="form-tip">留空将使用默认地址</div>
            </el-form-item>
            <el-form-item label="温度参数">
              <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input />
              <div class="form-tip">值越高回答越随机，建议 0.7</div>
            </el-form-item>
            <el-form-item label="最大长度">
              <el-input-number v-model="form.maxTokens" :min="100" :max="8000" :step="100" style="width: 100%" />
              <div class="form-tip">单次回复的最大 token 数</div>
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLlmList, saveLlmConfig, deleteLlmConfig, setLlmDefault, testLlmConnection } from '@/api'

const loading = ref(false)
const configList = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const testingId = ref(null)
const showAdvanced = ref([])

const formRef = ref()
const form = ref({
  id: null,
  name: '',
  provider: 'DEEPSEEK',
  apiKey: '',
  baseUrl: '',
  modelName: 'deepseek-chat',
  temperature: 0.7,
  maxTokens: 2000
})

// 判断 API Key 是否无效（为空、太短、或是模拟 key）
const isApiKeyInvalid = computed(() => {
  const apiKey = form.value.apiKey
  if (!apiKey || apiKey.trim() === '') return true
  if (apiKey === 'mock-api-key') return true
  // OpenAI 的 key 以 sk- 开头，至少 20 位
  // 其他服务商的 key 一般也至少 10 位以上
  if (apiKey.trim().length < 10) return true
  return false
})

const rules = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择服务商', trigger: 'change' }],
  apiKey: [{ 
    validator: (rule, value, callback) => {
      // 模拟模式不需要验证
      if (form.value.provider === 'MOCK') {
        callback()
        return
      }
      // 非模拟模式允许为空，但会提示使用模拟模式
      callback()
    },
    trigger: 'blur' 
  }],
  modelName: [{ 
    required: true,
    validator: (rule, value, callback) => {
      if (form.value.provider === 'MOCK' || value) {
        callback()
      } else {
        callback(new Error('请选择或输入模型名称'))
      }
    },
    trigger: 'blur' 
  }]
}

const providerNames = {
  MOCK: '模拟模式',
  DMXAPI: 'DMXAPI',
  OPENAI: 'OpenAI',
  AZURE: 'Azure',
  DEEPSEEK: 'DeepSeek',
  QWEN: '通义千问',
  ZHIPU: '智谱AI',
  OTHER: '其他'
}

const getProviderName = (provider) => providerNames[provider] || provider

const getModelOptions = (provider) => {
  const models = {
    DMXAPI: ['qwen-max', 'qwen-turbo', 'GLM-4-Flash', 'gpt-3.5-turbo'],
    OPENAI: ['gpt-4', 'gpt-4-turbo', 'gpt-3.5-turbo'],
    DEEPSEEK: ['deepseek-chat', 'deepseek-coder'],
    QWEN: ['qwen-turbo', 'qwen-plus', 'qwen-max'],
    ZHIPU: ['glm-4', 'glm-4-flash', 'glm-3-turbo'],
    OTHER: ['gpt-4', 'gpt-3.5-turbo']
  }
  return models[provider] || models.OTHER
}

const getDefaultUrl = (provider) => {
  const urls = {
    DMXAPI: 'https://www.dmxapi.cn/v1',
    OPENAI: 'https://api.openai.com/v1',
    DEEPSEEK: 'https://api.deepseek.com/v1',
    QWEN: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    ZHIPU: 'https://open.bigmodel.cn/api/paas/v4'
  }
  return urls[provider] || '请输入 API 地址'
}

const getApiKeyTip = (provider) => {
  const tips = {
    DMXAPI: '从 dmxapi.cn 获取 API Key',
    OPENAI: '从 platform.openai.com 获取',
    DEEPSEEK: '从 platform.deepseek.com 获取',
    QWEN: '从阿里云 DashScope 控制台获取',
    ZHIPU: '从 open.bigmodel.cn 获取'
  }
  return tips[provider] || '从服务商控制台获取 API Key'
}

const onProviderChange = (provider) => {
  if (provider === 'MOCK') {
    form.value.modelName = 'mock-model'
    form.value.apiKey = 'mock-api-key'
    form.value.baseUrl = ''
    return
  }
  const defaultModels = {
    DMXAPI: 'qwen-max',
    OPENAI: 'gpt-4',
    DEEPSEEK: 'deepseek-chat',
    QWEN: 'qwen-turbo',
    ZHIPU: 'glm-4',
    OTHER: 'gpt-4'
  }
  form.value.modelName = defaultModels[provider] || 'gpt-4'
  form.value.baseUrl = getDefaultUrl(provider) || ''
  form.value.apiKey = ''
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLlmList()
    configList.value = res.data
  } finally {
    loading.value = false
  }
}

const openDialog = (row = null) => {
  showAdvanced.value = []
  if (row) {
    form.value = { ...row }
  } else {
    form.value = {
      id: null,
      name: '',
      provider: 'MOCK',
      apiKey: 'mock-api-key',
      baseUrl: '',
      modelName: 'mock-model',
      temperature: 0.7,
      maxTokens: 2000
    }
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await saveLlmConfig(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该配置？', '提示', { 
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await deleteLlmConfig(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const setDefault = async (id) => {
  await setLlmDefault(id)
  ElMessage.success('设置成功')
  loadData()
}

const testConnection = async (id) => {
  testingId.value = id
  try {
    const res = await testLlmConnection(id)
    ElMessage.success(res.data)
  } finally {
    testingId.value = null
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.llm-config {
  .tip-box {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #ecf5ff;
    border-radius: 8px;
    margin-bottom: 16px;
    color: #409eff;
    font-size: 14px;
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }

  :deep(.el-radio-group) {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  :deep(.el-collapse) {
    border: none;
    margin-top: 16px;
  }

  :deep(.el-collapse-item__header) {
    background: #f5f7fa;
    padding: 0 12px;
    border-radius: 4px;
    font-size: 13px;
    color: #606266;
  }

  :deep(.el-collapse-item__content) {
    padding: 16px 0 0;
  }

  :deep(.el-slider) {
    padding-right: 50px;
  }
}
</style>