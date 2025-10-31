package com.david.open.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.david.open.problem.entity.ProblemTag;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ProblemTagMapper extends BaseMapper<ProblemTag> {

    @Select(
            """
            <script>
            SELECT
                pt.problem_id    AS problemId,
                t.id             AS tagId,
                t.name           AS name,
                t.slug           AS slug
            FROM problem_tags pt
            INNER JOIN tags t ON t.id = pt.tag_id
            WHERE pt.problem_id IN
            <foreach item="id" collection="problemIds" open="(" separator="," close=")">
                #{id}
            </foreach>
            ORDER BY pt.problem_id ASC, t.name ASC
            </script>
            """)
    List<TagRelationRow> selectTagsForProblems(@Param("problemIds") Collection<Long> problemIds);

    record TagRelationRow(Long problemId, Long tagId, String name, String slug) {}
}
