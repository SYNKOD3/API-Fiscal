create table operational_logs (
    id uuid not null,
    request_id varchar(64) not null,
    level varchar(16) not null,
    event_type varchar(64) not null,
    http_method varchar(16),
    path varchar(512),
    status_code integer,
    duration_ms bigint,
    company_id uuid,
    document_id uuid,
    external_reference varchar(255),
    message varchar(1000) not null,
    details text,
    created_at timestamp with time zone not null,
    primary key (id)
);

create index idx_operational_logs_created
    on operational_logs (created_at);

create index idx_operational_logs_level_created
    on operational_logs (level, created_at);

create index idx_operational_logs_company_created
    on operational_logs (company_id, created_at);

create index idx_operational_logs_document_created
    on operational_logs (document_id, created_at);

create index idx_operational_logs_request_id
    on operational_logs (request_id);
