# Fiscal API

Base Spring Boot para emissao de NF-e e NFC-e em ambiente multiempresa, com contingencia local quando a SEFAZ estiver indisponivel.

## O que esta base entrega

- Cadastro de multiplas empresas emissoras.
- API protegida por `X-API-Key` para integracao com o seu sistema.
- Suporte ao modelo `NFE` e ao modelo `NFCE` por requisicao.
- Emissao desacoplada por `FiscalGateway`, facilitando a troca pelo seu SDK/lib fiscal existente.
- Contingencia automatica: quando o gateway fiscal ficar indisponivel, o documento fica salvo e um comprovante local e gerado.
- Reprocessamento automatico em background para transmissao posterior.
- Swagger em `/swagger-ui/index.html` apenas quando habilitado.
- Perfil `prod` com PostgreSQL, Flyway, console dev fechado e validacao de segredos no boot.
- Vinculo com lojistas do Bivaro por `bivaroTenantId` e `bivaroMerchantId`.
- Roteamento SEFAZ por UF da empresa emitente, nao pela UF do comprador.
- Logs operacionais com `X-Request-Id`, status HTTP, tempo de resposta e motivo de erro para suporte.
- Upload e gestao de certificado A1 por empresa emissora, com senha criptografada e storage privado.
- Autenticacao dupla em producao: `X-API-Key` + JWT Bearer com escopos e isolamento por tenant/lojista.

## Fluxo operacional

1. Seu sistema chama `POST /api/v1/documents`.
2. A aplicacao localiza a empresa emissora pelo `companyId` ou por `bivaroTenantId + bivaroMerchantId`.
3. A rota SEFAZ e definida pela UF da empresa emissora.
4. Se a SEFAZ estiver online, o gateway autoriza o documento.
5. Se a SEFAZ cair, o documento entra em `CONTINGENCY_PENDING` com recibo local.
6. O XML local e o comprovante ficam disponiveis para consulta/download.
7. O agendador tenta reenviar automaticamente ate conseguir a autorizacao.

## Endpoints principais

- `POST /api/v1/companies`
- `GET /api/v1/companies`
- `GET /api/v1/companies?bivaroTenantId={tenant}`
- `POST /api/v1/companies/{companyId}/certificates`
- `GET /api/v1/companies/{companyId}/certificates`
- `POST /api/v1/documents`
- `GET /api/v1/documents/{id}`
- `GET /api/v1/documents?companyId={uuid}`
- `POST /api/v1/documents/{id}/retry`
- `GET /api/v1/documents/{id}/receipt`
- `GET /api/v1/documents/{id}/xml`
- `GET /api/v1/documents/{id}/print`
- `GET /api/v1/sefaz/states`
- `GET /api/v1/sefaz/companies/{companyId}/route?model=NFCE`
- `GET /api/v1/sefaz/bivaro-route?bivaroTenantId={tenant}&bivaroMerchantId={merchant}&model=NFCE`
- `GET /api/v1/audit/companies/{companyId}`
- `GET /api/v1/audit/documents/{documentId}`
- `GET /api/v1/operational-logs`
- `GET /api/v1/operational-logs?level=WARN&limit=100`
- `GET /api/v1/operational-logs?companyId={uuid}`
- `GET /api/v1/operational-logs?documentId={uuid}`
- `GET /api/v1/operational-logs/requests/{requestId}`

## Autenticacao e autorizacao

Em producao, a API exige duas credenciais em cada chamada protegida:

```http
X-API-Key: chave-forte-da-integracao
Authorization: Bearer <jwt-curto-emitido-pela-bivaro>
```

A `X-API-Key` identifica a integracao. O JWT autoriza a acao, o tenant, o lojista e os escopos permitidos.

Claims esperadas no JWT:

```json
{
  "iss": "bivaro",
  "aud": "fiscal-api",
  "sub": "bivaro-backend",
  "jti": "uuid-unico-do-token",
  "bivaroTenantId": "tenant-001",
  "bivaroMerchantId": "merchant-001",
  "scopes": [
    "fiscal:documents:issue",
    "fiscal:documents:read",
    "fiscal:certificates:write"
  ],
  "exp": 1797532800
}
```

