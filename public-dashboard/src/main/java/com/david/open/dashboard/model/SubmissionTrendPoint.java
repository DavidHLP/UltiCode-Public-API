package com.david.open.dashboard.model;

import java.time.LocalDate;

/**
 * 展示提交趋势的每日统计数据。
 *
 * @param date     日期
 * @param total    当日提交总量
 * @param accepted 当日通过次数
 */
public record SubmissionTrendPoint(
        LocalDate date,
        long total,
        long accepted) {
}
