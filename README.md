# Fiscal API

API Spring Boot para emissão fiscal multiempresa, com suporte a NF-e, NFC-e, contingência local, retry automático, auditoria, logs operacionais, certificados A1 por empresa e autenticação para integração entre plataformas externas e a SEFAZ.

## Funcionalidades

- Cadastro de empresas emissoras com CNPJ, IE, UF, endereço, regime tributário, séries e numeração fiscal.
- Emissão de NF-e modelo 55 e NFC-e modelo 65.
- Contingência automática quando o serviço fiscal estiver indisponível.
- Reprocessamento em background de documentos pendentes.
- Upload de certificado A1 `.pfx` ou `.p12` por empresa emissora.
- Senhas sensíveis criptografadas no banco de dados.
- Roteamento SEFAZ pela UF da empresa emitente.
- Auditoria de eventos fiscais por empresa e documento.
- Logs operacionais com `X-Request-Id`, status HTTP e tempo de resposta.
- Autenticação por login e senha via HTTP Basic.
- Suporte opcional a `X-API-Key` e JWT Bearer para ativação futura.
- Swagger opcional para desenvolvimento.
- Console dev opcional para testes locais.

## Arquitetura Geral

```text
Sistema integrador
  -> Fiscal API
    -> Empresa emissora cadastrada
      -> Certificado A1 da empresa
      -> Rota SEFAZ da UF da empresa
        -> Autorização fiscal
```

A empresa emissora é sempre a empresa cadastrada na API. A UF do comprador não define a rota de emissão. A rota fiscal é definida por `stateCode`, ou seja, pela UF da empresa emitente.

## Pré-Requisitos

- Java 21.
- Maven 3.9+.
- Docker e Docker Compose para execução com PostgreSQL.
- PostgreSQL em produção.
- Certificado A1 válido para emissão real.
- Credenciamento fiscal da empresa emitente na SEFAZ da respectiva UF.
- CSC/token configurado quando houver emissão de NFC-e.

## Instalação Local

Clone o repositório:

```powershell
git clone https://github.com/SYNKOD3/API-Fiscal.git
cd API-Fiscal
```

Execute os testes:

```powershell
mvn test
```

Suba a aplicação em modo local:

```powershell
mvn spring-boot:run
```

Por padrão, a API local usa:

```text
URL: http://localhost:8081
Banco: H2 em memória
Provider fiscal: STUB
API key: desativada por padrão
Login: dev-client
Senha: dev-password-change-me
API key obrigatória: não
JWT obrigatório: não
Swagger: habilitado
Console dev: habilitado
```

Swagger:

```text
http://localhost:8081/swagger-ui/index.html
```

Console dev:

```text
http://localhost:8081/dev/index.html
```

## Teste Pelo Swagger

Para testar tudo pelo Swagger, abra:

```text
http://localhost:8081/swagger-ui/index.html
```

Clique em `Authorize` e preencha:

```text
Login e senha
username: dev-client
password: dev-password-change-me
```

Depois execute `Empresas > POST /api/v1/companies` e, em seguida, `Documentos fiscais > POST /api/v1/documents`.

Para teste sem certificado e sem SEFAZ real, mantenha `FISCAL_PROVIDER=STUB`.

## Configuração de Produção

Crie o arquivo `.env` a partir do modelo:

```powershell
Copy-Item env.production.example .env
```

Configure os valores reais no `.env`:

```env
POSTGRES_PASSWORD=senha-forte-do-postgres
APP_SECRETS_KEY=chave-fixa-com-pelo-menos-32-caracteres

AUTH_USERNAME=usuario-da-integracao
AUTH_PASSWORD=senha-forte-da-integracao-com-mais-de-24-caracteres

API_KEY_AUTH_ENABLED=false
APP_API_KEY=chave-forte-da-api

JWT_AUTH_ENABLED=false
JWT_SECRET=chave-jwt-com-pelo-menos-32-caracteres
JWT_ISSUER=fiscal-platform
JWT_AUDIENCE=fiscal-api
AUTH_DEFAULT_SCOPES=fiscal:companies:write,fiscal:companies:read,fiscal:certificates:write,fiscal:certificates:read,fiscal:documents:issue,fiscal:documents:retry,fiscal:documents:read,fiscal:audit:read,fiscal:logs:read,fiscal:sefaz:read
AUTH_TOKEN_TTL_MINUTES=60
FISCAL_PROVIDER=LIBRARY
CERTIFICATE_STORAGE_PATH=/var/lib/fiscal-api/certificates
APP_DEV_CONSOLE_ENABLED=false
OPENAPI_ENABLED=false
OPENAPI_PUBLIC_ACCESS=false
```

