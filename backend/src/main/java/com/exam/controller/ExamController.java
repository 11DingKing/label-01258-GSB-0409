package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.SubmitExamRequest;
import com.exam.entity.ExamRecord;
import com.exam.entity.Paper;
import com.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping("/available")
    public Result<List<Paper>> getAvailable() {
        return Result.success(examService.getAvailablePapers());
    }

    @PostMapping("/start/{paperId}")
    public Result<ExamRecord> start(@PathVariable Long paperId) {
        return Result.success(examService.startExam(paperId));
    }

    @GetMapping("/record/{id}")
    public Result<ExamRecord> getRecord(@PathVariable Long id) {
        return Result.success(examService.getExamRecord(id));
    }

    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody SubmitExamRequest request) {
        examService.submitExam(request);
        return Result.success();
    }
}
