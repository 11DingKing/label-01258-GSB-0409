package com.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class SubmitExamRequest {
    @NotNull(message = "Exam record ID is required")
    private Long examRecordId;
    
    private List<AnswerItem> answers;
    
    @Data
    public static class AnswerItem {
        private Long questionId;
        private String answer;
    }
}
