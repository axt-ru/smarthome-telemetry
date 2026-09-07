package ru.otus.smarthome.gateway.api;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.otus.smarthome.common.MeasurementDto;
import ru.otus.smarthome.gateway.telemetry.TelemetryPublisher;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {
    private static final Logger log = LoggerFactory.getLogger(TelemetryController.class);

    private static final int LOG_EVERY = 100;

    private final TelemetryPublisher publisher;
    private final AtomicLong received = new AtomicLong();

    public TelemetryController(TelemetryPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE})
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> accept(@RequestBody Flux<MeasurementDto> measurements) {
        return measurements
                .flatMap(measurement -> publisher.publish(measurement).thenReturn(measurement))
                .doOnNext(this::countAndLog)
                .then();
    }

    private void countAndLog(MeasurementDto measurement) {
        var total = received.incrementAndGet();
        if (total % LOG_EVERY == 0) {
            log.info("принято показаний: {}, последнее: {}", total, measurement);
        }
    }
}
