package ru.otus.smarthome.gateway.repository;

import java.time.Instant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.otus.smarthome.gateway.domain.MeasurementEntity;

public interface MeasurementRepository extends ReactiveCrudRepository<MeasurementEntity, Long> {

    @Query(
            """
            select m.* from measurement m
            join device d on d.id = m.device_id
            where d.external_id = :externalId and m.measured_at >= :from
            order by m.measured_at
            """)
    Flux<MeasurementEntity> findHistory(@Param("externalId") String externalId, @Param("from") Instant from);
}
