<template>
  <div class="exam-take" v-loading="loading">
    <template v-if="record">
      <!-- Header -->
      <div class="exam-header-bar">
        <div class="exam-title">{{ record.paperTitle }}</div>
        <div class="exam-timer" :class="{ warning: remainingMinutes < 10 }">
          <el-icon><Clock /></el-icon>
          剩余时间：{{ formatTime(remainingSeconds) }}
        </div>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          提交答卷
        </el-button>
      </div>

      <!-- Question Navigator -->
      <div class="question-nav">
        <div 
          v-for="(answer, index) in record.answers" 
          :key="answer.id"
          class="nav-item"
          :class="{ active: currentIndex === index, answered: answer.userAnswer }"
          @click="currentIndex = index"
        >
          {{ index + 1 }}
        </div>
      </div>

      <!-- Question Content -->
      <div class="question-panel">
        <div v-if="currentQuestion" class="question-content">
          <div class="question-header">
            <span class="q-num">第 {{ currentIndex + 1 }} / {{ record.answers.length }} 题</span>
            <el-tag :class="getTypeClass(currentQuestion.type)" size="small">
              {{ getTypeName(currentQuestion.type) }}
            </el-tag>
            <span class="q-score">{{ currentQuestion.score }} 分</span>
          </div>

          <div class="q-text">{{ currentQuestion.content }}</div>

          <!-- Choice Questions -->
          <div v-if="isChoiceType(currentQuestion.type)" class="options-area">
            <template v-if="currentQuestion.type === 'SINGLE_CHOICE'">
              <el-radio-group v-model="currentAnswer.userAnswer" class="option-group">
                <el-radio 
                  v-for="opt in parseOptions(currentQuestion.options)" 
                  :key="opt" 
                  :label="opt.charAt(0)"
                  class="option-item"
                >
                  {{ opt }}
                </el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="currentQuestion.type === 'MULTI_CHOICE'">
              <el-checkbox-group v-model="multiAnswer" class="option-group" @change="updateMultiAnswer">
                <el-checkbox 
                  v-for="opt in parseOptions(currentQuestion.options)" 
                  :key="opt" 
                  :label="opt.charAt(0)"
                  class="option-item"
                >
                  {{ opt }}
                </el-checkbox>
              </el-checkbox-group>
            </template>
          </div>

          <!-- True/False -->
          <div v-else-if="currentQuestion.type === 'TRUE_FALSE'" class="tf-area">
            <el-radio-group v-model="currentAnswer.userAnswer" class="option-group">
              <el-radio label="TRUE" class="option-item">
                A. 对
              </el-radio>
              <el-radio label="FALSE" class="option-item">
                B. 错
              </el-radio>
            </el-radio-group>
          </div>

          <!-- Text Answer -->
          <div v-else class="text-area">
            <el-input 
              v-model="currentAnswer.userAnswer" 
              type="textarea" 
              :rows="6" 
              placeholder="请输入你的答案..."
            />
          </div>

          <!-- Navigation -->
          <div class="nav-buttons">
            <el-button @click="prevQuestion" :disabled="currentIndex === 0">
              <el-icon><ArrowLeft /></el-icon>
              上一题
            </el-button>
            <el-button @click="nextQuestion" :disabled="currentIndex === record.answers.length - 1">
              下一题
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExamRecord, submitExam } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const record = ref(null)
const currentIndex = ref(0)
const remainingSeconds = ref(0)
const multiAnswer = ref([])

let timer = null

const currentAnswer = computed(() => record.value?.answers[currentIndex.value])
const currentQuestion = computed(() => currentAnswer.value?.question)
const remainingMinutes = computed(() => Math.floor(remainingSeconds.value / 60))

const typeMap = {
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
  ESSAY: '论述题'
}

const getTypeName = (type) => typeMap[type] || type
const getTypeClass = (type) => {
  const map = {
    SINGLE_CHOICE: 'question-type single',
    MULTI_CHOICE: 'question-type multi',
    TRUE_FALSE: 'question-type truefalse',
    FILL_BLANK: 'question-type fill',
    SHORT_ANSWER: 'question-type short',
    ESSAY: 'question-type essay'
  }
  return map[type] || ''
}

const isChoiceType = (type) => ['SINGLE_CHOICE', 'MULTI_CHOICE'].includes(type)

