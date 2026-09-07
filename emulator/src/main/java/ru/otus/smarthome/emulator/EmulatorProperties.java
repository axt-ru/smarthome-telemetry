package ru.otus.smarthome.emulator;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emulator")
public record EmulatorProperties(String gatewayUrl, List<String> rooms, long intervalMillis) {}
