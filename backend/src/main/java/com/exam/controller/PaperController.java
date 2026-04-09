package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.AiGenerateRequest;
import com.exam.entity.Paper;
import com.exam.entity.Question;
import com.exam.service.PaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paper")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @GetMapping("/list")
    public Result<List<Paper>> list() {
        return Result.success(paperService.list());
    }

    @GetMapping("/{id}")
    public Result<Paper> getById(@PathVariable Long id) {
        return Result.success(paperService.getById(id));
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody Paper paper) {
        paperService.save(paper);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        paperService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        paperService.publish(id);
        return Result.success();
    }

    @PutMapping("/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable Long id) {
        paperService.unpublish(id);
        return Result.success();
    }

    @PostMapping("/ai-generate")
    public Result<List<Question>> aiGenerate(@Valid @RequestBody AiGenerateRequest request) {
        return Result.success(paperService.aiGenerate(request));
    }
}
