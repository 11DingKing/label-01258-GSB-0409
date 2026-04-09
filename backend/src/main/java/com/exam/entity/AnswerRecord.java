package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("answer_record")
public class AnswerRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long examRecordId;
    private Long questionId;
    private String userAnswer;
    private Integer score;
    private String aiComment;
    private Boolean isGraded;
    
    @TableField(exist = false)
    private Question question;
}
