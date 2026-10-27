# 🎃 Encurtador de Links - Balta Halloween 2026

<p align="center">
  <img src="https://baltaio.blob.core.windows.net/static/images/v4/challenges/halloween-2026/01.png" width="160" />
  <img src="https://baltaio.blob.core.windows.net/static/images/v4/challenges/halloween-2026/02.png" width="160" />
</p>

Projeto desenvolvido durante o desafio **Halloween 2026** realizado pelo [balta.io](https://balta.io), utilizando **Spec-Driven Development (SDD)** de ponta a ponta.

---

## 🛠️ Stack Tecnológica

- **Linguagem:** Java 21 (LTS)
- **Framework:** Spring Boot 3.3.5
- **Persistência:** PostgreSQL 16 via Spring Data JPA e Hibernate
- **Migrações de Banco:** Flyway
- **Documentação:** SpringDoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`)
- **Testes:** JUnit 5, Mockito, AssertJ e Testcontainers (PostgreSQL)

---

## 🏛️ Arquitetura e Decisões

- **Padrão em Camadas:** `Controller` ➔ `Service` ➔ `Repository`
- **Isolamento de Entidades:** Entidades JPA nunca são expostas na camada Web; uso estrito de DTOs (`records`).
- **Unicidade Case-Sensitive:** Índice único `uq_links_code ON links (code)` com validação estrita no PostgreSQL.
- **Concorrência Atômica:** Incremento de acessos via query `@Modifying` nativa, evitando perda de contagem sob acessos simultâneos.
- **Redirecionamento:** HTTP `302 Found` para possibilitar métricas de acesso sem cache definitivo no cliente.
- **Tratamento de Erros:** Padronização segundo a **RFC 7807 (ProblemDetails)** com mensagens em português.

---

## 🚀 Como Executar

### 1. Pré-requisitos
- [Docker](https://www.docker.com/) e Docker Compose
- [Java 21](https://www.oracle.com/java/technologies/downloads/#java21)

### 2. Iniciar o Banco de Dados
```bash
docker compose up -d
```

### 3. Iniciar a Aplicação
```bash
./mvnw spring-boot:run
```
A API estará disponível na porta `8081`:
- **Base URL:** `http://localhost:8081`
- **Swagger UI:** `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8081/v3/api-docs`

---

## 🧪 Como Executar os Testes

Para executar toda a suíte de testes unitários e de integração (padrão AAA):

```bash
./mvnw test
```

---

## 📬 Endpoints da API

### 1. Criar Link Encurtado
- **POST** `/api/links`
- **Body (JSON):**
  ```json
  {
    "targetUrl": "https://balta.io/artigos/sdd-com-ia",
    "code": "artigo-sdd",
    "expiresAtUtc": "2026-12-31T23:59:59Z"
  }
  ```
  *(Os campos `code` e `expiresAtUtc` são opcionais)*

### 2. Redirecionar para URL Original
- **GET** `/{code}`
- Retorna `302 Found` com cabeçalho `Location: <targetUrl>` e incrementa o contador.

### 3. Consultar Estatísticas Públicas
- **GET** `/api/links/{code}/stats`
- Retorna `200 OK` com dados do link e total de acessos.

