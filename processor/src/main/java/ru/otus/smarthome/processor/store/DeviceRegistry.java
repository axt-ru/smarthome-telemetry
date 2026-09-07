package ru.otus.smarthome.processor.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.otus.smarthome.common.MeasurementDto;

@Component
public class DeviceRegistry {
    private static final Logger log = LoggerFactory.getLogger(DeviceRegistry.class);

    private static final String INSERT_SQL =
            """
            insert into device (external_id, name, room, metric, unit)
            values (?, ?, ?, ?, ?)
            on conflict (external_id) do nothing
            """;

    private static final String SELECT_SQL = "select id from device where external_id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, Long> cache = new ConcurrentHashMap<>();

    public DeviceRegistry(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long resolve(MeasurementDto measurement) {
        return cache.computeIfAbsent(measurement.deviceId(), externalId -> register(externalId, measurement));
    }

    private long register(String externalId, MeasurementDto measurement) {
        jdbcTemplate.update(
                INSERT_SQL,
                externalId,
                "%s %s".formatted(measurement.room(), measurement.metric().name()),
                measurement.room(),
                measurement.metric().name(),
                measurement.metric().unit());

        var id = jdbcTemplate.queryForObject(SELECT_SQL, Long.class, externalId);
        log.info("зарегистрировано устройство {} с id {}", externalId, id);
        return id;
    }
}
