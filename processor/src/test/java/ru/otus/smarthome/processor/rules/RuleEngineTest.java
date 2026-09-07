package ru.otus.smarthome.processor.rules;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.smarthome.common.MeasurementDto;
import ru.otus.smarthome.common.Metric;
import ru.otus.smarthome.processor.notification.AlertNotifier;
import ru.otus.smarthome.processor.store.DeviceRegistry;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    private static final long DEVICE_ID = 42L;

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private DeviceRegistry deviceRegistry;

    @Mock
    private AlertNotifier notifier;

    private RuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RuleEngine(ruleRepository, deviceRegistry, notifier);
    }

    @Test
    @DisplayName("правило срабатывает только после нужного числа нарушений подряд")
    void shouldFireAfterConsecutiveViolations() {
        givenRules(rule("GT", null, 24.0, 3));
        lenientDevice();

        engine.evaluate(List.of(measurement("Детская", Metric.TEMPERATURE, 25.0)));
        engine.evaluate(List.of(measurement("Детская", Metric.TEMPERATURE, 25.5)));
        verify(notifier, never()).fire(anyLong(), anyLong(), anyInt(), anyString(), anyDouble(), anyString());

        engine.evaluate(List.of(measurement("Детская", Metric.TEMPERATURE, 26.0)));
        verify(notifier, times(1)).fire(eq(1L), eq(DEVICE_ID), eq(120), eq("LOG"), eq(26.0), anyString());
    }

    @Test
    @DisplayName("нормальное значение сбрасывает счётчик нарушений")
    void shouldResetCounterOnNormalValue() {
        givenRules(rule("GT", null, 24.0, 3));
        lenientDevice();

        engine.evaluate(List.of(measurement("Детская", Metric.TEMPERATURE, 25.0)));
        engine.evaluate(List.of(measurement("Детская", Metric.TEMPERATURE, 25.0)));
        engine.evaluate(List.of(measurement("Детская", Metric.TEMPERATURE, 20.0)));
        engine.evaluate(List.of(measurement("Детская", Metric.TEMPERATURE, 25.0)));
        engine.evaluate(List.of(measurement("Детская", Metric.TEMPERATURE, 25.0)));

        verify(notifier, never()).fire(anyLong(), anyLong(), anyInt(), anyString(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("условие вне диапазона ловит оба края")
    void shouldFireOnBothSidesOfRange() {
        givenRules(rule("OUT_OF_RANGE", 18.0, 25.0, 1));
        lenientDevice();

        engine.evaluate(List.of(measurement("Кухня", Metric.TEMPERATURE, 30.0)));
        engine.evaluate(List.of(measurement("Кухня", Metric.TEMPERATURE, 22.0)));
        engine.evaluate(List.of(measurement("Кухня", Metric.TEMPERATURE, 15.0)));

        verify(notifier, times(2)).fire(anyLong(), anyLong(), anyInt(), anyString(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("правило чужой комнаты не срабатывает")
    void shouldIgnoreOtherRoom() {
        givenRules(ruleForRoom("Детская"));

        engine.evaluate(List.of(measurement("Кухня", Metric.TEMPERATURE, 30.0)));

        verify(notifier, never()).fire(anyLong(), anyLong(), anyInt(), anyString(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("правило другой метрики не срабатывает")
    void shouldIgnoreOtherMetric() {
        givenRules(rule("GT", null, 24.0, 1));

        engine.evaluate(List.of(measurement("Детская", Metric.HUMIDITY, 90.0)));

        verify(notifier, never()).fire(anyLong(), anyLong(), anyInt(), anyString(), anyDouble(), anyString());
    }

    private void givenRules(RuleDefinition... rules) {
        when(ruleRepository.loadEnabled()).thenReturn(List.of(rules));
        engine.reload();
    }

    private void lenientDevice() {
        org.mockito.Mockito.lenient()
                .when(deviceRegistry.resolve(org.mockito.ArgumentMatchers.any()))
                .thenReturn(DEVICE_ID);
    }

    private RuleDefinition rule(String condition, Double low, Double high, int consecutive) {
        return new RuleDefinition(1L, "тест", null, null, "TEMPERATURE", condition, low, high, consecutive, 120, "LOG");
    }

    private RuleDefinition ruleForRoom(String room) {
        return new RuleDefinition(1L, "тест", null, room, "TEMPERATURE", "GT", null, 24.0, 1, 120, "LOG");
    }

    private MeasurementDto measurement(String room, Metric metric, double value) {
        return new MeasurementDto(room + "-" + metric.name(), room, metric, Instant.now(), value);
    }
}
