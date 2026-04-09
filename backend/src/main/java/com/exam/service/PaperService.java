package com.exam.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BusinessException;
import com.exam.dto.AiGenerateRequest;
import com.exam.entity.ExamRecord;
import com.exam.entity.LlmConfig;
import com.exam.entity.Paper;
import com.exam.entity.Question;
import com.exam.entity.User;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.PaperMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.UserMapper;
import com.exam.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperMapper paperMapper;
    private final QuestionMapper questionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final UserMapper userMapper;
    private final LlmConfigService llmConfigService;
    private final LlmService llmService;

    public List<Paper> list() {
        List<Paper> papers = paperMapper.selectList(
                new LambdaQueryWrapper<Paper>().orderByDesc(Paper::getCreatedAt)
        );
        for (Paper paper : papers) {
            User creator = userMapper.selectById(paper.getCreatorId());
            if (creator != null) {
                paper.setCreatorName(creator.getNickname());
            }
        }
        return papers;
    }

    public Paper getById(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getPaperId, id)
                        .orderByAsc(Question::getSortOrder)
        );
        paper.setQuestions(questions);
        return paper;
    }

    @Transactional
    public void save(Paper paper) {
        if (paper.getId() == null) {
            paper.setCreatorId(UserContext.getUserId());
            paper.setStatus("DRAFT");
            paper.setCreatedAt(java.time.LocalDateTime.now());
            paperMapper.insert(paper);
            log.info("Paper created: {}", paper.getTitle());
        } else {
            Paper existing = paperMapper.selectById(paper.getId());
            if (existing == null) {
                throw new BusinessException("试卷不存在");
            }
            // 保持原有状态，允许编辑已发布的试卷
            paper.setStatus(existing.getStatus());
            paperMapper.updateById(paper);
            log.info("Paper updated: {}", paper.getTitle());
        }

        // Save questions
        if (paper.getQuestions() != null) {
            // Delete old questions
            questionMapper.delete(
                    new LambdaQueryWrapper<Question>().eq(Question::getPaperId, paper.getId())
            );
            // Insert new questions
            int order = 0;
            int totalScore = 0;
            for (Question q : paper.getQuestions()) {
                q.setId(null);
                q.setPaperId(paper.getId());
                q.setSortOrder(order++);
                questionMapper.insert(q);
                totalScore += q.getScore() != null ? q.getScore() : 0;
            }
            // Update total score
            paper.setTotalScore(totalScore);
            paperMapper.updateById(paper);
        }
    }

    @Transactional
    public void delete(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            return;
        }
        // 删除相关的考试记录和答题记录（通过外键级联删除答题记录）
        examRecordMapper.delete(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getPaperId, id));
        // 删除题目
        questionMapper.delete(new LambdaQueryWrapper<Question>().eq(Question::getPaperId, id));
        // 删除试卷
        paperMapper.deleteById(id);
        log.info("Paper deleted: id={}, title={}", id, paper.getTitle());
    }

    public void publish(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        long questionCount = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>().eq(Question::getPaperId, id)
        );
        if (questionCount == 0) {
            throw new BusinessException("试卷没有题目，无法发布");
        }
        paper.setStatus("PUBLISHED");
        paperMapper.updateById(paper);
        log.info("Paper published: {}", paper.getTitle());
    }

    public void unpublish(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        paper.setStatus("DRAFT");
        paperMapper.updateById(paper);
        log.info("Paper unpublished: {}", paper.getTitle());
    }

    public List<Question> aiGenerate(AiGenerateRequest request) {
        // 检查是否至少选择了一种题型
        if (!request.hasAnyQuestionType()) {
            throw new BusinessException("请至少选择一种题型");
        }
        
        LlmConfig config = llmConfigService.getDefault();
        if (config == null) {
            throw new BusinessException("请先在「大模型配置」中配置并测试连接成功");
        }
        
        // 如果是 MOCK 模式，明确返回模拟数据
        if ("MOCK".equalsIgnoreCase(config.getProvider())) {
            log.info("Using mock mode for question generation");
            return generateMockQuestions(request);
        }

        String systemPrompt = """
            You are an expert exam question generator. Generate exam questions in JSON format.
            Each question must have: type, content, options (for choice questions), answer, score.
            Question types: SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE, FILL_BLANK, SHORT_ANSWER, ESSAY, 
                           READING, COMPOSITION, CLOZE, TRANSLATION, CALCULATION, PROOF, EXPERIMENT, ANALYSIS.
            For SINGLE_CHOICE and MULTI_CHOICE, options should be a JSON array like ["A. xxx", "B. xxx", "C. xxx", "D. xxx"].
            For MULTI_CHOICE, answer should be comma-separated like "A,B,C".
            For TRUE_FALSE, answer should be "TRUE" or "FALSE".
            Return ONLY a JSON array of questions, no other text.
            """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Subject: ").append(request.getSubject()).append("\n");
        if (request.getTopic() != null && !request.getTopic().isEmpty()) {
            userPrompt.append("Topic: ").append(request.getTopic()).append("\n");
        }
        userPrompt.append("Difficulty: ").append(request.getDifficulty()).append("\n");
        userPrompt.append("Generate the following questions:\n");
        
        // 通用题型
        if (request.getSingleChoiceCount() > 0) {
            userPrompt.append("- ").append(request.getSingleChoiceCount()).append(" single choice questions (3 points each)\n");
        }
        if (request.getMultiChoiceCount() > 0) {
            userPrompt.append("- ").append(request.getMultiChoiceCount()).append(" multiple choice questions (4 points each)\n");
        }
        if (request.getTrueFalseCount() > 0) {
            userPrompt.append("- ").append(request.getTrueFalseCount()).append(" true/false questions (2 points each)\n");
        }
        if (request.getFillBlankCount() > 0) {
            userPrompt.append("- ").append(request.getFillBlankCount()).append(" fill-in-the-blank questions (4 points each)\n");
        }
        if (request.getShortAnswerCount() > 0) {
            userPrompt.append("- ").append(request.getShortAnswerCount()).append(" short answer questions (6 points each)\n");
        }
        if (request.getEssayCount() > 0) {
            userPrompt.append("- ").append(request.getEssayCount()).append(" essay questions (12 points each)\n");
        }
        
        // 学科专属题型
        if (request.getReadingCount() > 0) {
            userPrompt.append("- ").append(request.getReadingCount()).append(" reading comprehension questions (10 points each)\n");
        }
        if (request.getCompositionCount() > 0) {
            userPrompt.append("- ").append(request.getCompositionCount()).append(" composition/writing questions (40 points each)\n");
        }
        if (request.getClozeCount() > 0) {
            userPrompt.append("- ").append(request.getClozeCount()).append(" cloze test questions (20 points each)\n");
        }
        if (request.getTranslationCount() > 0) {
            userPrompt.append("- ").append(request.getTranslationCount()).append(" translation questions (10 points each)\n");
        }
        if (request.getCalculationCount() > 0) {
            userPrompt.append("- ").append(request.getCalculationCount()).append(" calculation questions (10 points each)\n");
        }
        if (request.getProofCount() > 0) {
            userPrompt.append("- ").append(request.getProofCount()).append(" proof questions (12 points each)\n");
        }
        if (request.getExperimentCount() > 0) {
            userPrompt.append("- ").append(request.getExperimentCount()).append(" experiment questions (12 points each)\n");
        }
        if (request.getAnalysisCount() > 0) {
            userPrompt.append("- ").append(request.getAnalysisCount()).append(" material analysis questions (15 points each)\n");
        }

        log.info("AI generating questions for subject: {}", request.getSubject());
        
        try {
            String response = llmService.chat(config, systemPrompt, userPrompt.toString());
            
            // Parse response
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();
            
            JSONArray jsonArray = JSON.parseArray(jsonStr);
            List<Question> questions = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Question q = new Question();
                q.setType(obj.getString("type"));
                q.setContent(obj.getString("content"));
                q.setOptions(obj.containsKey("options") ? obj.getJSONArray("options").toJSONString() : null);
                q.setAnswer(obj.getString("answer"));
                q.setScore(obj.getIntValue("score", 10));
                questions.add(q);
            }
            log.info("AI generated {} questions", questions.size());
            return questions;
        } catch (Exception e) {
            log.error("AI generation failed: {}", e.getMessage());
            throw new BusinessException("AI 生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成模拟题目数据（当 AI 服务不可用时使用）
     */
    private List<Question> generateMockQuestions(AiGenerateRequest request) {
        List<Question> questions = new ArrayList<>();
        String subject = request.getSubject();
        
        // 生成单选题
        for (int i = 0; i < request.getSingleChoiceCount(); i++) {
            Question q = new Question();
            q.setType("SINGLE_CHOICE");
            q.setContent(subject + " - 单选题 " + (i + 1) + "：以下哪个选项是正确的？");
            q.setOptions("[\"A. 选项A\", \"B. 选项B\", \"C. 选项C\", \"D. 选项D\"]");
            q.setAnswer("A");
            q.setScore(3);
            questions.add(q);
        }
        
        // 生成多选题
        for (int i = 0; i < request.getMultiChoiceCount(); i++) {
            Question q = new Question();
            q.setType("MULTI_CHOICE");
            q.setContent(subject + " - 多选题 " + (i + 1) + "：以下哪些选项是正确的？");
            q.setOptions("[\"A. 选项A\", \"B. 选项B\", \"C. 选项C\", \"D. 选项D\"]");
            q.setAnswer("A,B");
            q.setScore(4);
            questions.add(q);
        }
        
        // 生成判断题
        for (int i = 0; i < request.getTrueFalseCount(); i++) {
            Question q = new Question();
            q.setType("TRUE_FALSE");
            q.setContent(subject + " - 判断题 " + (i + 1) + "：这是一个正确的陈述。");
            q.setAnswer("TRUE");
            q.setScore(2);
            questions.add(q);
        }
        
        // 生成填空题
        for (int i = 0; i < request.getFillBlankCount(); i++) {
            Question q = new Question();
            q.setType("FILL_BLANK");
            q.setContent(subject + " - 填空题 " + (i + 1) + "：请填写 ______ 的内容。");
            q.setAnswer("答案");
            q.setScore(4);
            questions.add(q);
        }
        
        // 生成简答题
        for (int i = 0; i < request.getShortAnswerCount(); i++) {
            Question q = new Question();
            q.setType("SHORT_ANSWER");
            q.setContent(subject + " - 简答题 " + (i + 1) + "：请简要回答这个问题。");
            q.setAnswer("参考答案：这是一个简答题的参考答案。");
            q.setScore(6);
            questions.add(q);
        }
        
        // 生成论述题
        for (int i = 0; i < request.getEssayCount(); i++) {
            Question q = new Question();
            q.setType("ESSAY");
            q.setContent(subject + " - 论述题 " + (i + 1) + "：请详细论述这个问题。");
            q.setAnswer("参考答案：这是一个论述题的参考答案，需要详细展开论述。");
            q.setScore(12);
            questions.add(q);
        }
        
        // 生成阅读理解
        for (int i = 0; i < request.getReadingCount(); i++) {
            Question q = new Question();
            q.setType("READING");
            q.setContent(subject + " - 阅读理解 " + (i + 1) + "：阅读下面的文章，回答问题。\n\n这是一段示例文章内容...\n\n问题：文章的主旨是什么？");
            q.setAnswer("参考答案：文章主要讲述了...");
            q.setScore(10);
            questions.add(q);
        }
        
        // 生成作文
        for (int i = 0; i < request.getCompositionCount(); i++) {
            Question q = new Question();
            q.setType("COMPOSITION");
            q.setContent(subject + " - 作文 " + (i + 1) + "：请根据题目要求写一篇文章。\n\n题目：我的梦想\n要求：不少于800字");
            q.setAnswer("评分标准：内容切题、结构完整、语言流畅、书写工整");
            q.setScore(40);
            questions.add(q);
        }
        
        // 生成完形填空
        for (int i = 0; i < request.getClozeCount(); i++) {
            Question q = new Question();
            q.setType("CLOZE");
            q.setContent(subject + " - 完形填空 " + (i + 1) + "：阅读下面短文，从每题所给的选项中选出最佳选项。\n\nThe weather was (1)_____ yesterday...");
            q.setOptions("[\"A. fine\", \"B. bad\", \"C. cold\", \"D. hot\"]");
            q.setAnswer("A");
            q.setScore(20);
            questions.add(q);
        }
        
        // 生成翻译题
        for (int i = 0; i < request.getTranslationCount(); i++) {
            Question q = new Question();
            q.setType("TRANSLATION");
            q.setContent(subject + " - 翻译 " + (i + 1) + "：请将下列句子翻译成中文/英文。\n\nKnowledge is power.");
            q.setAnswer("知识就是力量。");
            q.setScore(10);
            questions.add(q);
        }
        
        // 生成计算题
        for (int i = 0; i < request.getCalculationCount(); i++) {
            Question q = new Question();
            q.setType("CALCULATION");
            q.setContent(subject + " - 计算题 " + (i + 1) + "：请计算下列问题，写出详细的解题过程。\n\n已知 x + y = 10，x - y = 4，求 x 和 y 的值。");
            q.setAnswer("解：由 x + y = 10 和 x - y = 4，两式相加得 2x = 14，所以 x = 7，y = 3。");
            q.setScore(10);
            questions.add(q);
        }
        
        // 生成证明题
        for (int i = 0; i < request.getProofCount(); i++) {
            Question q = new Question();
            q.setType("PROOF");
            q.setContent(subject + " - 证明题 " + (i + 1) + "：请证明下列命题。\n\n证明：三角形内角和等于180度。");
            q.setAnswer("证明过程：过三角形一个顶点作对边的平行线...");
            q.setScore(12);
            questions.add(q);
        }
        
        // 生成实验题
        for (int i = 0; i < request.getExperimentCount(); i++) {
            Question q = new Question();
            q.setType("EXPERIMENT");
            q.setContent(subject + " - 实验题 " + (i + 1) + "：根据实验要求回答问题。\n\n实验目的：验证牛顿第二定律\n实验器材：小车、砝码、打点计时器...\n\n问题：请写出实验步骤和数据处理方法。");
            q.setAnswer("参考答案：1. 安装实验装置... 2. 调整轨道水平... 3. 记录数据...");
            q.setScore(12);
            questions.add(q);
        }
        
        // 生成材料分析题
        for (int i = 0; i < request.getAnalysisCount(); i++) {
            Question q = new Question();
            q.setType("ANALYSIS");
            q.setContent(subject + " - 材料分析 " + (i + 1) + "：阅读下列材料，回答问题。\n\n材料一：...\n材料二：...\n\n问题：根据材料分析其历史意义。");
            q.setAnswer("参考答案：根据材料可以看出...");
            q.setScore(15);
            questions.add(q);
        }
        
        log.info("Generated {} mock questions for subject: {}", questions.size(), subject);
        return questions;
    }
}
