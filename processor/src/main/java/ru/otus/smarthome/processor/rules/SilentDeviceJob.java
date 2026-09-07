package ru.otus.smarthome.processor.rules;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.otus.smarthome.processor.notification.AlertNotifier;

@Component
public class SilentDeviceJob {

    private static final String SQL =
            """
            select r.id as rule_id, r.name as rule_name, r.channel as channel,
                   r.cooldown_seconds as cooldown_seconds,
                   d.id as device_id, d.room as room, d.metric as metric
            from alert_rule r
            join device d on (r.device_id is null or r.device_id = d.id)
                         and (r.room is null or r.room = d.room)
                         and r.metric = d.metric
            where r.enabled = true
              and r.condition_type = 'NO_DATA'
              and d.last_seen_at is not null
              and d.last_seen_at < ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AlertNotifier notifier;

    @Value("${processor.rules.silence-seconds}")
    private long silenceSeconds;

    public SilentDeviceJob(JdbcTemplate jdbcTemplate, AlertNotifier notifier) {
        this.jdbcTemplate = jdbcTemplate;
        this.notifier = notifier;
    }

    @Scheduled(fixedDelayString = "${processor.rules.silence-check-delay-millis}")
    public void checkSilence() {
        var silentSince = Timestamp.from(Instant.now().minusSeconds(silenceSeconds));

        jdbcTemplate.query(
                SQL,
                rs -> {
                    var message = "%s: %s, %s молчит больше %d с"
                            .formatted(
                                    rs.getString("rule_name"),
                                    rs.getString("room"),
                                    rs.getString("metric"),
                                    silenceSeconds);
                    notifier.fire(
                            rs.getLong("rule_id"),
                            rs.getLong("device_id"),
                            rs.getInt("cooldown_seconds"),
                            rs.getString("channel"),
                            0,
                            message);
                },
                silentSince);
    }
}
