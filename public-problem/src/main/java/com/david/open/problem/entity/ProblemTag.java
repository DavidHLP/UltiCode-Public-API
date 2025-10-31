package com.david.open.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("problem_tags")
public class ProblemTag {

    @TableId(value = "problem_id", type = IdType.INPUT)
    private Long problemId;

    @TableField("tag_id")
    private Long tagId;
}
