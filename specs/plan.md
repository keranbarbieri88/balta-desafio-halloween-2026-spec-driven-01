# Plano técnico

## Contexto
Implementa a especificação do encurtador de links, respeitando a constituição do projeto (Java 21, Spring Boot 3, PostgreSQL, Flyway e arquitetura em camadas).

## Arquitetura
- Camada Web/Controller: LinkController gerenciando endpoints REST via Spring MVC, com validações baseadas em Jakarta Validation (@Valid) e retorno em conformidade com RFC 7807 (ProblemDetail).
- Camada de Aplicação/Serviço: LinkService responsável por orquestrar a lógica de negócio, expiração, geração de código alfanumérico e tratamento de exceções de domínio.
- Camada de Persistência: LinkRepository estendendo JpaRepository com consultas otimizadas e operações atômicas via @Modifying.
- Contratos: Comunicação externa estritamente por DTOs imutáveis (records do Java), sem expor a entidade JPA.

## Decisões
- D-01(Stack de Banco): PostgreSQL com migrações versionadas via Flyway (a partir de V1__create_links_table.sql), garantindo atomicidade, índices e integridade em produção conforme estabelecido na Constituição.
- D-02(Concorrência no Contador): Incremento de acessos via query atômica com @Modifying no repositório (UPDATE links SET access_count = access_count + 1 WHERE id = :id), evitando lost updates e eliminando a necessidade de locks pessimistas pesados na leitura.
- D-03(Geração de Código Alfanumérico): Código aleatório de 7 caracteres gerado com SecureRandom usando o conjunto completo [a-zA-Z0-9] com diferenciação de maiúsculas e minúsculas (case-sensitive), garantindo que termos reservados do sistema (RN-07) nunca sejam gerados.
- D-04(Garantia de Unicidade Case-Sensitive): Constraint UNIQUE no banco de dados na coluna code com collation case-sensitive, com tratamento de DataIntegrityViolationException no serviço para responder com HTTP 409 (Conflict).
- D-05(Tratamento Global de Erros): GlobalExceptionHandler centralizado via @RestControllerAdvice em conformidade com a RFC 7807 (ProblemDetail), mapeando erros de validação e de regras de negócio para os status HTTP apropriados (400, 404, 409, 410).
- D-06(Redirecionamento): O endpoint GET /{code} responde estritamente com HTTP 302 Found para instruir clientes a não cachearem o redirecionamento definitivamente, garantindo a contabilidade exata de acessos e atendendo ao critério de aceite da especificação.

## Modelo de dados
### Tabela `links` (PostgreSQL via Flyway)
```sql
CREATE TABLE links (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    target_url VARCHAR(2048) NOT NULL,
    created_at_utc TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    expires_at_utc TIMESTAMP WITH TIME ZONE NULL,
    access_count BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_links_code ON links (code);
```

### Entidade JPA (LinkEntity.java)
- Long id
- String code
- String targetUrl
- Instant createdAtUtc
- Instant expiresAtUtc (anulável)
- Long accessCount

## Contratos    
### 1. Criar Link Encurtado
- **Rota:** `POST /api/links`
- **Request Body (JSON Record):**
  ```json
  {
    "targetUrl": "https://exemplo.com/artigo-muito-longo",
    "code": "artigo-top", 
    "expiresAtUtc": "2026-12-31T23:59:59Z"
  }
  ```
  *(Os campos `code` e `expiresAtUtc` são opcionais)*
- **Response Body (201 Created):**
  ```json
  {
    "code": "artigo-top",
    "shortUrl": "https://dominio.com/artigo-top",
    "targetUrl": "https://exemplo.com/artigo-muito-longo",
    "expiresAtUtc": "2026-12-31T23:59:59Z"
  }
  ```
- **Erros:**
  - `400 Bad Request` (`ProblemDetail`): URL inválida, protocolo não HTTP(S), código com caracteres não permitidos, código reservado ou data de expiração no passado.
  - `409 Conflict` (`ProblemDetail`): Código já em utilização.

---

### 2. Redirecionamento
- **Rota:** `GET /{code}`
- **Respostas:**
  - `302 Found`: Cabeçalho `Location: <targetUrl>` e execução do incremento atômico de acessos.
  - `404 Not Found` (`ProblemDetail`): Código inexistente.
  - `410 Gone` (`ProblemDetail`): Link expirado (sem incremento de acessos).

---

### 3. Consultar Estatísticas de Acesso (Pública)
- **Rota:** `GET /api/links/{code}/stats`
- **Consulta pública:** Sem necessidade de autenticação ou cabeçalhos proprietários.
- **Response Body (200 OK):**
  ```json
  {
    "code": "artigo-top",
    "targetUrl": "https://exemplo.com/artigo-muito-longo",
    "accessCount": 42,
    "createdAtUtc": "2026-10-27T10:00:00Z",
    "expiresAtUtc": "2026-12-31T23:59:59Z",
    "isExpired": false
  }
  ```
- **Erros:**
  - `404 Not Found` (`ProblemDetail`): Código inexistente.

---

## Riscos
- Concorrência na criação do mesmo código, mitigada com o índice único no PostgreSQL (`uq_links_code`) e a captura da exceção `DataIntegrityViolationException`.
- Perda de contagem sob acessos concorrentes, mitigada pela instrução SQL de atualização atômica (`access_count = access_count + 1`).
- Colisão de rotas na raiz (`GET /{code}`): mitigada ao isolar a API sob o prefixo `/api/` e bloquear termos reservados do sistema (tais como `api`, `actuator`, `swagger-ui`).