Regras aplicadas:

- JWT e validado com `HS256` e `JWT_SECRET`.
- `iss` precisa bater com `JWT_ISSUER`.
- `aud` precisa bater com `JWT_AUDIENCE`.
- `exp` e obrigatorio e precisa estar no futuro.
- Se o token tiver `bivaroTenantId` ou `bivaroMerchantId`, a API impede acesso a outra empresa/lojista.
- O escopo `fiscal:admin` libera todos os endpoints protegidos.

Escopos principais:

```text
fiscal:companies:write       POST /api/v1/companies
fiscal:companies:read        GET /api/v1/companies
fiscal:certificates:write    POST /api/v1/companies/{companyId}/certificates
fiscal:certificates:read     GET /api/v1/companies/{companyId}/certificates
fiscal:documents:issue       POST /api/v1/documents
fiscal:documents:retry       POST /api/v1/documents/{id}/retry
fiscal:documents:read        GET /api/v1/documents...
fiscal:audit:read            GET /api/v1/audit...
fiscal:logs:read             GET /api/v1/operational-logs...
fiscal:sefaz:read            GET /api/v1/sefaz...
```

No perfil local/dev, `JWT_AUTH_ENABLED=false` por padrao para facilitar uso do console interno. No perfil `prod`, `JWT_AUTH_ENABLED=true` por padrao e a aplicacao nao sobe sem `JWT_SECRET` forte.

## Bivaro como SaaS fiscal

O Bivaro e a plataforma intermediadora. A empresa emitente da NF-e/NFC-e e sempre o lojista cadastrado, com seu proprio CNPJ, UF, inscricao estadual, certificado, CSC, serie e numeracao.

Exemplo:

```text
Bivaro -> API Fiscal -> Lojista BA -> SEFAZ/autorizador da BA
Bivaro -> API Fiscal -> Lojista SP -> SEFAZ/autorizador de SP
Bivaro -> API Fiscal -> Lojista MG -> SEFAZ/autorizador de MG
```

A UF do comprador nao define a SEFAZ de emissao. A rota fiscal vem de `Company.stateCode`, ou seja, da UF do lojista emitente.

## Exemplo de cadastro de empresa

```http
POST /api/v1/companies
X-API-Key: sua-chave-forte
Authorization: Bearer jwt-curto-da-bivaro
Content-Type: application/json

{
  "bivaroTenantId": "bivaro-prod",
  "bivaroMerchantId": "lojista-1001",
  "callbackUrl": "https://bivaro.com.br/webhooks/fiscal",
  "legalName": "Empresa Exemplo LTDA",
  "taxId": "12345678000199",
  "stateRegistration": "123456789",
  "stateCode": "BA",
  "tradeName": "Empresa Exemplo",
  "street": "Rua Fiscal",
  "addressNumber": "100",
  "addressComplement": "Sala 01",
  "district": "Centro",
  "cityCode": "2927408",
  "cityName": "Salvador",
  "zipCode": "40000000",
  "phone": "7133334444",
  "taxRegime": "SIMPLES_NACIONAL",
  "fiscalEnvironment": "HOMOLOGATION",
  "certificatePath": "C:/certificados/empresa.pfx",
  "certificatePassword": "senha-do-certificado",
  "cscId": "000001",
  "cscToken": "token-csc",
  "nfeSeriesNumber": 1,
  "nextNfeNumber": 1,
  "nfceSeriesNumber": 1,
  "nextNfceNumber": 1
}
```

## Upload de certificado A1

Para o fluxo SaaS da Bivaro, o certificado nao deve ficar solto nem ser escolhido manualmente na emissao. Ele e sempre vinculado a uma empresa emissora.

Fluxo recomendado:

```text
Cliente sobe o A1 na Bivaro
Bivaro envia .pfx/.p12 + senha para a API Fiscal
API valida o arquivo e a senha
API salva o arquivo em storage privado
API salva a senha criptografada no banco
API marca esse certificado como ACTIVE para a empresa
```

