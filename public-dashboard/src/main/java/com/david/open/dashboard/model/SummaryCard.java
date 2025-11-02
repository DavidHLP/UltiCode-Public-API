package com.david.open.dashboard.model;

/**
 * 用于首页统计卡片的数据模型。
 *
 * @param key         唯一标识
 * @param title       卡片标题
 * @param value       主显示值
 * @param description 辅助描述文本
 */
public record SummaryCard(
        String key,
        String title,
        String value,
        String description) {
}
