package com.david.open.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("problem_statements")
public class ProblemStatement {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("problem_id")
    private Long problemId;

    @TableField("lang_code")
    private String langCode;

    private String title;

    @TableField("description_md")
    private String descriptionMd;

    @TableField("constraints_md")
    private String constraintsMd;

    @TableField("examples_md")
    private String examplesMd;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
