package ru.otus.smarthome.processor.rules;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.otus.smarthome.common.MeasurementDto;
import ru.otus.smarthome.processor.notification.AlertNotifier;
import ru.otus.smarthome.processor.store.DeviceRegistry;

@Component
public class RuleEngine {
    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final RuleRepository ruleRepository;
    private final DeviceRegistry deviceRegistry;
    private final AlertNotifier notifier;

    private final AtomicReference<List<RuleDefinition>> rules = new AtomicReference<>(List.of());
    private final Map<String, Integer> hitCounters = new ConcurrentHashMap<>();

    public RuleEngine(RuleRepository ruleRepository, DeviceRegistry deviceRegistry, AlertNotifier notifier) {
        this.ruleRepository = ruleRepository;
        this.deviceRegistry = deviceRegistry;
        this.notifier = notifier;
    }

    @Scheduled(initialDelay = 0, fixedDelayString = "${processor.rules.refresh-delay-millis}")
    public void reload() {
        var loaded = ruleRepository.loadEnabled();
        if (loaded.size() != rules.get().size()) {
            log.info("правил загружено: {}", loaded.size());
        }
        rules.set(loaded);
    }

    public void evaluate(List<MeasurementDto> batch) {
        var active = rules.get();
        if (active.isEmpty()) {
            return;
        }
        for (var measurement : batch) {
            for (var rule : active) {
                if (matches(rule, measurement)) {
                    check(rule, measurement);
                }
            }
        }
    }

    private boolean matches(RuleDefinition rule, MeasurementDto measurement) {
        if (!rule.metric().equals(measurement.metric().name())) {
            return false;
        }
        if (rule.room() != null && !rule.room().equals(measurement.room())) {
            return false;
        }
        return rule.deviceId() == null || rule.deviceId() == deviceRegistry.resolve(measurement);
    }

    private void check(RuleDefinition rule, MeasurementDto measurement) {
        var key = rule.id() + "|" + measurement.deviceId();

        if (!violates(rule, measurement.value())) {
            hitCounters.remove(key);
            return;
        }

        var hits = hitCounters.merge(key, 1, Integer::sum);
        if (hits < rule.consecutiveCount()) {
            return;
        }
        hitCounters.remove(key);

        notifier.fire(
                rule.id(),
                deviceRegistry.resolve(measurement),
                rule.cooldownSeconds(),
                rule.channel(),
                measurement.value(),
                describe(rule, measurement));
    }

    private boolean violates(RuleDefinition rule, double value) {
        return switch (rule.conditionType()) {
            case "GT" -> rule.thresholdHigh() != null && value > rule.thresholdHigh();
            case "LT" -> rule.thresholdLow() != null && value < rule.thresholdLow();
            case "OUT_OF_RANGE" -> (rule.thresholdLow() != null && value < rule.thresholdLow())
                    || (rule.thresholdHigh() != null && value > rule.thresholdHigh());
            default -> false;
        };
    }

    private String describe(RuleDefinition rule, MeasurementDto measurement) {
        return "%s: %s, %s = %s".formatted(
                rule.name(), measurement.room(), measurement.metric().name(), measurement.value());
    }
}
