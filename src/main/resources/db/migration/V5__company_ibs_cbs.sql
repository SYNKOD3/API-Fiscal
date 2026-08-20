-- CST e classificacao tributaria do IBS/CBS, da reforma tributaria.
--
-- Nascem nulos de proposito, inclusive para as empresas que ja existem: sao
-- decisao do contador de cada uma, e nao ha padrao que sirva para todas. O
-- schema da NF-e aceita qualquer numero no formato, entao um valor chutado
-- aqui geraria documento que a SEFAZ autoriza e que entra errado na
-- escrituracao - pior do que a rejeicao 1115, que ao menos e visivel.
--
-- Uma instrucao por coluna: o Postgres aceita as duas juntas, o H2 dos testes
-- nao, e nao ha nada a ganhar com a forma que so funciona em um dos dois.
alter table companies add column ibs_cbs_cst varchar(3);

alter table companies add column ibs_cbs_class_trib varchar(6);
