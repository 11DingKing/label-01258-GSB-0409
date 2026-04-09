package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("paper")
public class Paper {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private String description;
    private Integer totalScore;
    private Integer durationMinutes;
    private Long creatorId;
    private String status;
    private LocalDateTime createdAt;
    
    @TableField(exist = false)
    private List<Question> questions;
    
    @TableField(exist = false)
    private String creatorName;
}
