package ru.otus.smarthome.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.otus.smarthome.common.RabbitTopology;

@Configuration
@Import(RabbitTopology.class)
public class RabbitConfig {}
