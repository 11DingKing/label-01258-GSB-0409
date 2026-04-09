package com.exam.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BusinessException;
import com.exam.entity.*;
import com.exam.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradingService {

    private final ExamRecordMapper examRecordMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final PaperMapper paperMapper;
    private final LlmConfigService llmConfigService;
    private final LlmService llmService;

    public List<ExamRecord> getPendingList() {
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getStatus, "SUBMITTED")
                        .in(ExamRecord::getGradingStatus, List.of("PENDING", "GRADING"))
                        .orderByAsc(ExamRecord::getSubmitTime)
        );
        for (ExamRecord record : records) {
            Paper paper = paperMapper.selectById(record.getPaperId());
            if (paper != null) {
                record.setPaperTitle(paper.getTitle());
            }
            User user = userMapper.selectById(record.getUserId());
            if (user != null) {
                record.setUsername(user.getNickname());
            }
        }
        return records;
    }

    @Transactional
    public void aiGrade(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("考试记录不存在");
        }

        LlmConfig config = llmConfigService.getDefault();
        
        record.setGradingStatus("GRADING");
        examRecordMapper.updateById(record);

        List<AnswerRecord> answers = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getExamRecordId, recordId)
                        .eq(AnswerRecord::getIsGraded, false)
        );

        int additionalScore = 0;

        for (AnswerRecord answer : answers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            if (question == null) continue;

            String type = question.getType();
            if ("FILL_BLANK".equals(type) || "SHORT_ANSWER".equals(type) || "ESSAY".equals(type)) {
                try {
                    GradeResult result;
                    boolean isMockMode = config == null || "MOCK".equalsIgnoreCase(config.getProvider()) 
                            || config.getApiKey() == null || config.getApiKey().length() < 10 
                            || "mock-api-key".equals(config.getApiKey());
                    
                    if (isMockMode) {
                        // 使用模拟批改
                        result = mockGradeQuestion(question, answer.getUserAnswer());
                        log.info("使用模拟模式批改题目 {}", question.getId());
                    } else {
                        result = gradeSubjectiveQuestion(config, question, answer.getUserAnswer());
                    }
                    answer.setScore(result.score);
                    answer.setAiComment(result.comment);
                    answer.setIsGraded(true);
                    answerRecordMapper.updateById(answer);
                    additionalScore += result.score;
                } catch (Exception e) {
                    log.error("AI grading failed for answer {}", answer.getId(), e);
                    // 批改失败时使用模拟批改
                    GradeResult result = mockGradeQuestion(question, answer.getUserAnswer());
                    answer.setScore(result.score);
                    answer.setAiComment(result.comment + "（API调用失败，已使用模拟批改）");
                    answer.setIsGraded(true);
                    answerRecordMapper.updateById(answer);
                    additionalScore += result.score;
                }
            }
        }

        // Recalculate total score from all graded answers (fix: avoid cumulative score bug on retries)
        record = examRecordMapper.selectById(recordId);
        List<AnswerRecord> allGradedAnswers = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getExamRecordId, recordId)
                        .eq(AnswerRecord::getIsGraded, true)
        );
        int totalScore = allGradedAnswers.stream()
                .mapToInt(a -> a.getScore() != null ? a.getScore() : 0)
                .sum();
        record.setTotalScore(totalScore);
        
        // Convergence check: only set to COMPLETED if ALL answers are graded
        long ungradedCount = answerRecordMapper.selectCount(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getExamRecordId, recordId)
                        .eq(AnswerRecord::getIsGraded, false)
        );
        record.setGradingStatus(ungradedCount == 0 ? "COMPLETED" : "PENDING");
        
        examRecordMapper.updateById(record);

        log.info("AI grading completed for exam record {}, ungraded answers: {}, totalScore: {}", 
                recordId, ungradedCount, totalScore);
    }
    
    /**
     * 模拟批改（当没有配置大模型时使用）
     */
    private GradeResult mockGradeQuestion(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return new GradeResult(0, "未作答，得0分");
        }
        
        int maxScore = question.getScore();
        int answerLength = userAnswer.trim().length();
        
        // 根据答案长度给予分数
        int score;
        String comment;
        
        if (answerLength < 10) {
            score = maxScore / 4;
            comment = "回答过于简短，建议详细阐述";
        } else if (answerLength < 50) {
            score = maxScore / 2;
            comment = "回答基本完整，但可以更加详细";
        } else if (answerLength < 100) {
            score = (int)(maxScore * 0.7);
            comment = "回答较为完整，表述清晰";
        } else {
            score = (int)(maxScore * 0.85);
            comment = "回答详细完整，论述充分";
        }
        
        return new GradeResult(score, comment + "（模拟批改）");
    }

    @Transactional
    public void manualGrade(Long answerId, Integer score, String comment) {
        AnswerRecord answer = answerRecordMapper.selectById(answerId);
        if (answer == null) {
            throw new BusinessException("答题记录不存在");
        }

        Question question = questionMapper.selectById(answer.getQuestionId());
        if (question != null && score > question.getScore()) {
            throw new BusinessException("得分不能超过题目满分");
        }

        int oldScore = answer.getScore() != null ? answer.getScore() : 0;
        answer.setScore(score);
        answer.setAiComment(comment);
        answer.setIsGraded(true);
        answerRecordMapper.updateById(answer);

        // Update exam record total score
        ExamRecord record = examRecordMapper.selectById(answer.getExamRecordId());
        int newTotal = (record.getTotalScore() != null ? record.getTotalScore() : 0) - oldScore + score;
        record.setTotalScore(newTotal);

        // Check if all graded
        long ungradedCount = answerRecordMapper.selectCount(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getExamRecordId, record.getId())
                        .eq(AnswerRecord::getIsGraded, false)
        );
        if (ungradedCount == 0) {
            record.setGradingStatus("COMPLETED");
        }
        examRecordMapper.updateById(record);

        log.info("Manual grading: answer={}, score={}", answerId, score);
    }

    private GradeResult gradeSubjectiveQuestion(LlmConfig config, Question question, String userAnswer) {
        String systemPrompt = """
            You are an expert exam grader. Grade the student's answer based on the question and reference answer.
            Return a JSON object with two fields:
            - score: integer (0 to max_score)
            - comment: string (brief feedback in Chinese)
            Be fair but strict. Partial credit is allowed.
            Return ONLY the JSON object, no other text.
            """;

        String userPrompt = String.format("""
            Question Type: %s
            Max Score: %d
            Question: %s
            Reference Answer: %s
            Student's Answer: %s
            
            Please grade this answer.
            """,
                question.getType(),
                question.getScore(),
                question.getContent(),
                question.getAnswer(),
                userAnswer != null ? userAnswer : "(No answer provided)"
        );

        String response = llmService.chat(config, systemPrompt, userPrompt);

        try {
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

            JSONObject json = JSON.parseObject(jsonStr);
            int score = json.getIntValue("score", 0);
            score = Math.min(score, question.getScore()); // Cap at max score
            String comment = json.getString("comment");
            return new GradeResult(score, comment);
        } catch (Exception e) {
            log.error("Failed to parse grading response: {}", response, e);
            return new GradeResult(0, "AI grading parse error");
        }
    }

    private record GradeResult(int score, String comment) {}
}
