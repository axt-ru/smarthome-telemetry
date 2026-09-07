package ru.otus.smarthome.gateway.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.otus.smarthome.gateway.domain.AlertRuleEntity;

public interface AlertRuleRepository extends ReactiveCrudRepository<AlertRuleEntity, Long> {

    @Query("select * from alert_rule order by id")
    Flux<AlertRuleEntity> findAllOrdered();
}
