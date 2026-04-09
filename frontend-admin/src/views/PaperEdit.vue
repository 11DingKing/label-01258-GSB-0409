<template>
  <div class="paper-edit">
    <!-- Basic Info -->
    <div class="form-section">
      <div class="section-title">基本信息</div>
      <el-form ref="basicFormRef" :model="paper" :rules="basicRules" label-width="100px">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="试卷名称" prop="title">
              <el-input v-model="paper.title" placeholder="请输入试卷名称" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="考试时长" prop="durationMinutes">
              <el-input-number v-model="paper.durationMinutes" :min="10" :max="300" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="总分">
              <el-tag size="large">{{ totalScore }} 分</el-tag>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="试卷描述">
          <el-input v-model="paper.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
    </div>

    <!-- AI Generate -->
    <div class="form-section">
      <div class="section-title">
        <span>AI 辅助出题</span>
        <el-tag type="success" size="small">大模型驱动</el-tag>
      </div>
      <el-form :model="aiForm" label-width="100px">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="学科">
              <el-select v-model="aiForm.subject" placeholder="请选择学科" style="width: 100%" @change="onSubjectChange">
                <el-option v-for="s in subjectList" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="知识点">
              <el-input v-model="aiForm.topic" :placeholder="topicPlaceholder" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度">
              <el-select v-model="aiForm.difficulty" style="width: 100%">
                <el-option label="简单" value="EASY" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="困难" value="HARD" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- 题型选择卡片 -->
        <div v-if="!aiForm.subject" class="empty-subject-tip">
          <el-icon><InfoFilled /></el-icon>
          请先选择学科，系统将显示该学科对应的题型
        </div>
        <div v-else class="question-type-cards">
          <div 
            class="type-card" 
            v-for="item in availableQuestionTypes" 
            :key="item.key"
          >
            <div class="type-icon" :style="{ background: item.color }">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
            <div class="type-info">
              <span class="type-name">{{ item.name }}</span>
              <span class="type-score">{{ item.score }}分/题</span>
            </div>
            <el-input-number 
              v-model="aiForm[item.key]" 
              :min="0" 
              :max="item.max" 
              size="small"
              controls-position="right"
            />
          </div>
        </div>

        <el-form-item style="margin-top: 16px;">
          <el-button type="success" @click="handleAiGenerate" :loading="aiGenerating">
            <el-icon><MagicStick /></el-icon>
            AI 生成题目
          </el-button>
          <span class="generate-tip" v-if="aiTotalCount > 0">
            将生成 {{ aiTotalCount }} 道题目，预计 {{ aiTotalScore }} 分
          </span>
        </el-form-item>
      </el-form>
    </div>

    <!-- Questions -->
    <div class="form-section">
      <div class="section-title">
        <span>题目列表 ({{ paper.questions.length }} 题)</span>
        <div class="title-actions">
          <span v-if="paper.questions.length > 1" class="drag-tip">
            <el-icon><Rank /></el-icon>
            <span>拖拽排序</span>
          </span>
          <el-button type="primary" size="small" @click="addQuestion">
            <el-icon><Plus /></el-icon>
            手动添加
          </el-button>
        </div>
      </div>

      <div v-if="paper.questions.length === 0" class="empty-tip">
        暂无题目，请使用 AI 生成或手动添加
      </div>

      <draggable 
        v-model="paper.questions" 
        item-key="id"
        handle=".drag-handle"
        animation="200"
        ghost-class="ghost"
      >
        <template #item="{ element: q, index }">
          <div class="question-item">
            <div class="question-header">
              <el-icon class="drag-handle"><Rank /></el-icon>
              <span class="question-num">第 {{ index + 1 }} 题</span>
              <el-tag :class="getTypeClass(q.type)" size="small">{{ getTypeName(q.type) }}</el-tag>
              <span class="question-score">{{ q.score }} 分</span>
              <el-button type="danger" size="small" text @click="removeQuestion(index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>

            <el-form label-width="80px" class="question-form">
              <el-row :gutter="16">
                <el-col :span="6">
                  <el-form-item label="题型">
                    <el-select v-model="q.type" size="small" @change="(val) => onTypeChange(q, val)">
                      <el-option v-for="(name, value) in typeMap" :key="value" :label="name" :value="value" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="分值">
                    <el-input-number v-model="q.score" :min="1" :max="50" size="small" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="题目内容">
                <el-input v-model="q.content" type="textarea" :rows="2" />
              </el-form-item>
              <el-form-item v-if="isChoiceType(q.type)" label="选项">
                <el-input v-model="q.options" type="textarea" :rows="2" placeholder='JSON 格式，如：["A. 选项1", "B. 选项2"]' />
              </el-form-item>
              <el-form-item label="答案">
                <!-- 判断题用单选 -->
                <template v-if="q.type === 'TRUE_FALSE'">
                  <el-radio-group v-model="q.answer">
                    <el-radio label="TRUE">对</el-radio>
                    <el-radio label="FALSE">错</el-radio>
                  </el-radio-group>
                </template>
                <!-- 主观题用多行文本 -->
                <template v-else-if="isSubjectiveType(q.type)">
                  <el-input v-model="q.answer" type="textarea" :rows="2" placeholder="请输入参考答案" />
                </template>
                <!-- 其他题型用单行文本 -->
                <template v-else>
                  <el-input v-model="q.answer" placeholder="请输入正确答案，多选题用逗号分隔如：A,B,C" />
                </template>
              </el-form-item>
            </el-form>
          </div>
        </template>
      </draggable>
    </div>

    <!-- Actions -->
    <div class="action-bar">
      <el-button @click="handleBack">返回</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">保存试卷</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPaperDetail, savePaper, aiGenerateQuestions } from '@/api'
