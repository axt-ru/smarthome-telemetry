package ru.otus.smarthome.processor.notification;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WebhookChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(WebhookChannel.class);

    private final RestClient restClient = RestClient.create();

    @Value("${processor.notification.webhook-url:}")
    private String webhookUrl;

    @Override
    public String name() {
        return "WEBHOOK";
    }

    @Override
    public void send(String message) {
        if (webhookUrl.isBlank()) {
            log.warn("вебхук не настроен, сообщение не отправлено: {}", message);
            return;
        }
        try {
            restClient.post().uri(webhookUrl).body(Map.of("text", message)).retrieve().toBodilessEntity();
            log.info("оповещение отправлено на вебхук: {}", message);
        } catch (Exception ex) {
            log.error("не удалось отправить оповещение на вебхук", ex);
        }
    }
}
