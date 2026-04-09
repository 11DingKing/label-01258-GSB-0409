package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long paperId;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Integer totalScore;
    private String status;          // IN_PROGRESS, SUBMITTED
    private String gradingStatus;   // PENDING, GRADING, COMPLETED
    
    @TableField(exist = false)
    private String paperTitle;
    
    @TableField(exist = false)
    private String username;
    
    @TableField(exist = false)
    private List<AnswerRecord> answers;
}
