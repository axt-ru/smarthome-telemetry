package ru.otus.smarthome.gateway.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.otus.smarthome.common.MeasurementDto;
import ru.otus.smarthome.common.Queues;

@Component
public class TelemetryPublisher {
    private static final Logger log = LoggerFactory.getLogger(TelemetryPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public TelemetryPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public Mono<Void> publish(MeasurementDto measurement) {
        return Mono.fromRunnable(() -> rabbitTemplate.convertAndSend(Queues.EXCHANGE, Queues.ROUTING_KEY, measurement))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("не удалось опубликовать показание {}", measurement, ex))
                .then();
    }
}
