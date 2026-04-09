package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.ExamRecord;
import com.exam.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/score")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @GetMapping("/my")
    public Result<List<ExamRecord>> getMyScores() {
        return Result.success(scoreService.getMyScores());
    }

    @GetMapping("/detail/{recordId}")
    public Result<ExamRecord> getDetail(@PathVariable Long recordId) {
        return Result.success(scoreService.getScoreDetail(recordId));
    }

    @GetMapping("/ranking/{paperId}")
    public Result<List<ScoreService.RankingItem>> getRanking(@PathVariable Long paperId) {
        return Result.success(scoreService.getRanking(paperId));
    }
}
