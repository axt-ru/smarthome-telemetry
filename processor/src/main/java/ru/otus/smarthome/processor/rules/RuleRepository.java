package ru.otus.smarthome.processor.rules;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RuleRepository {

    private static final String LOAD_SQL =
            """
            select id, name, device_id, room, metric, condition_type,
                   threshold_low, threshold_high, consecutive_count, cooldown_seconds, channel
            from alert_rule
            where enabled = true and condition_type <> 'NO_DATA'
            """;

    private final JdbcTemplate jdbcTemplate;

    public RuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RuleDefinition> loadEnabled() {
        return jdbcTemplate.query(
                LOAD_SQL,
                (rs, rowNum) -> new RuleDefinition(
                        rs.getLong("id"),
                        rs.getString("name"),
                        (Long) rs.getObject("device_id"),
                        rs.getString("room"),
                        rs.getString("metric"),
                        rs.getString("condition_type"),
                        (Double) rs.getObject("threshold_low"),
                        (Double) rs.getObject("threshold_high"),
                        rs.getInt("consecutive_count"),
                        rs.getInt("cooldown_seconds"),
                        rs.getString("channel")));
    }
}