const parseOptions = (options) => {
  try {
    return JSON.parse(options)
  } catch {
    return []
  }
}

const formatTime = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) {
    return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return `${m}:${String(s).padStart(2, '0')}`
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getExamRecord(route.params.recordId)
    record.value = res.data

    if (record.value.status === 'SUBMITTED') {
      ElMessage.warning('该考试已提交')
      router.push('/score')
      return
    }

    // Calculate remaining time
    const startTime = new Date(record.value.startTime).getTime()
    const duration = record.value.answers[0]?.question?.paperId 
      ? 120 * 60 * 1000 // Default 120 minutes
      : 120 * 60 * 1000
    const endTime = startTime + duration
    remainingSeconds.value = Math.max(0, Math.floor((endTime - Date.now()) / 1000))

    startTimer()
  } finally {
    loading.value = false
  }
}

const startTimer = () => {
  timer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      clearInterval(timer)
      ElMessage.warning('考试时间到，自动提交答卷')
      handleSubmit()
    }
  }, 1000)
}

const prevQuestion = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

const nextQuestion = () => {
  if (currentIndex.value < record.value.answers.length - 1) {
    currentIndex.value++
  }
}

const updateMultiAnswer = () => {
  currentAnswer.value.userAnswer = multiAnswer.value.sort().join(',')
}

// Watch for multi-choice sync
watch(currentIndex, () => {
  if (currentQuestion.value?.type === 'MULTI_CHOICE') {
    multiAnswer.value = currentAnswer.value.userAnswer 
      ? currentAnswer.value.userAnswer.split(',') 
      : []
  }
}, { immediate: true })

const handleSubmit = async () => {
  const unanswered = record.value.answers.filter(a => !a.userAnswer).length
  if (unanswered > 0) {
    await ElMessageBox.confirm(
      `还有 ${unanswered} 道题未作答，确定提交？`,
      '提示',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } else {
    await ElMessageBox.confirm('确定提交答卷？', '提示', { type: 'info', confirmButtonText: '确定', cancelButtonText: '取消' })
  }

  submitting.value = true
  try {
    await submitExam({
      examRecordId: record.value.id,
      answers: record.value.answers.map(a => ({
        questionId: a.questionId,
        answer: a.userAnswer
      }))
    })
    ElMessage.success('提交成功')
    router.push('/score')
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.exam-take {
  .exam-header-bar {
    background: #fff;
    padding: 16px 24px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    gap: 24px;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    .exam-title {
      font-size: 18px;
      font-weight: 600;
      flex: 1;
    }

    .exam-timer {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      color: #409EFF;
      font-weight: 500;

      &.warning {
        color: #F56C6C;
        animation: blink 1s infinite;
      }
    }
  }

  .question-nav {
    background: #fff;
    padding: 16px;
    border-radius: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 16px;

    .nav-item {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      background: #F5F7FA;
      cursor: pointer;
      font-size: 14px;
      transition: all 0.2s;

      &:hover {
        background: #ECF5FF;
      }

      &.active {
        background: #409EFF;
        color: #fff;
      }

      &.answered {
        background: #E1F3D8;
        color: #67C23A;

        &.active {
          background: #67C23A;
          color: #fff;
        }
      }
    }
  }

  .question-panel {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    min-height: 400px;

    .question-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;
      padding-bottom: 16px;
      border-bottom: 1px solid #EBEEF5;

      .q-num {
        font-weight: 600;
        font-size: 16px;
      }

      .q-score {
        color: #E6A23C;
        font-weight: 500;
      }
    }

    .q-text {
      font-size: 16px;
      line-height: 1.8;
      margin-bottom: 24px;
      color: #303133;
    }

    .option-group {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .option-item {
        padding: 12px 16px;
        background: #F5F7FA;
        border-radius: 8px;
        margin: 0;

        &:hover {
          background: #ECF5FF;
        }

        :deep(.el-radio__label),
        :deep(.el-checkbox__label) {
          font-size: 15px;
        }
      }
    }

    .text-area {
      :deep(.el-textarea__inner) {
        font-size: 15px;
        line-height: 1.8;
      }
    }

    .nav-buttons {
      display: flex;
      justify-content: space-between;
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid #EBEEF5;
    }
  }
}

@keyframes blink {
  50% { opacity: 0.5; }
}
</style>
