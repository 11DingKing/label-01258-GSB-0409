package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BusinessException;
import com.exam.dto.SubmitExamRequest;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final PaperMapper paperMapper;
    private final QuestionMapper questionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final UserMapper userMapper;

    public List<Paper> getAvailablePapers() {
        return paperMapper.selectList(
                new LambdaQueryWrapper<Paper>()
                        .eq(Paper::getStatus, "PUBLISHED")
                        .orderByDesc(Paper::getCreatedAt)
        );
    }

    @Transactional
    public ExamRecord startExam(Long paperId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null || !"PUBLISHED".equals(paper.getStatus())) {
            throw new BusinessException("试卷不可用");
        }

        // Check if already has an in-progress exam
        ExamRecord existing = examRecordMapper.selectOne(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, UserContext.getUserId())
                        .eq(ExamRecord::getPaperId, paperId)
                        .eq(ExamRecord::getStatus, "IN_PROGRESS")
        );
        if (existing != null) {
            return loadExamRecord(existing);
        }

        // Create new exam record
        ExamRecord record = new ExamRecord();
        record.setUserId(UserContext.getUserId());
        record.setPaperId(paperId);
        record.setStartTime(LocalDateTime.now());
        record.setStatus("IN_PROGRESS");
        record.setGradingStatus("PENDING");
        examRecordMapper.insert(record);

        // Create answer records for each question
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getPaperId, paperId)
                        .orderByAsc(Question::getSortOrder)
        );
        for (Question q : questions) {
            AnswerRecord answer = new AnswerRecord();
            answer.setExamRecordId(record.getId());
            answer.setQuestionId(q.getId());
            answer.setIsGraded(false);
            answerRecordMapper.insert(answer);
        }

        log.info("User {} started exam for paper {}", UserContext.getUsername(), paper.getTitle());
        return loadExamRecord(record);
    }

    public ExamRecord getExamRecord(Long id) {
        ExamRecord record = examRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("考试记录不存在");
        }
        // Check permission
        if (!record.getUserId().equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException("无权限查看此考试");
        }
        return loadExamRecord(record);
    }

    @Transactional
    public void submitExam(SubmitExamRequest request) {
        ExamRecord record = examRecordMapper.selectById(request.getExamRecordId());
        if (record == null) {
            throw new BusinessException("考试记录不存在");
        }
        if (!record.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("无权限");
        }
        if ("SUBMITTED".equals(record.getStatus())) {
            throw new BusinessException("考试已提交");
        }

        // Save answers
        if (request.getAnswers() != null) {
            for (SubmitExamRequest.AnswerItem item : request.getAnswers()) {
                AnswerRecord answer = answerRecordMapper.selectOne(
                        new LambdaQueryWrapper<AnswerRecord>()
                                .eq(AnswerRecord::getExamRecordId, record.getId())
                                .eq(AnswerRecord::getQuestionId, item.getQuestionId())
                );
                if (answer != null) {
                    answer.setUserAnswer(item.getAnswer());
                    answerRecordMapper.updateById(answer);
                }
            }
        }

        // Update record status
        record.setStatus("SUBMITTED");
        record.setSubmitTime(LocalDateTime.now());
        record.setGradingStatus("PENDING");
        examRecordMapper.updateById(record);

        // Auto-grade objective questions
        autoGradeObjective(record.getId());

        log.info("User {} submitted exam {}", UserContext.getUsername(), record.getId());
    }

    private void autoGradeObjective(Long examRecordId) {
        List<AnswerRecord> answers = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getExamRecordId, examRecordId)
        );

        int totalScore = 0;
        boolean hasSubjective = false;

        for (AnswerRecord answer : answers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            if (question == null) continue;

            String type = question.getType();
            if ("SINGLE_CHOICE".equals(type) || "MULTI_CHOICE".equals(type) || "TRUE_FALSE".equals(type)) {
                // Objective question - auto grade
                String correctAnswer = question.getAnswer().trim().toUpperCase();
                String userAnswer = answer.getUserAnswer() != null ? answer.getUserAnswer().trim().toUpperCase() : "";
                
                if (correctAnswer.equals(userAnswer)) {
                    answer.setScore(question.getScore());
                    totalScore += question.getScore();
                } else {
                    answer.setScore(0);
                }
                answer.setIsGraded(true);
                answerRecordMapper.updateById(answer);
            } else {
                hasSubjective = true;
            }
        }

        // Update exam record
        ExamRecord record = examRecordMapper.selectById(examRecordId);
        record.setTotalScore(totalScore);
        record.setGradingStatus(hasSubjective ? "PENDING" : "COMPLETED");
        examRecordMapper.updateById(record);
    }

    private ExamRecord loadExamRecord(ExamRecord record) {
        Paper paper = paperMapper.selectById(record.getPaperId());
        if (paper != null) {
            record.setPaperTitle(paper.getTitle());
        }

        User user = userMapper.selectById(record.getUserId());
        if (user != null) {
            record.setUsername(user.getNickname());
        }

        List<AnswerRecord> answers = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getExamRecordId, record.getId())
        );
        for (AnswerRecord answer : answers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            answer.setQuestion(question);
        }
        record.setAnswers(answers);

        return record;
    }
}
