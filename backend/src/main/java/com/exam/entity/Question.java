package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long paperId;
    private String type;       // SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE, FILL_BLANK, SHORT_ANSWER, ESSAY
    private String content;
    private String options;    // JSON array for choice questions
    private String answer;
    private Integer score;
    private Integer sortOrder;
}
