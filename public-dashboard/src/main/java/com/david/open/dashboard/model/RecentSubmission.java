package com.david.open.dashboard.model;

import java.time.OffsetDateTime;

/**
 * 最近提交列表展示的单条记录。
 *
 * @param id           提交ID
 * @param username     用户名
 * @param displayName  用户展示昵称
 * @param email        用户邮箱
 * @param avatarUrl    用户头像地址
 * @param problemTitle 题目标题
 * @param verdict      判题结果
 * @param submittedAt  提交时间
 */
public record RecentSubmission(
        long id,
        String username,
        String displayName,
        String email,
        String avatarUrl,
        String problemTitle,
        String verdict,
        OffsetDateTime submittedAt) {
}