import draggable from 'vuedraggable'
import { Select, Grid, CircleCheck, EditPen, Document, Notebook, Rank, InfoFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const aiGenerating = ref(false)

const basicFormRef = ref()
const paper = ref({
  id: null,
  title: '',
  description: '',
  durationMinutes: 120,
  questions: []
})

const basicRules = {
  title: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  durationMinutes: [{ required: true, message: '请设置考试时长', trigger: 'blur' }]
}

const aiForm = ref({
  subject: '',
  topic: '',
  difficulty: 'MEDIUM',
  // 通用题型
  singleChoiceCount: 0,
  multiChoiceCount: 0,
  trueFalseCount: 0,
  fillBlankCount: 0,
  // 学科专属题型
  readingCount: 0,        // 阅读理解
  compositionCount: 0,    // 作文/写作
  clozeCount: 0,          // 完形填空
  translationCount: 0,    // 翻译
  calculationCount: 0,    // 计算题
  proofCount: 0,          // 证明题
  experimentCount: 0,     // 实验题
  analysisCount: 0,       // 材料分析
  shortAnswerCount: 0,    // 简答题
  essayCount: 0           // 论述题
})

// 学科列表
const subjectList = [
  { value: '语文', label: '语文' },
  { value: '数学', label: '数学' },
  { value: '英语', label: '英语' },
  { value: '物理', label: '物理' },
  { value: '化学', label: '化学' },
  { value: '生物', label: '生物' },
  { value: '历史', label: '历史' },
  { value: '地理', label: '地理' },
  { value: '政治', label: '政治' }
]

// 所有题型定义（分值参考高考标准）
const allQuestionTypes = {
  singleChoiceCount: { name: '单选题', score: 3, max: 20, color: '#409EFF', icon: markRaw(Select) },
  multiChoiceCount: { name: '多选题', score: 4, max: 10, color: '#67C23A', icon: markRaw(Grid) },
  trueFalseCount: { name: '判断题', score: 2, max: 10, color: '#E6A23C', icon: markRaw(CircleCheck) },
  fillBlankCount: { name: '填空题', score: 4, max: 10, color: '#F56C6C', icon: markRaw(EditPen) },
  readingCount: { name: '阅读理解', score: 10, max: 4, color: '#409EFF', icon: markRaw(Document) },
  compositionCount: { name: '作文', score: 40, max: 1, color: '#8B5CF6', icon: markRaw(Notebook) },
  clozeCount: { name: '完形填空', score: 20, max: 2, color: '#F56C6C', icon: markRaw(EditPen) },
  translationCount: { name: '翻译', score: 10, max: 2, color: '#67C23A', icon: markRaw(Document) },
  calculationCount: { name: '计算题', score: 10, max: 5, color: '#409EFF', icon: markRaw(Document) },
  proofCount: { name: '证明题', score: 12, max: 3, color: '#8B5CF6', icon: markRaw(Notebook) },
  experimentCount: { name: '实验题', score: 12, max: 3, color: '#67C23A', icon: markRaw(Document) },
  analysisCount: { name: '材料分析', score: 15, max: 3, color: '#909399', icon: markRaw(Document) },
  shortAnswerCount: { name: '简答题', score: 6, max: 5, color: '#909399', icon: markRaw(Document) },
  essayCount: { name: '论述题', score: 12, max: 3, color: '#8B5CF6', icon: markRaw(Notebook) }
}

// 各学科的题型配置（包含默认数量）
const subjectQuestionTypes = {
  '语文': [
    { key: 'singleChoiceCount', count: 10 },
    { key: 'fillBlankCount', count: 5 },
    { key: 'readingCount', count: 2 },
    { key: 'shortAnswerCount', count: 2 },
    { key: 'compositionCount', count: 1 }
  ],
  '数学': [
    { key: 'singleChoiceCount', count: 10 },
    { key: 'multiChoiceCount', count: 5 },
    { key: 'fillBlankCount', count: 5 },
    { key: 'calculationCount', count: 4 },
    { key: 'proofCount', count: 2 }
  ],
  '英语': [
    { key: 'singleChoiceCount', count: 15 },
    { key: 'clozeCount', count: 1 },
    { key: 'readingCount', count: 2 },
    { key: 'translationCount', count: 2 },
    { key: 'compositionCount', count: 1 }
  ],
  '物理': [
    { key: 'singleChoiceCount', count: 8 },
    { key: 'multiChoiceCount', count: 4 },
    { key: 'fillBlankCount', count: 4 },
    { key: 'calculationCount', count: 4 },
    { key: 'experimentCount', count: 2 }
  ],
  '化学': [
    { key: 'singleChoiceCount', count: 8 },
    { key: 'multiChoiceCount', count: 4 },
    { key: 'fillBlankCount', count: 4 },
    { key: 'calculationCount', count: 4 },
    { key: 'experimentCount', count: 2 }
  ],
  '生物': [
    { key: 'singleChoiceCount', count: 10 },
    { key: 'multiChoiceCount', count: 5 },
    { key: 'trueFalseCount', count: 5 },
    { key: 'fillBlankCount', count: 5 },
    { key: 'shortAnswerCount', count: 3 },
    { key: 'experimentCount', count: 2 }
  ],
  '历史': [
    { key: 'singleChoiceCount', count: 12 },
    { key: 'multiChoiceCount', count: 4 },
    { key: 'fillBlankCount', count: 4 },
    { key: 'shortAnswerCount', count: 3 },
    { key: 'analysisCount', count: 2 }
  ],
  '地理': [
    { key: 'singleChoiceCount', count: 10 },
    { key: 'multiChoiceCount', count: 5 },
    { key: 'trueFalseCount', count: 5 },
    { key: 'fillBlankCount', count: 5 },
    { key: 'shortAnswerCount', count: 3 }
  ],
  '政治': [
    { key: 'singleChoiceCount', count: 10 },
    { key: 'multiChoiceCount', count: 5 },
    { key: 'shortAnswerCount', count: 3 },
    { key: 'analysisCount', count: 2 },
    { key: 'essayCount', count: 1 }
  ]
}

// 根据学科获取可用的题型
const availableQuestionTypes = computed(() => {
  const subject = aiForm.value.subject
  if (!subject) return []
  
  const typeConfigs = subjectQuestionTypes[subject] || []
  return typeConfigs.map(({ key }) => ({
    key,
    ...allQuestionTypes[key]
  }))
})

// 各学科知识点提示
const subjectTopicHints = {
  '语文': '如：文言文阅读、诗词鉴赏、现代文阅读（选填）',
  '数学': '如：三角函数、导数、概率统计（选填）',
  '英语': '如：时态语态、阅读理解、写作（选填）',
  '物理': '如：力学、电磁学、光学（选填）',
  '化学': '如：有机化学、无机化学、化学平衡（选填）',
  '生物': '如：细胞、遗传、生态系统（选填）',
  '历史': '如：古代史、近代史、世界史（选填）',
  '地理': '如：自然地理、人文地理、区域地理（选填）',
  '政治': '如：经济生活、政治生活、哲学（选填）'
}

// 知识点输入框提示
const topicPlaceholder = computed(() => {
  const subject = aiForm.value.subject
  return subjectTopicHints[subject] || '请先选择学科'
})

// 学科切换时自动填充推荐的题型数量
const onSubjectChange = () => {
  // 先清空所有题型数量
  Object.keys(allQuestionTypes).forEach(key => {
    aiForm.value[key] = 0
  })
  
  // 填充该学科的推荐数量
  const subject = aiForm.value.subject
  if (subject && subjectQuestionTypes[subject]) {
    subjectQuestionTypes[subject].forEach(({ key, count }) => {
      aiForm.value[key] = count
    })
  }
}

// AI 生成预计数量和分数
const aiTotalCount = computed(() => {
  return availableQuestionTypes.value
    .reduce((sum, t) => sum + (aiForm.value[t.key] || 0), 0)
})

const aiTotalScore = computed(() => {
  return availableQuestionTypes.value
    .reduce((sum, t) => sum + (aiForm.value[t.key] || 0) * t.score, 0)
})

const totalScore = computed(() => {
  return paper.value.questions.reduce((sum, q) => sum + (q.score || 0), 0)
})

const typeMap = {
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
  ESSAY: '论述题',
  READING: '阅读理解',
  COMPOSITION: '作文',
  CLOZE: '完形填空',
  TRANSLATION: '翻译',
  CALCULATION: '计算题',
  PROOF: '证明题',
  EXPERIMENT: '实验题',
  ANALYSIS: '材料分析'
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
const isSubjectiveType = (type) => ['SHORT_ANSWER', 'ESSAY'].includes(type)

// 题型切换时重置相关字段
const onTypeChange = (question, newType) => {
  const defaults = {
    SINGLE_CHOICE: {
      content: '请选择正确的选项：',
      options: '["A. 选项A", "B. 选项B", "C. 选项C", "D. 选项D"]',
      answer: '',
      score: 10
    },
    MULTI_CHOICE: {
      content: '请选择所有正确的选项：',
      options: '["A. 选项A", "B. 选项B", "C. 选项C", "D. 选项D"]',
      answer: '',
      score: 15
    },
    TRUE_FALSE: {
      content: '请判断以下说法是否正确：',
      options: null,
      answer: 'TRUE',
      score: 5
    },
    FILL_BLANK: {
      content: '请填写空白处的内容：______',
      options: null,
      answer: '',
      score: 10
    },
    SHORT_ANSWER: {
      content: '请简要回答以下问题：',
      options: null,
      answer: '',
      score: 15
    },
    ESSAY: {
      content: '请详细论述以下问题：',
      options: null,
      answer: '',
      score: 20
    }
  }
  
  const defaultValues = defaults[newType]
  if (defaultValues) {
    question.content = defaultValues.content
    question.options = defaultValues.options
    question.answer = defaultValues.answer
    question.score = defaultValues.score
  }
}

// 清空 AI 表单中的题型数量
const resetAiFormCounts = () => {
  Object.keys(allQuestionTypes).forEach(key => {
    aiForm.value[key] = 0
  })
}

const loadData = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const res = await getPaperDetail(id)
    paper.value = res.data
  } finally {
    loading.value = false
  }
}