Endpoint:

```http
POST /api/v1/companies/{companyId}/certificates
X-API-Key: sua-chave-forte
Authorization: Bearer jwt-curto-da-bivaro
Content-Type: multipart/form-data

file=@empresa.pfx
password=senha-do-certificado
```

Resposta:

```json
{
  "data": {
    "id": "UUID_DO_CERTIFICADO",
    "companyId": "UUID_DA_EMPRESA",
    "originalFileName": "empresa.pfx",
    "certificateTaxId": "12345678000199",
    "serialNumber": "ABC123",
    "validUntil": "2027-08-17T12:00:00Z",
    "status": "ACTIVE"
  }
}
```

Ao enviar um novo certificado para a mesma empresa, a API marca o anterior como `REPLACED` e ativa o novo. A emissao sempre usa o certificado `ACTIVE` da empresa localizada por `companyId` ou por `bivaroTenantId + bivaroMerchantId`.

A API tenta extrair o CNPJ do certificado e bloqueia o upload quando o CNPJ extraido nao confere com o CNPJ da empresa. Alguns certificados podem nao expor o CNPJ em formato textual simples; nesses casos, a senha/validade ainda sao validadas e o upload e aceito sem `certificateTaxId`.

## Exemplo de emissao

```http
POST /api/v1/documents
X-API-Key: sua-chave-forte
Authorization: Bearer jwt-curto-da-bivaro
Content-Type: application/json

{
  "companyId": "UUID_DA_EMPRESA",
  "model": "NFCE",
  "externalReference": "PEDIDO-1001",
  "customerName": "Cliente Teste",
  "totalAmount": 199.90,
  "items": [
    {
      "sku": "PROD-001",
      "description": "Produto A",
      "ncm": "01012100",
      "cest": null,
      "gtin": "SEM GTIN",
      "cfop": "5102",
      "unit": "UN",
      "quantity": 1,
      "unitAmount": 199.90,
      "totalAmount": 199.90,
      "origin": "0",
      "icmsCode": "102",
      "pisCode": "49",
      "cofinsCode": "49",
      "approximateTaxAmount": 0
    }
  ]
}
```

O `totalAmount` do documento precisa bater com a soma de `items[].totalAmount`.

Tambem e possivel emitir sem expor o UUID interno da empresa, usando os identificadores do Bivaro:

```json
{
  "bivaroTenantId": "bivaro-prod",
  "bivaroMerchantId": "lojista-1001",
  "model": "NFCE",
  "externalReference": "PEDIDO-1001",
  "customerName": "Cliente Teste",
  "totalAmount": 199.90,
  "items": [
    {
      "sku": "PROD-001",
      "description": "Produto A",
      "ncm": "01012100",
      "cest": null,
      "gtin": "SEM GTIN",
      "cfop": "5102",
      "unit": "UN",
      "quantity": 1,
      "unitAmount": 199.90,
      "totalAmount": 199.90,
      "origin": "0",
      "icmsCode": "102",
      "pisCode": "49",
      "cofinsCode": "49",
      "approximateTaxAmount": 0
    }
  ]
}
```

## Como plugar a biblioteca fiscal real

Hoje a aplicacao sobe com `app.fiscal.provider=STUB` para validar o fluxo de negocio.
A dependencia da Java-NFe ja esta declarada no Maven:

```xml
<dependency>
  <groupId>br.com.swconsultoria</groupId>
  <artifactId>java-nfe</artifactId>
  <version>4.1.1</version>
</dependency>
```

Para integrar a biblioteca existente:

1. Ajuste `app.fiscal.provider=LIBRARY`.
2. Use `JavaNfeMapper` para converter `FiscalXmlDraft` para `TEnviNFe`/`TNFe` da Java-NFe.
3. Monte `ConfiguracoesNfe` com UF, ambiente, certificado e CSC da empresa.
4. Chame `Nfe.montaNfe(config, enviNFe, true)` para assinar/validar.
5. Chame `Nfe.enviarNfe(config, enviNFeAssinado, DocumentoEnum.NFE/NFCE)`.
6. Se o provedor gerar XML, DANFE, NFC-e ou PDF, persista esses artefatos no documento e publique-os para download.

