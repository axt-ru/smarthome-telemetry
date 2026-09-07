package ru.otus.smarthome.processor;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.otus.smarthome.common.MeasurementDto;
import ru.otus.smarthome.common.Queues;
import ru.otus.smarthome.processor.store.MeasurementWriter;

@Component
public class MeasurementListener {
    private static final Logger log = LoggerFactory.getLogger(MeasurementListener.class);

    private final MeasurementWriter writer;
    private final AtomicLong total = new AtomicLong();

    public MeasurementListener(MeasurementWriter writer) {
        this.writer = writer;
    }

    @RabbitListener(queues = Queues.QUEUE, containerFactory = "batchListenerFactory")
    public void onBatch(List<MeasurementDto> batch) {
        writer.write(batch);
        log.info(
                "записана пачка: {} показаний, всего: {}, поток: {}",
                batch.size(),
                total.addAndGet(batch.size()),
                Thread.currentThread());
    }
}
