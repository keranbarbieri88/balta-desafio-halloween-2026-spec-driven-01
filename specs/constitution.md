# Constituição do projeto

## Stack
- Java 21, Spring Boot 3, Maven, API RESTful
- Persistência em PostgreSQL 16 via Spring Data JPA e Hibernate (executado via `docker compose up -d`)
- Migrações de banco gerenciadas com Flyway
- Testes com JUnit 5, Mockito e Testcontainers (PostgreSQL)
- Documentação com SpringDoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`, Diagramas C4)

## Arquitetura
- Padrão arquitetural em camadas: Controller -> Service -> Repository
- Endpoint não acessa banco diretamente, sempre via serviço de aplicação
- Regra de negócio não vive no endpoint nem no handler HTTP
- Entidades JPA nunca são expostas na camada Web; uso estrito de DTOs para Request/Response
- Redirecionamento implementado via HTTP 302/307 para possibilitar métricas de acesso
- Nenhuma dependência nova entra sem justificativa escrita no plano

## Qualidade
- Toda regra de negócio precisa ter teste unitário
- Todo endpoint precisa ter ao menos um teste de integração de caminho feliz e triste
- Erros de validação e regras de negócio violadas retornam formato RFC 7807 (ProblemDetails) com status HTTP correspondente (ex: 400, 404, 409)

## Convenções
- Nomes de classe e arquivos em inglês, mensagens de erro em português
- Commit no formato **Conventional Commits**: descrição, com o identificador do requisito
- Todo teste unitário no back-end deve seguir explicitamente o padrão **AAA (Arrange, Act, Assert)**.

## Governança
- Se o plano contradiz a constituição, quem muda é o plano
- Se a constituição precisa mudar, muda em commit separado.
