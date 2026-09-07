package ru.otus.smarthome.gateway.api;

public record RuleView(
        Long id,
        String name,
        String room,
        String metric,
        String conditionType,
        Double thresholdLow,
        Double thresholdHigh,
        Integer consecutiveCount,
        Integer cooldownSeconds,
        String channel,
        Boolean enabled) {}
