package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.ExamRecord;
import com.exam.service.GradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;

    @GetMapping("/pending")
    public Result<List<ExamRecord>> getPending() {
        return Result.success(gradingService.getPendingList());
    }

    @PostMapping("/ai-grade/{recordId}")
    public Result<Void> aiGrade(@PathVariable Long recordId) {
        gradingService.aiGrade(recordId);
        return Result.success();
    }

    @PostMapping("/manual")
    public Result<Void> manualGrade(
            @RequestParam Long answerId,
            @RequestParam Integer score,
            @RequestParam(required = false) String comment) {
        gradingService.manualGrade(answerId, score, comment);
        return Result.success();
    }
}
