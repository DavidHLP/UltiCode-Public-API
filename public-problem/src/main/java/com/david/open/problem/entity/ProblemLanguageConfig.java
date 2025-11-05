package com.david.open.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("problem_language_configs")
public class ProblemLanguageConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("problem_id")
    private Long problemId;

    @TableField("language_id")
    private Integer languageId;

    @TableField("function_name")
    private String functionName;

    @TableField("starter_code")
    private String starterCode;
}
