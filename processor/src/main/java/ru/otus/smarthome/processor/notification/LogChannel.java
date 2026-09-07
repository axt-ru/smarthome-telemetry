package ru.otus.smarthome.processor.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(LogChannel.class);

    @Override
    public String name() {
        return "LOG";
    }

    @Override
    public void send(String message) {
        log.warn("ОПОВЕЩЕНИЕ: {}", message);
    }
}
