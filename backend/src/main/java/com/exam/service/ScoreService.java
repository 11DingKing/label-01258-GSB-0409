package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BusinessException;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ExamRecordMapper examRecordMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final QuestionMapper questionMapper;
    private final PaperMapper paperMapper;
    private final UserMapper userMapper;

    public List<ExamRecord> getMyScores() {
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, UserContext.getUserId())
                        .eq(ExamRecord::getStatus, "SUBMITTED")
                        .orderByDesc(ExamRecord::getSubmitTime)
        );
        for (ExamRecord record : records) {
            Paper paper = paperMapper.selectById(record.getPaperId());
            if (paper != null) {
                record.setPaperTitle(paper.getTitle());
            }
        }
        return records;
    }

    public ExamRecord getScoreDetail(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        
        // Check permission
        if (!record.getUserId().equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException("无权限查看");
        }

        Paper paper = paperMapper.selectById(record.getPaperId());
        if (paper != null) {
            record.setPaperTitle(paper.getTitle());
        }

        User user = userMapper.selectById(record.getUserId());
        if (user != null) {
            record.setUsername(user.getNickname());
        }

        List<AnswerRecord> answers = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getExamRecordId, recordId)
        );
        for (AnswerRecord answer : answers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            answer.setQuestion(question);
        }
        record.setAnswers(answers);

        return record;
    }

    public List<RankingItem> getRanking(Long paperId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }

        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getPaperId, paperId)
                        .eq(ExamRecord::getStatus, "SUBMITTED")
                        .eq(ExamRecord::getGradingStatus, "COMPLETED")
                        .orderByDesc(ExamRecord::getTotalScore)
        );

        List<RankingItem> ranking = new ArrayList<>();
        int rank = 0;
        Integer lastScore = null;
        int sameRankCount = 0;

        for (ExamRecord record : records) {
            User user = userMapper.selectById(record.getUserId());
            String nickname = user != null ? user.getNickname() : "Unknown";

            if (lastScore == null || !lastScore.equals(record.getTotalScore())) {
                rank += sameRankCount + 1;
                sameRankCount = 0;
                lastScore = record.getTotalScore();
            } else {
                sameRankCount++;
            }

            RankingItem item = new RankingItem();
            item.setRank(rank);
            item.setUserId(record.getUserId());
            item.setNickname(nickname);
            item.setScore(record.getTotalScore());
            item.setSubmitTime(record.getSubmitTime());
            ranking.add(item);
        }

        return ranking;
    }

    @lombok.Data
    public static class RankingItem {
        private Integer rank;
        private Long userId;
        private String nickname;
        private Integer score;
        private java.time.LocalDateTime submitTime;
    }
}
