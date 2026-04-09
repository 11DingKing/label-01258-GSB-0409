package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.LlmConfig;
import com.exam.service.LlmConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmConfigController {

    private final LlmConfigService llmConfigService;

    @GetMapping("/list")
    public Result<List<LlmConfig>> list() {
        List<LlmConfig> configs = llmConfigService.list();
        // Mask API keys
        for (LlmConfig config : configs) {
            if (config.getApiKey() != null && config.getApiKey().length() > 8) {
                config.setApiKey(config.getApiKey().substring(0, 4) + "****" + 
                        config.getApiKey().substring(config.getApiKey().length() - 4));
            }
        }
        return Result.success(configs);
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody LlmConfig config) {
        llmConfigService.save(config);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        llmConfigService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        llmConfigService.setDefault(id);
        return Result.success();
    }

    @PostMapping("/test/{id}")
    public Result<String> test(@PathVariable Long id) {
        return Result.success(llmConfigService.testConnection(id));
    }
}
