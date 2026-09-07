package ru.otus.smarthome.gateway.api;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import ru.otus.smarthome.common.MeasurementDto;
import ru.otus.smarthome.gateway.repository.DeviceRepository;
import ru.otus.smarthome.gateway.repository.MeasurementAggRepository;
import ru.otus.smarthome.gateway.repository.MeasurementRepository;
import ru.otus.smarthome.gateway.telemetry.LiveTelemetryStream;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private static final int RAW_LIMIT_MINUTES = 120;
    private static final int MINUTE_LIMIT_MINUTES = 2880;

    private final DeviceRepository deviceRepository;
    private final MeasurementRepository measurementRepository;
    private final MeasurementAggRepository aggRepository;
    private final LiveTelemetryStream liveStream;

    public DashboardController(
            DeviceRepository deviceRepository,
            MeasurementRepository measurementRepository,
            MeasurementAggRepository aggRepository,
            LiveTelemetryStream liveStream) {
        this.deviceRepository = deviceRepository;
        this.measurementRepository = measurementRepository;
        this.aggRepository = aggRepository;
        this.liveStream = liveStream;
    }

    @GetMapping("/devices")
    public Flux<DeviceView> devices() {
        return deviceRepository
                .findAllOrdered()
                .map(device -> new DeviceView(
                        device.externalId(), device.room(), device.metric(), device.unit(), device.lastSeenAt()));
    }

    @GetMapping("/measurements/{deviceId}")
    public Flux<PointView> history(
            @PathVariable("deviceId") String deviceId, @RequestParam(name = "minutes", defaultValue = "30") int minutes) {
        var from = Instant.now().minus(Duration.ofMinutes(minutes));

        if (minutes <= RAW_LIMIT_MINUTES) {
            log.debug("история {} за {} мин: сырые показания", deviceId, minutes);
            return measurementRepository
                    .findHistory(deviceId, from)
                    .map(measurement -> new PointView(measurement.measuredAt(), measurement.value()));
        }

        var windowType = minutes <= MINUTE_LIMIT_MINUTES ? "MINUTE" : "HOUR";
        log.debug("история {} за {} мин: агрегаты {}", deviceId, minutes, windowType);
        return aggRepository
                .findHistory(deviceId, windowType, from)
                .map(agg -> new PointView(agg.bucketStart(), agg.avgValue()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MeasurementDto> stream(@RequestParam(name = "deviceId", required = false) String deviceId) {
        return liveStream.stream().filter(m -> deviceId == null || deviceId.equals(m.deviceId()));
    }
}
