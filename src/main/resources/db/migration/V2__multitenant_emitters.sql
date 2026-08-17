alter table companies
    add column tenant_id varchar(80);

alter table companies
    add column merchant_id varchar(80);

alter table companies
    add column callback_url varchar(255);

create index idx_companies_tenant
    on companies (tenant_id);

create unique index uk_companies_tenant_merchant
    on companies (tenant_id, merchant_id);
