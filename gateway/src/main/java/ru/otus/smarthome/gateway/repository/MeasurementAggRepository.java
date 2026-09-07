package ru.otus.smarthome.gateway.repository;

import java.time.Instant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.otus.smarthome.gateway.domain.MeasurementAggEntity;

public interface MeasurementAggRepository extends ReactiveCrudRepository<MeasurementAggEntity, Long> {

    @Query(
            """
            select a.* from measurement_agg a
            join device d on d.id = a.device_id
            where d.external_id = :externalId
              and a.window_type = :windowType
              and a.bucket_start >= :from
            order by a.bucket_start
            """)
    Flux<MeasurementAggEntity> findHistory(
            @Param("externalId") String externalId, @Param("windowType") String windowType, @Param("from") Instant from);
}
