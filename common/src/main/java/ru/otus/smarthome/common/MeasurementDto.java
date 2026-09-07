package ru.otus.smarthome.common;

import java.time.Instant;

public record MeasurementDto(String deviceId, String room, Metric metric, Instant measuredAt, double value) {}