Suba com Docker:

```powershell
docker compose --env-file .env up -d --build
```

Verifique o status:

```powershell
docker compose ps
```

Healthcheck:

```powershell
Invoke-WebRequest http://localhost:8081/actuator/health
```

## Variáveis de Ambiente

| Variável | Obrigatória em produção | Descrição |
| --- | --- | --- |
| `SERVER_PORT` | Não | Porta HTTP da API. Padrão: `8081`. |
| `DATABASE_URL` | Sim | URL JDBC do PostgreSQL. |
| `DATABASE_USERNAME` | Sim | Usuário do PostgreSQL. |
| `DATABASE_PASSWORD` | Sim | Senha do PostgreSQL. |
| `APP_SECRETS_KEY` | Sim | Chave fixa usada para criptografar segredos. Não alterar após cadastrar certificados. |
| `AUTH_USERNAME` | Sim | Usuário da integração usado no HTTP Basic. |
| `AUTH_PASSWORD` | Sim | Senha forte da integração usada no HTTP Basic. |
| `API_KEY_AUTH_ENABLED` | Não | Ativa exigência adicional de `X-API-Key`. Padrão: `false`. |
| `APP_API_KEY` | Não | Chave exigida somente quando `API_KEY_AUTH_ENABLED=true`. |
| `JWT_AUTH_ENABLED` | Não | Ativa aceitação de JWT Bearer como alternativa ao Basic Auth. Padrão: `false`. |
| `JWT_SECRET` | Não | Chave usada para validar/emitir JWT HS256 quando `JWT_AUTH_ENABLED=true`. |
| `JWT_ISSUER` | Não | Emissor esperado no claim `iss` quando JWT estiver ativo. |
| `JWT_AUDIENCE` | Não | Audiência esperada no claim `aud` quando JWT estiver ativo. |
| `AUTH_DEFAULT_SCOPES` | Não | Escopos máximos permitidos para tokens emitidos pela Fiscal API quando JWT estiver ativo. |
| `AUTH_TOKEN_TTL_MINUTES` | Não | Tempo máximo de validade dos tokens emitidos pela Fiscal API quando JWT estiver ativo. |
| `FISCAL_PROVIDER` | Sim | `STUB` para simulação ou `LIBRARY` para integração fiscal real. |
| `FISCAL_UNAVAILABLE_STATES` | Não | Lista de UFs simuladas como indisponíveis, exemplo: `BA,SP`. |
| `CERTIFICATE_STORAGE_PATH` | Sim | Diretório privado e persistente para certificados A1. |
| `CERTIFICATE_MAX_SIZE_BYTES` | Não | Tamanho máximo do certificado enviado. |
| `APP_DEV_CONSOLE_ENABLED` | Sim | Deve ser `false` em produção. |
| `OPENAPI_ENABLED` | Sim | Deve ser `false` em produção, salvo necessidade controlada. |
| `OPENAPI_PUBLIC_ACCESS` | Sim | Deve ser `false` em produção. |

## Autenticação

O acesso principal da API é HTTP Basic com usuário e senha da integração.

```http
Authorization: Basic base64(username:password)
```

No Swagger, clique em `Authorize`, selecione `Login e senha`, informe `AUTH_USERNAME` e `AUTH_PASSWORD` e execute os endpoints.

Exemplo de chamada protegida:

```http
Authorization: Basic base64(usuario-da-integracao:senha-da-integracao)
```

## Camadas Opcionais de Segurança

