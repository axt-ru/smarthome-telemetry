package ru.otus.smarthome.common;

public final class Queues {
    public static final String EXCHANGE = "smarthome.telemetry";
    public static final String ROUTING_KEY = "measurement";
    public static final String QUEUE = "smarthome.measurement";
    public static final String DEAD_LETTER_EXCHANGE = "smarthome.telemetry.dlx";
    public static final String DEAD_LETTER_QUEUE = "smarthome.measurement.dlq";

    private Queues() {}
}
