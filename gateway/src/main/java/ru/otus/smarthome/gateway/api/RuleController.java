package ru.otus.smarthome.gateway.api;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.otus.smarthome.gateway.domain.AlertRuleEntity;
import ru.otus.smarthome.gateway.repository.AlertRuleRepository;

@RestController
@RequestMapping("/api")
public class RuleController {

    private static final String ALERTS_SQL =
            """
            select e.id as id, r.name as rule_name, d.room as room, d.metric as metric,
                   e.fired_at as fired_at, e.message as message
            from alert_event e
            join alert_rule r on r.id = e.rule_id
            join device d on d.id = e.device_id
            order by e.fired_at desc
            limit :limit
            """;

    private final AlertRuleRepository ruleRepository;
    private final DatabaseClient databaseClient;

    public RuleController(AlertRuleRepository ruleRepository, DatabaseClient databaseClient) {
        this.ruleRepository = ruleRepository;
        this.databaseClient = databaseClient;
    }

    @GetMapping("/rules")
    public Flux<RuleView> rules() {
        return ruleRepository.findAllOrdered().map(this::toView);
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RuleView> create(@RequestBody RuleView request) {
        var entity = new AlertRuleEntity(
                null,
                request.name(),
                null,
                blankToNull(request.room()),
                request.metric(),
                request.conditionType(),
                request.thresholdLow(),
                request.thresholdHigh(),
                request.consecutiveCount() == null ? 1 : request.consecutiveCount(),
                request.cooldownSeconds() == null ? 300 : request.cooldownSeconds(),
                request.channel() == null ? "LOG" : request.channel(),
                true,
                Instant.now());
        return ruleRepository.save(entity).map(this::toView);
    }

    @DeleteMapping("/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable("id") long id) {
        return ruleRepository.deleteById(id);
    }

    @GetMapping("/alerts")
    public Flux<AlertView> alerts(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        return databaseClient
                .sql(ALERTS_SQL)
                .bind("limit", limit)
                .map(row -> new AlertView(
                        row.get("id", Long.class),
                        row.get("rule_name", String.class),
                        row.get("room", String.class),
                        row.get("metric", String.class),
                        row.get("fired_at", Instant.class),
                        row.get("message", String.class)))
                .all();
    }

    private RuleView toView(AlertRuleEntity rule) {
        return new RuleView(
                rule.id(),
                rule.name(),
                rule.room(),
                rule.metric(),
                rule.conditionType(),
                rule.thresholdLow(),
                rule.thresholdHigh(),
                rule.consecutiveCount(),
                rule.cooldownSeconds(),
                rule.channel(),
                rule.enabled());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
