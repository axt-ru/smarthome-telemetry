package ru.otus.smarthome.gateway.telemetry;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import ru.otus.smarthome.common.MeasurementDto;

@Component
public class LiveTelemetryStream {

    private final Sinks.Many<MeasurementDto> sink = Sinks.many().multicast().directBestEffort();

    public void emit(MeasurementDto measurement) {
        sink.tryEmitNext(measurement);
    }

    public Flux<MeasurementDto> stream() {
        return sink.asFlux();
    }
}
