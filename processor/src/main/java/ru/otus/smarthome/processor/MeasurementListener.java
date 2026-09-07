package ru.otus.smarthome.processor;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.otus.smarthome.common.MeasurementDto;
import ru.otus.smarthome.common.Queues;
import ru.otus.smarthome.processor.rules.RuleEngine;
import ru.otus.smarthome.processor.store.MeasurementWriter;

@Component
public class MeasurementListener {
    private static final Logger log = LoggerFactory.getLogger(MeasurementListener.class);

    private static final int LOG_EVERY_BATCHES = 10;

    private final MeasurementWriter writer;
    private final RuleEngine ruleEngine;
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong batches = new AtomicLong();

    public MeasurementListener(MeasurementWriter writer, RuleEngine ruleEngine) {
        this.writer = writer;
        this.ruleEngine = ruleEngine;
    }

    @RabbitListener(queues = Queues.QUEUE, containerFactory = "batchListenerFactory")
    public void onBatch(List<MeasurementDto> batch) {
        writer.write(batch);
        ruleEngine.evaluate(batch);

        var written = total.addAndGet(batch.size());
        if (batches.incrementAndGet() % LOG_EVERY_BATCHES == 0) {
            log.info("записана пачка: {} показаний, всего: {}", batch.size(), written);
        }
    }
}