## Rascunho XML

A aplicacao possui uma camada intermediaria antes da Java-NFe:

- `FiscalXmlBuilder`: normaliza o payload interno e separa NF-e modelo 55 de NFC-e modelo 65.
- `FiscalIssuerDraft`: concentra os dados fiscais do emitente, incluindo CNPJ, IE, endereco, municipio IBGE e regime tributario.
- `FiscalXmlDraft`: representa o documento fiscal pronto para virar JAXB.
- `JavaNfeMapper`: converte o rascunho fiscal para `TEnviNFe`/`TNFe`, os objetos usados pela Java-NFe.
- `FiscalXmlPreviewRenderer`: gera uma pre-visualizacao XML para auditoria e testes.

Essa camada evita que detalhes da biblioteca fiscal vazem para controllers, servicos de contingencia ou API publica.

## Modelos fiscais

- `NFCE`: cupom fiscal de venda ao consumidor final, modelo 65.
- `NFE`: nota fiscal eletronica completa de mercadoria, modelo 55.

O campo `model` em `POST /api/v1/documents` define qual caminho fiscal sera usado.

## Roteamento SEFAZ nacional

A classe `SefazRouter` centraliza a decisao de rota por UF. A API usa a UF da empresa emitente para montar `ConfiguracoesNfe` na Java-NFe.

Endpoints uteis:

- `GET /api/v1/sefaz/states`: lista as UFs habilitadas na API.
- `GET /api/v1/sefaz/companies/{companyId}/route?model=NFCE`: mostra a rota de uma empresa.
- `GET /api/v1/sefaz/bivaro-route?bivaroTenantId=...&bivaroMerchantId=...&model=NFCE`: mostra a rota pelo identificador do Bivaro.

Para simular queda localizada de SEFAZ por UF:

```env
FISCAL_UNAVAILABLE_STATES=BA,SP
```

Nesse caso, apenas empresas emitentes dessas UFs entram em contingencia; lojistas de outras UFs continuam emitindo.

## Numeracao fiscal

A numeracao e separada por empresa e por modelo fiscal. Ao emitir um documento, a aplicacao bloqueia a empresa em transacao (`for update`), aloca a serie/numero correto e incrementa o proximo numero antes de salvar o documento. Se a transacao falhar, a numeracao volta junto com o rollback.

Tambem existe uma restricao unica para evitar duas emissoes com a mesma `externalReference` na mesma empresa.

## Persistencia de producao

O projeto sobe com H2 para testes locais. Em producao, use o perfil `prod`: ele exige PostgreSQL, API key forte, chave fixa de criptografia, provider fiscal real e bloqueia console dev/Swagger por padrao.

Crie um arquivo `.env` a partir do modelo:

```powershell
Copy-Item env.production.example .env
```

Edite o `.env` com valores reais. Os campos obrigatorios sao:

- `POSTGRES_PASSWORD`: senha forte do banco.
- `APP_API_KEY`: chave forte usada pelo seu sistema no header `X-API-Key`.
- `APP_SECRETS_KEY`: chave fixa com pelo menos 32 caracteres para criptografar senha do certificado e CSC.
- `JWT_SECRET`: chave forte com pelo menos 32 caracteres para validar os JWTs emitidos pela Bivaro.
- `JWT_ISSUER` e `JWT_AUDIENCE`: emissor e audiencia esperados no JWT.
- `FISCAL_PROVIDER=LIBRARY`: usa a camada da biblioteca fiscal real.
- `CERTIFICATE_STORAGE_PATH`: pasta/volume privado e persistente para armazenar os arquivos `.pfx/.p12`.

Para subir a aplicacao em modo producao com Docker:

```powershell
docker compose --env-file .env up -d --build
```

## Teste de tempo de resposta

