package com.david.open.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.david.open.problem.entity.Problem;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {}
