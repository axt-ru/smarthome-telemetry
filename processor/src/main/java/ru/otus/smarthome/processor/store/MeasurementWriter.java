package ru.otus.smarthome.processor.store;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.smarthome.common.MeasurementDto;

@Component
public class MeasurementWriter {

    private static final String INSERT_SQL =
            "insert into measurement (device_id, measured_at, value) values (?, ?, ?)";

    private static final String TOUCH_SQL = "update device set last_seen_at = ? where id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final DeviceRegistry deviceRegistry;

    public MeasurementWriter(JdbcTemplate jdbcTemplate, DeviceRegistry deviceRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.deviceRegistry = deviceRegistry;
    }

    @Transactional
    public void write(List<MeasurementDto> batch) {
        var rows = batch.stream()
                .map(measurement -> new Object[] {
                    deviceRegistry.resolve(measurement),
                    Timestamp.from(measurement.measuredAt()),
                    measurement.value()
                })
                .toList();

        jdbcTemplate.batchUpdate(INSERT_SQL, rows);

        var now = Timestamp.from(Instant.now());
        var touched = rows.stream()
                .map(row -> new Object[] {now, row[0]})
                .distinct()
                .toList();
        jdbcTemplate.batchUpdate(TOUCH_SQL, touched);
    }
}
