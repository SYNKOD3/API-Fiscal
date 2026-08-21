-- Conteudo do QR Code da NFC-e, como foi para a SEFAZ.
--
-- Quem imprime o cupom e a plataforma integradora, e ela nao tem como
-- recalcular o codigo: ele depende da URL da UF e do ambiente, que sao
-- conhecimento desta API. Sem guardar e devolver, a nota sai autorizada com o
-- QR Code no XML e o cupom do cliente sai sem — justamente o que o consumidor
-- usa para conferir a nota.
alter table fiscal_documents add column qr_code varchar(600);
