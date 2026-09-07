package ru.otus.smarthome.gateway.api;

import java.time.Instant;

public record DeviceView(String deviceId, String room, String metric, String unit, Instant lastSeenAt) {}
