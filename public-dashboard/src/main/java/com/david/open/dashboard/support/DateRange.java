package com.david.open.dashboard.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 表示一个包含起止日期的闭区间，便于转换为数据库查询所需的时间范围。
 */
public record DateRange(LocalDate start, LocalDate end) {

    public DateRange {
        Objects.requireNonNull(start, "开始日期不能为空");
        Objects.requireNonNull(end, "结束日期不能为空");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
    }

    public LocalDateTime startDateTime() {
        return start.atStartOfDay();
    }

    public LocalDateTime endExclusiveDateTime() {
        return end.plusDays(1).atStartOfDay();
    }

    public long lengthInDays() {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }
}