const addQuestion = () => {
  paper.value.questions.push({
    id: Date.now(),
    type: 'SINGLE_CHOICE',
    content: '请选择正确的选项：',
    options: '["A. 选项A", "B. 选项B", "C. 选项C", "D. 选项D"]',
    answer: '',
    score: 10
  })
}

const removeQuestion = (index) => {
  paper.value.questions.splice(index, 1)
}

// 题型排序优先级
const typeOrder = {
  SINGLE_CHOICE: 1,
  MULTI_CHOICE: 2,
  TRUE_FALSE: 3,
  FILL_BLANK: 4,
  CLOZE: 5,
  READING: 6,
  TRANSLATION: 7,
  CALCULATION: 8,
  PROOF: 9,
  EXPERIMENT: 10,
  SHORT_ANSWER: 11,
  ANALYSIS: 12,
  ESSAY: 13,
  COMPOSITION: 14
}

// 按题型排序题目
const sortQuestionsByType = (questions) => {
  return [...questions].sort((a, b) => {
    return (typeOrder[a.type] || 99) - (typeOrder[b.type] || 99)
  })
}

const handleAiGenerate = async () => {
  // 先验证基本信息
  const valid = await basicFormRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请先填写试卷名称')
    return
  }

  if (!aiForm.value.subject) {
    ElMessage.warning('请输入学科')
    return
  }

  if (aiTotalCount.value === 0) {
    ElMessage.warning('请至少选择一种题型')
    return
  }

  aiGenerating.value = true
  try {
    const res = await aiGenerateQuestions(aiForm.value)
    // 为每个题目添加唯一 id 用于拖拽
    const questionsWithId = res.data.map((q, i) => ({
      ...q,
      id: Date.now() + i
    }))
    // 合并后按题型排序
    paper.value.questions = sortQuestionsByType([...paper.value.questions, ...questionsWithId])
    ElMessage.success(`成功生成 ${res.data.length} 道题目`)
    // 生成成功后清空题型数量
    resetAiFormCounts()
  } finally {
    aiGenerating.value = false
  }
}

