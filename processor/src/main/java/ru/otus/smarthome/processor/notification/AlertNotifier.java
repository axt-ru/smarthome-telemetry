package ru.otus.smarthome.processor.notification;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlertNotifier {
    private static final Logger log = LoggerFactory.getLogger(AlertNotifier.class);

    private static final String INSERT_SQL =
            """
            insert into alert_event (rule_id, device_id, fired_at, value, message, delivered)
            values (?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, NotificationChannel> channels;
    private final Map<String, Instant> lastFired = new ConcurrentHashMap<>();

    public AlertNotifier(JdbcTemplate jdbcTemplate, List<NotificationChannel> channels) {
        this.jdbcTemplate = jdbcTemplate;
        this.channels = channels.stream().collect(Collectors.toMap(NotificationChannel::name, Function.identity()));
    }

    public void fire(long ruleId, long deviceId, int cooldownSeconds, String channelName, double value, String message) {
        var key = ruleId + "|" + deviceId;
        var now = Instant.now();
        var previous = lastFired.get(key);
        if (previous != null && previous.plus(Duration.ofSeconds(cooldownSeconds)).isAfter(now)) {
            return;
        }
        lastFired.put(key, now);

        var delivered = deliver(channelName, message);
        jdbcTemplate.update(INSERT_SQL, ruleId, deviceId, Timestamp.from(now), value, message, delivered);
    }

    private boolean deliver(String channelName, String message) {
        var channel = channels.get(channelName);
        if (channel == null) {
            log.error("неизвестный канал доставки: {}", channelName);
            return false;
        }
        try {
            channel.send(message);
            return true;
        } catch (Exception ex) {
            log.error("канал {} не смог доставить оповещение", channelName, ex);
            return false;
        }
    }
}
