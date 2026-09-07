package ru.otus.smarthome.emulator;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import ru.otus.smarthome.common.MeasurementDto;
import ru.otus.smarthome.common.Metric;

@Component
public class SensorEmulator implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SensorEmulator.class);

    private final WebClient gatewayClient;
    private final EmulatorProperties properties;
    private final List<SensorState> sensors;

    public SensorEmulator(WebClient gatewayClient, EmulatorProperties properties) {
        this.gatewayClient = gatewayClient;
        this.properties = properties;
        this.sensors = properties.rooms().stream()
                .flatMap(room -> Stream.of(Metric.values()).map(metric -> new SensorState(room, metric)))
                .toList();
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("эмулятор запущен, датчиков: {}, интервал: {} мс", sensors.size(), properties.intervalMillis());

        Flux.interval(Duration.ofMillis(properties.intervalMillis()))
                .concatMap(tick -> sendBatch())
                .doOnError(ex -> log.error("ошибка отправки показаний", ex))
                .retryWhen(reactor.util.retry.Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(10)))
                .subscribe();
    }

    private reactor.core.publisher.Mono<Void> sendBatch() {
        var now = Instant.now();
        var batch = sensors.stream()
                .map(sensor -> new MeasurementDto(
                        sensor.deviceId(), sensor.room(), sensor.metric(), now, sensor.nextValue()))
                .toList();

        return gatewayClient
                .post()
                .uri("/api/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(batch)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
