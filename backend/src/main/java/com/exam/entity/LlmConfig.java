package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("llm_config")
public class LlmConfig {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String provider;
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
