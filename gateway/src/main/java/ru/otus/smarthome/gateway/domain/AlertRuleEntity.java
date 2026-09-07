package ru.otus.smarthome.gateway.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("alert_rule")
public record AlertRuleEntity(
        @Id Long id,
        String name,
        Long deviceId,
        String room,
        String metric,
        String conditionType,
        Double thresholdLow,
        Double thresholdHigh,
        Integer consecutiveCount,
        Integer cooldownSeconds,
        String channel,
        Boolean enabled,
        Instant createdAt) {}
