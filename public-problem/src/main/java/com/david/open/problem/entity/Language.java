package com.david.open.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("languages")
public class Language {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String code;

    @TableField("display_name")
    private String displayName;

    @TableField("runtime_image")
    private String runtimeImage;

    @TableField("is_active")
    private Integer isActive;
}
