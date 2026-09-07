package ru.otus.smarthome.common;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopology {

    @Bean
    public DirectExchange telemetryExchange() {
        return new DirectExchange(Queues.EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(Queues.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue measurementQueue() {
        return QueueBuilder.durable(Queues.QUEUE)
                .deadLetterExchange(Queues.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(Queues.ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(Queues.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding measurementBinding(Queue measurementQueue, DirectExchange telemetryExchange) {
        return BindingBuilder.bind(measurementQueue).to(telemetryExchange).with(Queues.ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(Queues.ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
