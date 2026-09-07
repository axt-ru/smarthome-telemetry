package ru.otus.smarthome.processor.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.otus.smarthome.common.RabbitTopology;

@Configuration
@Import(RabbitTopology.class)
public class RabbitConfig {

    @Value("${processor.batch-size}")
    private int batchSize;

    @Value("${processor.batch-timeout-millis}")
    private long batchTimeoutMillis;

    @Bean
    public SimpleRabbitListenerContainerFactory batchListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(batchSize);
        factory.setReceiveTimeout(batchTimeoutMillis);
        return factory;
    }
}
