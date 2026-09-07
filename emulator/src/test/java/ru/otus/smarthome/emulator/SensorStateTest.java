package ru.otus.smarthome.emulator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.otus.smarthome.common.Metric;

class SensorStateTest {

    private static final int ITERATIONS = 10_000;

    @ParameterizedTest
    @EnumSource(Metric.class)
    @DisplayName("значения датчика не выходят за физически осмысленные пределы")
    void shouldStayInRange(Metric metric) {
        var sensor = new SensorState("Кухня", metric);
        var min = Double.MAX_VALUE;
        var max = -Double.MAX_VALUE;

        for (var i = 0; i < ITERATIONS; i++) {
            var value = sensor.nextValue();
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        assertThat(min).isGreaterThanOrEqualTo(expectedMin(metric));
        assertThat(max).isLessThanOrEqualTo(expectedMax(metric));
        assertThat(max).isGreaterThan(min);
    }

    @ParameterizedTest
    @EnumSource(Metric.class)
    @DisplayName("идентификатор датчика собирается из комнаты и метрики")
    void shouldBuildDeviceId(Metric metric) {
        var sensor = new SensorState("Спальня", metric);

        assertThat(sensor.deviceId()).isEqualTo("Спальня-" + metric.name());
        assertThat(sensor.room()).isEqualTo("Спальня");
        assertThat(sensor.metric()).isEqualTo(metric);
    }

    private double expectedMin(Metric metric) {
        return switch (metric) {
            case TEMPERATURE -> 16;
            case HUMIDITY -> 25;
            case CO2 -> 400;
            case POWER -> 0;
        };
    }

    private double expectedMax(Metric metric) {
        return switch (metric) {
            case TEMPERATURE -> 28;
            case HUMIDITY -> 70;
            case CO2 -> 1400;
            case POWER -> 3000;
        };
    }
}
