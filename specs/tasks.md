# Tarefas

| ID | Tarefa | Origem | Depende de | Concluída quando |
|---|---|---|---|---|
| T-01 | Inicializar projeto Spring Boot 3 (Java 21) com dependências base e estrutura de pacotes | Constituição, D-01 | - | `./mvnw clean compile` executa com sucesso |
| T-02 | Configurar Flyway e criar script `V1__create_links_table.sql` com índice único case-sensitive | D-01, RF-08, RN-01 | T-01 | Teste com Testcontainers aplica a migração V1 no PostgreSQL e valida unicidade |
| T-03 | Criar gerador de código alfanumérico com `SecureRandom` (`[a-zA-Z0-9]`, case-sensitive) e filtro de termos reservados | RF-02, RN-07, D-03 | T-01 | Testes unitários validam tamanho de 7 caracteres, case-sensitivity e ausência de palavras reservadas |
| T-04 | Implementar validador de URL (HTTP/HTTPS, limite 2048 caracteres, bloqueio de auto-referência) | RN-02, RN-06, Casos de borda | T-01 | Testes unitários cobrem URLs válidas, inválidas, > 2048 chars e auto-referência |
| T-05 | Implementar validador de código personalizado (regex e palavras reservadas) | RF-03, RN-05, RN-07 | T-01 | Testes unitários aceitam `^[a-zA-Z0-9_-]{4,20}$` e rejeitam caracteres especiais ou palavras reservadas |
| T-06 | Configurar documentação da API com SpringDoc OpenAPI 3 e Swagger UI | Constituição | T-01 | Teste de integração valida acesso aos endpoints `/swagger-ui.html` e `/v3/api-docs` |
| T-07 | Mapear entidade JPA `LinkEntity` e interface `LinkRepository` com busca case-sensitive | D-01, RF-08, RN-01 | T-02 | Teste de integração persiste e busca uma entidade no banco via Spring Data JPA |
| T-08 | Implementar `LinkService` e DTOs para criação de link encurtado (automático e customizado) | RF-01, RF-02, RF-03, RF-06, RN-01 | T-03, T-04, T-05, T-07 | Testes unitários cobrem criação com código gerado, código próprio, data futura e erro em data passada |
| T-09 | Configurar `GlobalExceptionHandler` com RFC 7807 (`ProblemDetail`) | Constituição, Qualidade, D-05 | T-01 | Teste de controller valida retorno de `ProblemDetail` com status HTTP adequado (400, 404, 409, 410) |
| T-10 | Implementar endpoint `POST /api/links` com validação de payload | RF-01, D-04 | T-08, T-09 | Teste de integração via `MockMvc` cria link (201), rejeita payload inválido (400) e código duplicado (409) |
| T-11 | Implementar consulta atômica com `@Modifying` no repositório para contagem de acessos | RF-05, RN-04, D-02 | T-07 | Teste com chamadas concorrentes contra Postgres real confirma que contagem final é exata |
| T-12 | Implementar endpoint de redirecionamento `GET /{code}` estritamente via HTTP 302 | RF-04, RN-03, D-06 | T-10, T-11 | Teste de integração valida status 302 com `Location`, 404 para inexistente e 410 para expirado |
| T-13 | Implementar endpoint de estatísticas públicas `GET /api/links/{code}/stats` sem autenticação | RF-07 | T-10 | Teste de integração valida retorno 200 público com métricas e 404 para inexistente |
