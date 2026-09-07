package ru.otus.smarthome.emulator;

import java.util.concurrent.ThreadLocalRandom;
import ru.otus.smarthome.common.Metric;

public class SensorState {

    private record Range(double min, double max, double maxStep) {}

    private final String deviceId;
    private final String room;
    private final Metric metric;
    private final Range range;

    private double value;

    public SensorState(String room, Metric metric) {
        this.room = room;
        this.metric = metric;
        this.deviceId = "%s-%s".formatted(room, metric.name());
        this.range = switch (metric) {
            case TEMPERATURE -> new Range(16, 28, 0.3);
            case HUMIDITY -> new Range(25, 70, 1.5);
            case CO2 -> new Range(400, 1400, 40);
            case POWER -> new Range(0, 3000, 250);
        };
        this.value = range.min() + (range.max() - range.min()) / 2;
    }

    public double nextValue() {
        var step = ThreadLocalRandom.current().nextDouble(-range.maxStep(), range.maxStep());
        value = Math.clamp(value + step, range.min(), range.max());
        return Math.round(value * 100.0) / 100.0;
    }

    public String deviceId() {
        return deviceId;
    }

    public String room() {
        return room;
    }

    public Metric metric() {
        return metric;
    }
}
