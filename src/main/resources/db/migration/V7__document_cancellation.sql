-- Cancelamento de documento fiscal pelo evento 110111.
--
-- A nota cancelada continua existindo no historico: ela foi autorizada e o
-- cancelamento e outro evento sobre ela, nao um apagamento. Guardar protocolo,
-- justificativa e data e o que permite explicar depois por que ela caiu.
alter table fiscal_documents add column cancellation_protocol varchar(255);

alter table fiscal_documents add column cancellation_reason varchar(255);

alter table fiscal_documents add column cancelled_at timestamp with time zone;
