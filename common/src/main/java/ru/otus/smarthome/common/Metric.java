package ru.otus.smarthome.common;

public enum Metric {
    TEMPERATURE("°C"),
    HUMIDITY("%"),
    CO2("ppm"),
    POWER("W");

    private final String unit;

    Metric(String unit) {
        this.unit = unit;
    }

    public String unit() {
        return unit;
    }
}
