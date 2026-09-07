package ru.otus.smarthome.processor.aggregation;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AggregationJob {
    private static final Logger log = LoggerFactory.getLogger(AggregationJob.class);

    private static final String MINUTE_SQL =
            """
            insert into measurement_agg (device_id, window_type, bucket_start, min_value, max_value, avg_value, cnt)
            select device_id, 'MINUTE', date_trunc('minute', measured_at),
                   min(value), max(value), avg(value), count(*)
            from measurement
            where measured_at >= ?
            group by device_id, date_trunc('minute', measured_at)
            on conflict (device_id, window_type, bucket_start) do update
            set min_value = excluded.min_value,
                max_value = excluded.max_value,
                avg_value = excluded.avg_value,
                cnt = excluded.cnt
            """;

    private static final String HOUR_SQL =
            """
            insert into measurement_agg (device_id, window_type, bucket_start, min_value, max_value, avg_value, cnt)
            select device_id, 'HOUR', date_trunc('hour', bucket_start),
                   min(min_value), max(max_value), sum(avg_value * cnt) / sum(cnt), sum(cnt)
            from measurement_agg
            where window_type = 'MINUTE' and bucket_start >= ?
            group by device_id, date_trunc('hour', bucket_start)
            on conflict (device_id, window_type, bucket_start) do update
            set min_value = excluded.min_value,
                max_value = excluded.max_value,
                avg_value = excluded.avg_value,
                cnt = excluded.cnt
            """;

    private static final String CLEANUP_SQL = "delete from measurement where measured_at < ?";

    private final JdbcTemplate jdbcTemplate;

    @Value("${processor.aggregation.lookback-minutes}")
    private int lookbackMinutes;

    @Value("${processor.retention.raw-hours}")
    private int rawHours;

    public AggregationJob(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelayString = "${processor.aggregation.minute-delay-millis}")
    public void rollUpMinutes() {
        var from = Timestamp.from(Instant.now().minus(lookbackMinutes, ChronoUnit.MINUTES));
        var affected = jdbcTemplate.update(MINUTE_SQL, from);
        log.info("минутных окон обновлено: {}", affected);
    }

    @Scheduled(fixedDelayString = "${processor.aggregation.hour-delay-millis}")
    public void rollUpHours() {
        var from = Timestamp.from(Instant.now().minus(lookbackMinutes + 60L, ChronoUnit.MINUTES));
        var affected = jdbcTemplate.update(HOUR_SQL, from);
        log.info("часовых окон обновлено: {}", affected);
    }

    @Scheduled(fixedDelayString = "${processor.retention.cleanup-delay-millis}")
    public void cleanupRaw() {
        var before = Timestamp.from(Instant.now().minus(rawHours, ChronoUnit.HOURS));
        var deleted = jdbcTemplate.update(CLEANUP_SQL, before);
        if (deleted > 0) {
            log.info("удалено сырых показаний старше {} ч: {}", rawHours, deleted);
        }
    }
}
