alter table companies
    add column bivaro_tenant_id varchar(80);

alter table companies
    add column bivaro_merchant_id varchar(80);

alter table companies
    add column callback_url varchar(255);

create index idx_companies_bivaro_tenant
    on companies (bivaro_tenant_id);

create unique index uk_companies_bivaro_tenant_merchant
    on companies (bivaro_tenant_id, bivaro_merchant_id);
