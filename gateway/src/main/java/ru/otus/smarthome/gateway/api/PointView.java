package ru.otus.smarthome.gateway.api;

import java.time.Instant;

public record PointView(Instant at, double value) {}
