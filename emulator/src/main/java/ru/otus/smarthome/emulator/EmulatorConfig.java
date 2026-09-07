package ru.otus.smarthome.emulator;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(EmulatorProperties.class)
public class EmulatorConfig {

    @Bean
    public WebClient gatewayClient(EmulatorProperties properties) {
        return WebClient.builder().baseUrl(properties.gatewayUrl()).build();
    }
}
