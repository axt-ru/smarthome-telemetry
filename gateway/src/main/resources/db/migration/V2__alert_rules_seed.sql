insert into alert_rule (name, room, metric, condition_type, threshold_low, threshold_high,
                        consecutive_count, cooldown_seconds, channel)
values ('Жара в детской', 'Детская', 'TEMPERATURE', 'GT', null, 24, 3, 120, 'LOG'),
       ('Духота', null, 'CO2', 'GT', null, 1100, 3, 180, 'LOG'),
       ('Влажность вне нормы', null, 'HUMIDITY', 'OUT_OF_RANGE', 30, 60, 5, 300, 'LOG'),
       ('Датчик молчит', null, 'TEMPERATURE', 'NO_DATA', null, null, 1, 600, 'LOG');
