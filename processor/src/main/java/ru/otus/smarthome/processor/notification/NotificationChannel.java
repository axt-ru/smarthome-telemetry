package ru.otus.smarthome.processor.notification;

public interface NotificationChannel {

    String name();

    void send(String message);
}
