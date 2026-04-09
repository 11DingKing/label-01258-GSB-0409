package com.exam.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiGenerateRequest {
    @NotBlank(message = "请输入学科")
    private String subject;
    
    private String topic;
    
    // 通用题型
    @Min(value = 0, message = "数量不能为负数")
    private Integer singleChoiceCount = 0;
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer multiChoiceCount = 0;
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer trueFalseCount = 0;
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer fillBlankCount = 0;
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer shortAnswerCount = 0;
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer essayCount = 0;
    
    // 学科专属题型
    @Min(value = 0, message = "数量不能为负数")
    private Integer readingCount = 0;        // 阅读理解
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer compositionCount = 0;    // 作文
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer clozeCount = 0;          // 完形填空
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer translationCount = 0;    // 翻译
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer calculationCount = 0;    // 计算题
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer proofCount = 0;          // 证明题
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer experimentCount = 0;     // 实验题
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer analysisCount = 0;       // 材料分析
    
    private String difficulty = "MEDIUM"; // EASY, MEDIUM, HARD
    
    /**
     * 检查是否至少选择了一种题型
     */
    public boolean hasAnyQuestionType() {
        return (singleChoiceCount != null && singleChoiceCount > 0) ||
               (multiChoiceCount != null && multiChoiceCount > 0) ||
               (trueFalseCount != null && trueFalseCount > 0) ||
               (fillBlankCount != null && fillBlankCount > 0) ||
               (shortAnswerCount != null && shortAnswerCount > 0) ||
               (essayCount != null && essayCount > 0) ||
               (readingCount != null && readingCount > 0) ||
               (compositionCount != null && compositionCount > 0) ||
               (clozeCount != null && clozeCount > 0) ||
               (translationCount != null && translationCount > 0) ||
               (calculationCount != null && calculationCount > 0) ||
               (proofCount != null && proofCount > 0) ||
               (experimentCount != null && experimentCount > 0) ||
               (analysisCount != null && analysisCount > 0);
    }
}