`X-API-Key` e JWT continuam implementados, mas não são obrigatórios por padrão.

Para exigir API key além do login/senha:

```env
API_KEY_AUTH_ENABLED=true
APP_API_KEY=chave-forte-da-api
```

Quando ativo, envie também:

```http
X-API-Key: chave-forte-da-api
```

Para aceitar JWT Bearer como alternativa ao Basic Auth:

```env
JWT_AUTH_ENABLED=true
JWT_SECRET=chave-jwt-com-pelo-menos-32-caracteres
JWT_ISSUER=fiscal-platform
JWT_AUDIENCE=fiscal-api
```

O token opcional pode ser gerado pela própria API:

```http
POST /api/v1/auth/token
Content-Type: application/json
```

```json
{
  "username": "usuario-da-integracao",
  "password": "senha-forte-da-integracao-com-mais-de-24-caracteres",
  "subject": "integrador-backend",
  "tenantId": "tenant-001",
  "merchantId": "merchant-001",
  "scopes": [
    "fiscal:documents:issue",
    "fiscal:documents:read"
  ],
  "expiresInMinutes": 60
}
```

Exemplo de payload JWT:

```json
{
  "iss": "fiscal-platform",
  "aud": "fiscal-api",
  "sub": "integrator-backend",
  "jti": "uuid-unico-do-token",
  "tenantId": "tenant-001",
  "merchantId": "merchant-001",
  "scopes": [
    "fiscal:documents:issue",
    "fiscal:documents:read",
    "fiscal:certificates:write"
  ],
  "exp": 1797532800
}
```

Regras do token:

- O algoritmo aceito é `HS256`.
- `iss` deve bater com `JWT_ISSUER`.
- `aud` deve bater com `JWT_AUDIENCE`.
- `exp` é obrigatório e deve estar no futuro.
- Tokens com `tenantId` ou `merchantId` ficam restritos ao respectivo lojista.
- O escopo `fiscal:admin` libera todos os endpoints protegidos.
- O integrador só pode solicitar escopos presentes em `AUTH_DEFAULT_SCOPES`.

Escopos disponíveis:

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

Em produção, mantenha o console dev desabilitado:

```env
APP_DEV_CONSOLE_ENABLED=false
```

## Fluxo de Uso

1. Cadastrar a empresa emissora.
2. Enviar o certificado A1 da empresa.
3. Validar a rota fiscal da empresa.
4. Emitir NF-e ou NFC-e.
5. Consultar XML, comprovante ou impressão.
6. Acompanhar logs e auditoria.
7. Reprocessar documentos pendentes quando necessário.

## Cadastro de Empresa

Endpoint:

```http
POST /api/v1/companies
```

Exemplo:

```http
POST /api/v1/companies
Authorization: Basic base64(usuario-da-integracao:senha-da-integracao)
Content-Type: application/json
```

```json
{
  "tenantId": "tenant-prod",
  "merchantId": "lojista-1001",
  "callbackUrl": "https://integrador.example.com/webhooks/fiscal",
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
  "certificatePath": null,
  "certificatePassword": null,
  "cscId": "000001",
  "cscToken": "token-csc",
  "nfeSeriesNumber": 1,
  "nextNfeNumber": 1,
  "nfceSeriesNumber": 1,
  "nextNfceNumber": 1
}
```

Campos importantes:

- `tenantId`: identificador do tenant na plataforma integradora.
- `merchantId`: identificador do lojista na plataforma integradora.
- `taxId`: CNPJ da empresa emissora, somente números.
- `stateRegistration`: inscrição estadual da empresa emissora.
- `stateCode`: UF da empresa emissora.
- `fiscalEnvironment`: `HOMOLOGATION` ou `PRODUCTION`.
- `nfeSeriesNumber` e `nextNfeNumber`: série e próxima numeração para NF-e.
- `nfceSeriesNumber` e `nextNfceNumber`: série e próxima numeração para NFC-e.

## Upload de Certificado A1

O certificado A1 é vinculado à empresa emissora. Cada empresa pode ter um certificado ativo.

Endpoint:

