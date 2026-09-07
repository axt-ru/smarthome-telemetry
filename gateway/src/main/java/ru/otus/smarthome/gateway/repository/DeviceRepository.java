package ru.otus.smarthome.gateway.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.otus.smarthome.gateway.domain.DeviceEntity;

public interface DeviceRepository extends ReactiveCrudRepository<DeviceEntity, Long> {

    @Query("select * from device order by room, metric")
    Flux<DeviceEntity> findAllOrdered();
}
