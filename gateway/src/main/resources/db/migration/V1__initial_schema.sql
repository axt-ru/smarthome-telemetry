create table device
(
    id           bigserial primary key,
    external_id  varchar(64)      not null unique,
    name         varchar(100)     not null,
    room         varchar(100)     not null,
    metric       varchar(32)      not null,
    unit         varchar(16)      not null,
    enabled      boolean          not null default true,
    created_at   timestamptz      not null default now(),
    last_seen_at timestamptz
);

create index idx_device_room on device (room);

create table measurement
(
    id          bigserial primary key,
    device_id   bigint           not null references device (id),
    measured_at timestamptz      not null,
    value       double precision not null
);

create index idx_measurement_device_time on measurement (device_id, measured_at desc);
create index idx_measurement_time on measurement (measured_at);

create table measurement_agg
(
    id           bigserial primary key,
    device_id    bigint           not null references device (id),
    window_type  varchar(16)      not null,
    bucket_start timestamptz      not null,
    min_value    double precision not null,
    max_value    double precision not null,
    avg_value    double precision not null,
    cnt          integer          not null,
    constraint uq_measurement_agg unique (device_id, window_type, bucket_start)
);

create index idx_measurement_agg_lookup on measurement_agg (device_id, window_type, bucket_start desc);

create table alert_rule
(
    id                bigserial primary key,
    name              varchar(100) not null,
    device_id         bigint references device (id),
    room              varchar(100),
    metric            varchar(32)  not null,
    condition_type    varchar(16)  not null,
    threshold_low     double precision,
    threshold_high    double precision,
    consecutive_count integer      not null default 1,
    cooldown_seconds  integer      not null default 300,
    channel           varchar(32)  not null default 'LOG',
    enabled           boolean      not null default true,
    created_at        timestamptz  not null default now()
);

create table alert_event
(
    id        bigserial primary key,
    rule_id   bigint           not null references alert_rule (id),
    device_id bigint           not null references device (id),
    fired_at  timestamptz      not null default now(),
    value     double precision not null,
    message   varchar(500)     not null,
    delivered boolean          not null default false
);

create index idx_alert_event_fired on alert_event (fired_at desc);
create index idx_alert_event_rule on alert_event (rule_id, fired_at desc);
