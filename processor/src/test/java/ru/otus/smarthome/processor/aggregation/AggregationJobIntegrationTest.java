package ru.otus.smarthome.processor.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AggregationJobIntegrationTest {

    private static final String MIGRATIONS = "filesystem:../gateway/src/main/resources/db/migration";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    private static JdbcTemplate jdbcTemplate;

    private AggregationJob job;
    private long deviceId;

    @BeforeAll
    static void prepareSchema() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");

        Flyway.configure().dataSource(dataSource).locations(MIGRATIONS).load().migrate();

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from measurement_agg");
        jdbcTemplate.update("delete from measurement");
        jdbcTemplate.update("delete from device");

        deviceId = jdbcTemplate.queryForObject(
                """
                insert into device (external_id, name, room, metric, unit)
                values ('Кухня-TEMPERATURE', 'Кухня TEMPERATURE', 'Кухня', 'TEMPERATURE', '°C')
                returning id
                """,
                Long.class);

        job = new AggregationJob(jdbcTemplate);
        ReflectionTestUtils.setField(job, "lookbackMinutes", 60);
        ReflectionTestUtils.setField(job, "rawHours", 24);
    }

    @Test
    @DisplayName("минутная свёртка считает минимум, максимум, среднее и количество")
    void shouldRollUpMinuteWindow() {
        var minute = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        insert(minute.plusSeconds(1), 20.0);
        insert(minute.plusSeconds(2), 24.0);
        insert(minute.plusSeconds(3), 22.0);

        job.rollUpMinutes();

        var row = jdbcTemplate.queryForMap(
                "select min_value, max_value, avg_value, cnt from measurement_agg where window_type = 'MINUTE'");

        assertThat((Double) row.get("min_value")).isEqualTo(20.0);
        assertThat((Double) row.get("max_value")).isEqualTo(24.0);
        assertThat((Double) row.get("avg_value")).isEqualTo(22.0);
        assertThat((Integer) row.get("cnt")).isEqualTo(3);
    }

    @Test
    @DisplayName("повторный запуск свёртки не плодит дубликаты и обновляет результат")
    void shouldBeIdempotent() {
        var minute = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        insert(minute.plusSeconds(1), 20.0);
        insert(minute.plusSeconds(2), 24.0);

        job.rollUpMinutes();
        job.rollUpMinutes();

        assertThat(countAgg("MINUTE")).isEqualTo(1);

        insert(minute.plusSeconds(3), 30.0);
        job.rollUpMinutes();

        assertThat(countAgg("MINUTE")).isEqualTo(1);
        var max = jdbcTemplate.queryForObject(
                "select max_value from measurement_agg where window_type = 'MINUTE'", Double.class);
        assertThat(max).isEqualTo(30.0);
    }

    @Test
    @DisplayName("часовая свёртка берёт взвешенное среднее из минутных окон")
    void shouldRollUpHourFromMinutes() {
        var hour = Instant.now().truncatedTo(ChronoUnit.HOURS);
        insertAgg(hour, 10.0, 10.0, 10.0, 1);
        insertAgg(hour.plus(1, ChronoUnit.MINUTES), 20.0, 20.0, 20.0, 3);

        job.rollUpHours();

        var row = jdbcTemplate.queryForMap(
                "select min_value, max_value, avg_value, cnt from measurement_agg where window_type = 'HOUR'");

        assertThat((Double) row.get("min_value")).isEqualTo(10.0);
        assertThat((Double) row.get("max_value")).isEqualTo(20.0);
        assertThat((Double) row.get("avg_value")).isEqualTo(17.5);
        assertThat((Integer) row.get("cnt")).isEqualTo(4);
    }

    @Test
    @DisplayName("сырые показания старше срока хранения удаляются, агрегаты остаются")
    void shouldCleanupOldRawData() {
        var old = Instant.now().minus(48, ChronoUnit.HOURS);
        insert(old, 15.0);
        insert(Instant.now(), 21.0);
        job.rollUpMinutes();

        job.cleanupRaw();

        var left = jdbcTemplate.queryForObject("select count(*) from measurement", Integer.class);
        assertThat(left).isEqualTo(1);
        assertThat(countAgg("MINUTE")).isGreaterThan(0);
    }

    private void insert(Instant at, double value) {
        jdbcTemplate.update(
                "insert into measurement (device_id, measured_at, value) values (?, ?, ?)",
                deviceId,
                Timestamp.from(at),
                value);
    }

    private void insertAgg(Instant bucketStart, double min, double max, double avg, int cnt) {
        jdbcTemplate.update(
                """
                insert into measurement_agg (device_id, window_type, bucket_start, min_value, max_value, avg_value, cnt)
                values (?, 'MINUTE', ?, ?, ?, ?, ?)
                """,
                deviceId,
                Timestamp.from(bucketStart),
                min,
                max,
                avg,
                cnt);
    }

    private int countAgg(String windowType) {
        return jdbcTemplate.queryForObject(
                "select count(*) from measurement_agg where window_type = ?", Integer.class, windowType);
    }
}
