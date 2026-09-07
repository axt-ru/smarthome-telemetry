package ru.otus.smarthome.gateway.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("device")
public record DeviceEntity(
        @Id Long id,
        String externalId,
        String name,
        String room,
        String metric,
        String unit,
        Boolean enabled,
        Instant createdAt,
        Instant lastSeenAt) {}