Existe um benchmark leve, desligado por padrao, para medir os endpoints principais sem depender de servidor externo. Ele usa MockMvc, H2 em memoria e `FISCAL_PROVIDER=STUB`, entao serve para medir overhead da API, controllers, filtros, banco local e serializacao. Nao mede latencia real da SEFAZ.

Para rodar:

```powershell
mvn -DperformanceTests=true -Dtest=PerformanceSmokeTests test
```

Para controlar o volume de chamadas:

```powershell
mvn -DperformanceTests=true -DperformanceReadRuns=100 -DperformanceWriteRuns=30 -Dtest=PerformanceSmokeTests test
```

O resultado mostra `Min`, `Avg`, `P50`, `P95`, `P99` e `Max` em milissegundos para healthcheck, listagem de empresas, listagem de documentos e emissao simulada.

Para rodar sem Docker, configure:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/fiscal_api"
$env:DATABASE_USERNAME="fiscal_api"
$env:DATABASE_PASSWORD="senha-forte-do-banco"
$env:APP_API_KEY="chave-forte-com-mais-de-24-caracteres"
$env:APP_SECRETS_KEY="chave-fixa-com-mais-de-32-caracteres"
$env:JWT_AUTH_ENABLED="true"
$env:JWT_SECRET="chave-jwt-com-mais-de-32-caracteres"
$env:JWT_ISSUER="bivaro"
$env:JWT_AUDIENCE="fiscal-api"
$env:FISCAL_PROVIDER="LIBRARY"
$env:CERTIFICATE_STORAGE_PATH="C:/fiscal-api/certificates"
$env:APP_DEV_CONSOLE_ENABLED="false"
$env:OPENAPI_ENABLED="false"
$env:OPENAPI_PUBLIC_ACCESS="false"
java -jar target/fiscal-api-0.0.1-SNAPSHOT.jar
```

Travas aplicadas no perfil `prod`:

- A aplicacao nao sobe se `APP_API_KEY` estiver ausente, curta ou `change-me`.
- A aplicacao nao sobe se JWT estiver desligado ou `JWT_SECRET` estiver ausente/fraco.
- A aplicacao nao sobe se `APP_SECRETS_KEY` estiver ausente, curta ou com valor dev.
- A aplicacao nao sobe se `FISCAL_PROVIDER` nao for `LIBRARY`.
- A aplicacao nao sobe se `/dev` ou Swagger publico estiverem ligados.
- A aplicacao nao sobe se o datasource nao for PostgreSQL.
- A aplicacao nao sobe se `CERTIFICATE_STORAGE_PATH` nao apontar para um storage privado/persistente.

Nunca altere `APP_SECRETS_KEY` depois de cadastrar certificados/CSC sem processo de recriptografia dos segredos ja gravados.

## Auditoria e artefatos

A aplicacao registra eventos fiscais em `fiscal_audit_events`, incluindo cadastro de empresa, recebimento de documento, geracao de chave, autorizacao, contingencia e rejeicao.

Artefatos disponiveis:

- XML fiscal local/autorizado em `/api/v1/documents/{id}/xml`.
- Comprovante textual em `/api/v1/documents/{id}/receipt`.
- Visualizacao HTML imprimivel em `/api/v1/documents/{id}/print`.
- Eventos de auditoria por empresa/documento nos endpoints `/api/v1/audit/...`.

## Logs operacionais

Toda requisicao em `/api/**` recebe um header `X-Request-Id`. Se o cliente enviar esse header, a API preserva o valor; caso contrario, gera um UUID automaticamente.

Os logs operacionais ficam em `operational_logs` e registram:

- `requestId`, metodo, path, status HTTP e duracao.
- Nivel `INFO`, `WARN` ou `ERROR`.
- Empresa, documento e referencia externa quando a chamada estiver vinculada a uma emissao.
- Codigo/mensagem de erro tratada, sem gravar API key, CSC, senha de certificado ou payload fiscal bruto.

Consultas uteis:

```http
GET /api/v1/operational-logs?level=WARN&limit=100
X-API-Key: sua-chave-forte
```

```http
GET /api/v1/operational-logs/requests/{requestId}
X-API-Key: sua-chave-forte
```
