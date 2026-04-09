import request, { longRequest } from './request'

// Auth
export const login = data => request.post('/auth/login', data)
export const register = data => request.post('/auth/register', data)
export const getUserInfo = () => request.get('/auth/info')

// LLM Config
export const getLlmList = () => request.get('/llm/list')
export const saveLlmConfig = data => request.post('/llm/save', data)
export const deleteLlmConfig = id => request.delete(`/llm/${id}`)
export const setLlmDefault = id => request.put(`/llm/${id}/default`)
export const testLlmConnection = id => longRequest.post(`/llm/test/${id}`)

// Paper
export const getPaperList = () => request.get('/paper/list')
export const getPaperDetail = id => request.get(`/paper/${id}`)
export const savePaper = data => request.post('/paper/save', data)
export const deletePaper = id => request.delete(`/paper/${id}`)
export const publishPaper = id => request.put(`/paper/${id}/publish`)
export const unpublishPaper = id => request.put(`/paper/${id}/unpublish`)
export const aiGenerateQuestions = data => longRequest.post('/paper/ai-generate', data)

// Exam
export const getAvailableExams = () => request.get('/exam/available')
export const startExam = paperId => request.post(`/exam/start/${paperId}`)
export const getExamRecord = id => request.get(`/exam/record/${id}`)
export const submitExam = data => request.post('/exam/submit', data)

// Grading
export const getPendingGrading = () => request.get('/grading/pending')
export const aiGrade = recordId => longRequest.post(`/grading/ai-grade/${recordId}`)
export const manualGrade = (answerId, score, comment) => 
  request.post('/grading/manual', null, { params: { answerId, score, comment } })

// Score
export const getMyScores = () => request.get('/score/my')
export const getScoreDetail = recordId => request.get(`/score/detail/${recordId}`)
export const getRanking = paperId => request.get(`/score/ranking/${paperId}`)