// 检查是否有未保存的内容
const hasUnsavedContent = () => {
  return paper.value.title || paper.value.questions.length > 0
}

// 保存
const saveChanges = async () => {
  if (!paper.value.title) {
    ElMessage.warning('请先填写试卷名称')
    return false
  }
  
  saving.value = true
  try {
    await savePaper(paper.value)
    const msg = paper.value.status === 'PUBLISHED' ? '修改已保存' : '草稿已保存'
    ElMessage.success(msg)
    return true
  } catch (e) {
    ElMessage.error('保存失败')
    return false
  } finally {
    saving.value = false
  }
}

// 返回按钮处理
const handleBack = async () => {
  if (hasUnsavedContent()) {
    const isPublished = paper.value.status === 'PUBLISHED'
    try {
      const action = await ElMessageBox.confirm(
        isPublished ? '当前有未保存的修改，是否保存？' : '当前有未保存的内容，是否保存为草稿？',
        '提示',
        {
          confirmButtonText: isPublished ? '保存修改' : '保存草稿',
          cancelButtonText: '不保存',
          distinguishCancelAndClose: true,
          type: 'warning'
        }
      )
      if (action === 'confirm') {
        const saved = await saveChanges()
        if (saved) {
          router.push('/paper')
        }
      }
    } catch (action) {
      if (action === 'cancel') {
        // 用户选择不保存，直接返回
        router.push('/paper')
      }
      // close 的情况不做处理，留在当前页面
    }
  } else {
    router.push('/paper')
  }
}