```http
POST /api/v1/companies/{companyId}/certificates
```

Exemplo:

```http
POST /api/v1/companies/{companyId}/certificates
Authorization: Basic base64(usuario-da-integracao:senha-da-integracao)
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

Comportamento:

- A API valida se o arquivo é um PKCS#12 válido.
- A API valida a senha do certificado.
- A API tenta extrair o CNPJ do certificado.
- Se o CNPJ extraído for diferente do CNPJ da empresa, o upload é recusado.
- Se não for possível extrair o CNPJ em formato textual, o upload pode ser aceito após validar senha e validade.
- Ao enviar um novo certificado, o certificado anterior é marcado como `REPLACED`.

## Emissão de Documento Fiscal

Endpoint:

```http
POST /api/v1/documents
```

Exemplo usando `companyId`:

```http
POST /api/v1/documents
Authorization: Basic base64(usuario-da-integracao:senha-da-integracao)
Content-Type: application/json
```

```json
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

Exemplo usando `tenantId` e `merchantId`:

```json
{
  "tenantId": "tenant-prod",
  "merchantId": "lojista-1001",
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

O campo `totalAmount` deve bater com a soma de `items[].totalAmount`.

## Consulta de Documentos

Listar documentos por empresa:

```http
GET /api/v1/documents?companyId={uuid}
Authorization: Basic base64(usuario-da-integracao:senha-da-integracao)
```

Consultar um documento:

```http
GET /api/v1/documents/{id}
```

Baixar XML:

```http
GET /api/v1/documents/{id}/xml
```

Baixar comprovante textual:

```http
GET /api/v1/documents/{id}/receipt
```

Visualizar impressão HTML:

```http
GET /api/v1/documents/{id}/print
```

Reprocessar documento:

```http
POST /api/v1/documents/{id}/retry
```

## Contingência e Retry

Quando o provedor fiscal fica indisponível, a API:

- Salva o documento fiscal.
- Gera XML local.
- Gera comprovante local.
- Marca o documento como `CONTINGENCY_PENDING`.
- Agenda nova tentativa de envio.

O retry em background usa:

```env
FISCAL_RETRY_DELAY_MS=30000
FISCAL_RETRY_BATCH_SIZE=50
```

Para simular indisponibilidade de UFs em ambiente de teste:

```env
FISCAL_UNAVAILABLE_STATES=BA,SP
```

## Roteamento SEFAZ

Listar UFs suportadas:

```http
GET /api/v1/sefaz/states
```

Consultar rota por empresa:

```http
GET /api/v1/sefaz/companies/{companyId}/route?model=NFCE
```

Consultar rota por identificadores externos:

```http
GET /api/v1/sefaz/merchant-route?tenantId={tenant}&merchantId={merchant}&model=NFCE
```

A API utiliza a Java-NFe para configuração fiscal por UF quando `FISCAL_PROVIDER=LIBRARY`.

## Auditoria

Eventos fiscais são salvos em `fiscal_audit_events`.

Consultar eventos por empresa:

```http
GET /api/v1/audit/companies/{companyId}
```

Consultar eventos por documento:

```http
GET /api/v1/audit/documents/{documentId}
```

Eventos comuns:

- `COMPANY_CREATED`
- `DOCUMENT_RECEIVED`
- `ACCESS_KEY_CREATED`
- `DOCUMENT_AUTHORIZED`
- `DOCUMENT_CONTINGENCY`
- `DOCUMENT_REJECTED`

## Logs Operacionais

Toda requisição em `/api/**` recebe um `X-Request-Id`. Se o header já for enviado pelo cliente HTTP, a API preserva o valor. Caso contrário, gera um UUID automaticamente.

Consultar logs:

```http
GET /api/v1/operational-logs?level=WARN&limit=100
Authorization: Basic base64(usuario-da-integracao:senha-da-integracao)
```

Consultar log por request id:

```http
GET /api/v1/operational-logs/requests/{requestId}
Authorization: Basic base64(usuario-da-integracao:senha-da-integracao)
```

Os logs registram:

- `requestId`.
- Método HTTP.
- Path.
- Status HTTP.
- Duração.
- Empresa e documento relacionados, quando houver.
- Código e mensagem de erro tratada.

Dados sensíveis não são gravados nos logs operacionais.

## Teste de Tempo de Resposta

O benchmark de performance é opcional e fica desativado por padrão.

Executar benchmark:

```powershell
mvn -DperformanceTests=true -Dtest=PerformanceSmokeTests test
```

Controlar volume de chamadas:

```powershell
mvn -DperformanceTests=true -DperformanceReadRuns=100 -DperformanceWriteRuns=30 -Dtest=PerformanceSmokeTests test
```

O resultado mostra `Min`, `Avg`, `P50`, `P95`, `P99` e `Max` em milissegundos.

Esse benchmark usa MockMvc, H2 em memória e `FISCAL_PROVIDER=STUB`. Ele mede o overhead interno da API, mas não mede latência real da SEFAZ.

## Build

Gerar o `.jar`:

```powershell
mvn package -DskipTests
```

Executar o `.jar`:

```powershell
java -jar target/fiscal-api-0.0.1-SNAPSHOT.jar
```

## Endpoints Principais

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `POST` | `/api/v1/companies` | Cadastra empresa emissora. |
| `GET` | `/api/v1/companies` | Lista empresas. |
| `POST` | `/api/v1/companies/{companyId}/certificates` | Envia certificado A1. |
| `GET` | `/api/v1/companies/{companyId}/certificates` | Lista certificados da empresa. |
| `POST` | `/api/v1/documents` | Emite NF-e ou NFC-e. |
| `GET` | `/api/v1/documents/{id}` | Consulta documento. |
| `GET` | `/api/v1/documents?companyId={uuid}` | Lista documentos da empresa. |
| `POST` | `/api/v1/documents/{id}/retry` | Reprocessa documento. |
| `GET` | `/api/v1/documents/{id}/receipt` | Retorna comprovante textual. |
| `GET` | `/api/v1/documents/{id}/xml` | Retorna XML fiscal. |
| `GET` | `/api/v1/documents/{id}/print` | Retorna visualização HTML imprimível. |
| `GET` | `/api/v1/sefaz/states` | Lista UFs suportadas. |
| `GET` | `/api/v1/sefaz/companies/{companyId}/route` | Consulta rota SEFAZ da empresa. |
| `GET` | `/api/v1/sefaz/merchant-route` | Consulta rota por `tenantId` e `merchantId`. |
| `GET` | `/api/v1/audit/companies/{companyId}` | Lista auditoria da empresa. |
| `GET` | `/api/v1/audit/documents/{documentId}` | Lista auditoria do documento. |
| `GET` | `/api/v1/operational-logs` | Lista logs operacionais. |
| `GET` | `/api/v1/operational-logs/requests/{requestId}` | Consulta log por request id. |

## Estado da Integração Fiscal

O projeto possui dois modos de provedor fiscal:

- `STUB`: modo simulado para desenvolvimento e testes locais.
- `LIBRARY`: modo de integração com a biblioteca fiscal Java-NFe.

Para emissão real, cada empresa emissora precisa estar apta na SEFAZ:

- CNPJ ativo.
- Inscrição estadual ativa quando exigida.
- Certificado A1 válido.
- CSC/token configurado para NFC-e, quando aplicável.
- Credenciamento no ambiente de homologação ou produção da UF.
- Dados fiscais corretos no payload de emissão.

## Segurança

Recomendações para produção:

- Usar HTTPS obrigatório na borda.
- Manter Swagger e console dev desabilitados.
- Não versionar `.env`, certificados, dumps ou logs sensíveis.
- Usar volume privado para certificados.
- Usar `APP_SECRETS_KEY` forte e estável.
- Usar senha de integração forte e rotacionada com procedimento controlado.
- Ativar `API_KEY_AUTH_ENABLED` ou `JWT_AUTH_ENABLED` somente quando houver necessidade operacional.
- Registrar e monitorar erros por `X-Request-Id`.

## Licença

Este projeto utiliza dependências de terceiros conforme suas respectivas licenças.
