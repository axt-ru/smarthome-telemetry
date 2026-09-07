package ru.otus.smarthome.gateway.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.otus.smarthome.common.MeasurementDto;
import ru.otus.smarthome.common.Metric;

class LiveTelemetryStreamTest {

    @Test
    @DisplayName("поток горячий: подписчик получает только то, что пришло после подписки")
    void shouldDeliverOnlyMeasurementsAfterSubscription() {
        var stream = new LiveTelemetryStream();
        stream.emit(measurement(1.0));

        StepVerifier.create(stream.stream())
                .then(() -> stream.emit(measurement(2.0)))
                .assertNext(measurement -> assertThat(measurement.value()).isEqualTo(2.0))
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("одно показание доходит до всех подписчиков")
    void shouldBroadcastToEverySubscriber() {
        var stream = new LiveTelemetryStream();
        var first = new ArrayList<MeasurementDto>();
        var second = new ArrayList<MeasurementDto>();

        stream.stream().subscribe(first::add);
        stream.stream().subscribe(second::add);

        stream.emit(measurement(3.0));

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        assertThat(first.getFirst().value()).isEqualTo(3.0);
        assertThat(second.getFirst().value()).isEqualTo(3.0);
    }

    private MeasurementDto measurement(double value) {
        return new MeasurementDto("Кухня-TEMPERATURE", "Кухня", Metric.TEMPERATURE, Instant.now(), value);
    }
}
