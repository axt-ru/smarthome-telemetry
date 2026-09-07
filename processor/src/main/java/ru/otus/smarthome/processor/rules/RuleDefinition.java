package ru.otus.smarthome.processor.rules;

public record RuleDefinition(
        long id,
        String name,
        Long deviceId,
        String room,
        String metric,
        String conditionType,
        Double thresholdLow,
        Double thresholdHigh,
        int consecutiveCount,
        int cooldownSeconds,
        String channel) {}
