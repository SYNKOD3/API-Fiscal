create table company_certificates (
    id uuid not null,
    company_id uuid not null,
    storage_path varchar(1024) not null,
    original_file_name varchar(255) not null,
    certificate_password varchar(2048) not null,
    certificate_tax_id varchar(14),
    serial_number varchar(128),
    subject_dn varchar(1000),
    valid_from timestamp with time zone,
    valid_until timestamp with time zone,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    activated_at timestamp with time zone not null,
    replaced_at timestamp with time zone,
    primary key (id)
);

alter table company_certificates
    add constraint fk_company_certificates_company foreign key (company_id) references companies (id);

create index idx_company_certificates_company_created
    on company_certificates (company_id, created_at);

create index idx_company_certificates_company_status
    on company_certificates (company_id, status, activated_at);
