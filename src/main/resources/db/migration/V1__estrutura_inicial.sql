create table companies (
    id uuid not null,
    active boolean not null,
    address_complement varchar(255),
    address_number varchar(255) not null,
    certificate_password varchar(2048),
    certificate_path varchar(255),
    city_code varchar(7) not null,
    city_name varchar(255) not null,
    created_at timestamp with time zone not null,
    csc_id varchar(255),
    csc_token varchar(2048),
    district varchar(255) not null,
    fiscal_environment varchar(32) not null,
    legal_name varchar(255) not null,
    next_nfce_number bigint not null,
    next_nfe_number bigint not null,
    nfce_series_number integer not null,
    nfe_series_number integer not null,
    phone varchar(255),
    state_code varchar(2) not null,
    state_registration varchar(255) not null,
    street varchar(255) not null,
    tax_id varchar(14) not null,
    tax_regime varchar(64) not null,
    trade_name varchar(255),
    updated_at timestamp with time zone not null,
    zip_code varchar(8) not null,
    primary key (id)
);

alter table companies
    add constraint uk_companies_tax_id unique (tax_id);

create table fiscal_documents (
    id uuid not null,
    access_key varchar(255),
    authorization_number varchar(255),
    authorized_at timestamp with time zone,
    company_id uuid not null,
    created_at timestamp with time zone not null,
    customer_name varchar(255) not null,
    external_reference varchar(255) not null,
    fiscal_xml text,
    invoice_number bigint not null,
    last_error varchar(1000),
    model varchar(16) not null,
    next_retry_at timestamp with time zone,
    payload_json text not null,
    receipt_content varchar(4096),
    retry_count integer not null,
    series_number integer not null,
    status varchar(32) not null,
    total_amount numeric(15, 2) not null,
    updated_at timestamp with time zone not null,
    primary key (id)
);

alter table fiscal_documents
    add constraint uk_fiscal_documents_company_external_reference unique (company_id, external_reference);

alter table fiscal_documents
    add constraint fk_fiscal_documents_company foreign key (company_id) references companies (id);

create index idx_fiscal_documents_retry
    on fiscal_documents (status, next_retry_at, created_at);

create table fiscal_audit_events (
    id uuid not null,
    company_id uuid,
    document_id uuid,
    event_type varchar(64) not null,
    message varchar(1000) not null,
    details text,
    created_at timestamp with time zone not null,
    primary key (id)
);

create index idx_fiscal_audit_events_company_created
    on fiscal_audit_events (company_id, created_at);

create index idx_fiscal_audit_events_document_created
    on fiscal_audit_events (document_id, created_at);
