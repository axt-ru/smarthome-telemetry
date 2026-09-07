package ru.otus.smarthome.gateway.api;

import java.time.Instant;

public record AlertView(long id, String ruleName, String room, String metric, Instant firedAt, String message) {}
