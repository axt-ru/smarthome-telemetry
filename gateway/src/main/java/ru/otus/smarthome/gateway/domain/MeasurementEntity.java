package ru.otus.smarthome.gateway.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("measurement")
public record MeasurementEntity(@Id Long id, Long deviceId, Instant measuredAt, Double value) {}
