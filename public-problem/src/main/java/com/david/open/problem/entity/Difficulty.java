package com.david.open.problem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("difficulties")
public class Difficulty {

    @TableId private Integer id;

    private String code;

    @TableField("sort_key")
    private Integer sortKey;
}