const handleSave = async () => {
  const valid = await basicFormRef.value.validate().catch(() => false)
  if (!valid) return

  if (paper.value.questions.length === 0) {
    ElMessage.warning('请至少添加一道题目')
    return
  }

  saving.value = true
  try {
    await savePaper(paper.value)
    ElMessage.success('保存成功')
    router.push('/paper')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.paper-edit {
  .empty-tip {
    text-align: center;
    padding: 40px;
    color: #909399;
  }

  .empty-subject-tip {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 24px;
    background: #f5f7fa;
    border-radius: 8px;
    color: #909399;
    font-size: 14px;
    margin-top: 8px;
  }

  .question-type-cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
    margin-top: 8px;

    .type-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      background: #f5f7fa;
      border-radius: 8px;
      border: 1px solid #e4e7ed;
      transition: all 0.2s;

      &:hover {
        border-color: #409eff;
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
      }

      .type-icon {
        width: 36px;
        height: 36px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 18px;
      }

      .type-info {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 2px;

        .type-name {
          font-weight: 500;
          color: #303133;
        }

        .type-score {
          font-size: 12px;
          color: #909399;
        }
      }

      :deep(.el-input-number) {
        width: 90px;
      }
    }
  }

  .generate-tip {
    margin-left: 16px;
    color: #67c23a;
    font-size: 14px;
  }

  .section-title {
    .title-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .drag-tip {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #909399;
        background: #f4f4f5;
        padding: 4px 10px;
        border-radius: 4px;
      }
    }
  }

  .question-item {
    background: #FAFAFA;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 16px;
    border: 1px solid #EBEEF5;
    transition: all 0.2s;

    &:hover {
      border-color: #409eff;
    }

    &.ghost {
      opacity: 0.5;
      background: #e6f7ff;
    }

    .question-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px dashed #EBEEF5;

      .drag-handle {
        cursor: grab;
        color: #909399;
        font-size: 18px;
        padding: 4px;
        border-radius: 4px;
        transition: all 0.2s;

        &:hover {
          color: #409eff;
          background: #ecf5ff;
        }

        &:active {
          cursor: grabbing;
        }
      }

      .question-num {
        font-weight: 600;
        color: #303133;
      }

      .question-score {
        color: #E6A23C;
        font-weight: 500;
      }

      .el-button {
        margin-left: auto;
      }
    }

    .question-form {
      :deep(.el-form-item) {
        margin-bottom: 12px;
      }
    }
  }

  .action-bar {
    position: sticky;
    bottom: 0;
    background: #fff;
    padding: 16px 24px;
    margin: 0 -24px -24px;
    border-top: 1px solid #EBEEF5;
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
  }
}
</style>
