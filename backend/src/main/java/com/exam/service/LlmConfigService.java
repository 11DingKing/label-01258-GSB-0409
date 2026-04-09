package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.common.BusinessException;
import com.exam.entity.LlmConfig;
import com.exam.mapper.LlmConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmConfigService {

    private final LlmConfigMapper llmConfigMapper;
    private final LlmService llmService;

    public List<LlmConfig> list() {
        return llmConfigMapper.selectList(
                new LambdaQueryWrapper<LlmConfig>().orderByDesc(LlmConfig::getIsDefault)
        );
    }

    @Transactional
    public void save(LlmConfig config) {
        if (config.getId() == null) {
            // Check duplicate name
            LlmConfig existing = llmConfigMapper.selectOne(
                    new LambdaQueryWrapper<LlmConfig>().eq(LlmConfig::getName, config.getName())
            );
            if (existing != null) {
                throw new BusinessException("配置名称已存在");
            }
            llmConfigMapper.insert(config);
            log.info("LLM config created: {}", config.getName());
        } else {
            llmConfigMapper.updateById(config);
            log.info("LLM config updated: {}", config.getName());
        }
    }

    public void delete(Long id) {
        LlmConfig config = llmConfigMapper.selectById(id);
        if (config != null && Boolean.TRUE.equals(config.getIsDefault())) {
            throw new BusinessException("无法删除默认配置");
        }
        llmConfigMapper.deleteById(id);
        log.info("LLM config deleted: id={}", id);
    }

    @Transactional
    public void setDefault(Long id) {
        // Clear all defaults
        llmConfigMapper.update(null,
                new LambdaUpdateWrapper<LlmConfig>().set(LlmConfig::getIsDefault, false)
        );
        // Set new default
        llmConfigMapper.update(null,
                new LambdaUpdateWrapper<LlmConfig>()
                        .eq(LlmConfig::getId, id)
                        .set(LlmConfig::getIsDefault, true)
        );
        log.info("LLM config set as default: id={}", id);
    }

    @Transactional
    public String testConnection(Long id) {
        LlmConfig config = llmConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        String result = llmService.testConnection(config);
        
        // 测试连接成功后自动设为默认配置
        if (result.startsWith("✅")) {
            setDefault(id);
            log.info("LLM config auto set as default after successful connection test: {}", config.getName());
        }
        
        return result;
    }

    public LlmConfig getDefault() {
        return llmConfigMapper.selectOne(
                new LambdaQueryWrapper<LlmConfig>().eq(LlmConfig::getIsDefault, true)
        );
    }
}
