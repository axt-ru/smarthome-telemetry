package ru.otus.smarthome.gateway.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("measurement_agg")
public record MeasurementAggEntity(
        @Id Long id,
        Long deviceId,
        String windowType,
        Instant bucketStart,
        Double minValue,
        Double maxValue,
        Double avgValue,
        Integer cnt) {}
