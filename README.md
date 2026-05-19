# Digital Bank Platform

A backend portfolio project simulating a digital bank built with Java microservices.

## Overview

The platform covers the core flows of a digital bank: user registration, account management and financial transactions, all secured with JWT and communicating asynchronously via Kafka.

## Architecture

```
                        ┌─────────────────┐
                        │   API Gateway   │  :8080
                        │  (Spring Cloud) │
                        └────────┬────────┘
                                 │ routes
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
   ┌──────▼──────┐       ┌───────▼──────┐    ┌─────────▼────────┐
   │ user-service│       │account-service│   │transaction-service│
   │    :8081    │       │    :8082      │   │      :8083         │
   └──────┬──────┘       └───────┬───────┘   └────────┬──────────┘
          │                      │                     │
          └──────────────────────┴─────────────────────┘
                         Kafka (async events)
                         PostgreSQL (per-service DB)
                         Redis (rate limiting / cache)
```

**Infrastructure services:**
- `config-server` — centralized config via Spring Cloud Config
- `discovery-service` — service registry (Eureka)
- Kafka (KRaft, no Zookeeper)
- Keycloak 24 — authentication and JWT issuing
- Prometheus + Grafana — metrics and dashboards

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3, Spring Cloud 2023 |
| Security | Spring Security + Keycloak (JWT/OAuth2) |
| Persistence | PostgreSQL 16, Spring Data JPA, Flyway |
| Messaging | Apache Kafka 3.7 (KRaft) |
| Cache | Redis 7 |
| Service discovery | Netflix Eureka |
| Config | Spring Cloud Config Server |
| API docs | SpringDoc OpenAPI (Swagger UI) |
| Observability | Micrometer, Prometheus, Grafana |
| Build | Maven (multi-module) |
| Runtime | Docker, Docker Compose |

## Services

### user-service
Manages user registration and profile.
- `POST /api/v1/users` — register (public)
- `GET  /api/v1/users` — list all (ADMIN)
- `GET  /api/v1/users/{id}` — find by id (authenticated)
- `PUT  /api/v1/users/{id}` — update (authenticated)
- `DELETE /api/v1/users/{id}` — deactivate (ADMIN)

On user creation, publishes a `user.created` event → account-service automatically opens a default checking account.

### account-service
Manages bank accounts and balance updates.
- `POST /api/v1/accounts` — open account (authenticated)
- `GET  /api/v1/accounts/{id}` — find by id
- `GET  /api/v1/accounts/{id}/balance` — current balance
- `GET  /api/v1/accounts/user/{userId}` — accounts by user
- `PATCH /api/v1/accounts/{id}/block` — block account (ADMIN)

Consumes `transaction.created` events from Kafka to apply balance changes, then publishes `balance.updated`.

### transaction-service
Processes financial transactions.
- `POST /api/v1/transactions` — create transaction (DEPOSIT / WITHDRAWAL / TRANSFER)
- `GET  /api/v1/transactions/{id}` — find by id
- `GET  /api/v1/transactions/account/{accountId}` — list by account
- `GET  /api/v1/transactions/account/{accountId}/statement` — filtered statement (date, type, paginated)

Transactions start as `PENDING`, are processed asynchronously via Kafka, and end as `COMPLETED` or `FAILED`.

## Running locally

### Prerequisites
- Docker and Docker Compose
- Java 21 and Maven (only if building locally)

### Quick start

```bash
# 1. Copy and review environment variables
cp .env.example .env

# 2. Build all service images
mvn clean package -DskipTests
docker compose build

# 3. Start everything
docker compose up -d

# 4. Check that all containers are healthy
docker compose ps
```

Keycloak takes ~60–90 seconds on the first boot. Wait for all containers to show `healthy` before making requests.

### Service URLs

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| Keycloak admin | http://localhost:8180 |
| Eureka dashboard | http://localhost:8761 |
| Swagger — user | http://localhost:8081/swagger-ui.html |
| Swagger — account | http://localhost:8082/swagger-ui.html |
| Swagger — transaction | http://localhost:8083/swagger-ui.html |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

### Getting a token

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/digital-bank/protocol/openid-connect/token \
  -d "client_id=digital-bank-client" \
  -d "grant_type=password" \
  -d "username=admin@digitalbank.com" \
  -d "password=admin123" | jq -r .access_token)
```

Then pass it on requests:
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users
```

### Running only infrastructure (for IDE development)

```bash
docker compose -f docker-compose.infra.yml up -d
```

Then run each service from your IDE. They will register with Eureka and pick config from the config-server.

## Running tests

```bash
# All services
mvn test

# Single service
mvn test -pl user-service
```

## Project structure

```
digital-bank-platform/
├── api-gateway/
├── config-server/
│   └── src/main/resources/config/   # per-service YAML configs
├── discovery-service/
├── user-service/
├── account-service/
├── transaction-service/
├── infra/
│   ├── keycloak/                    # realm export
│   ├── postgres/                    # init SQL
│   ├── prometheus/
│   └── grafana/
├── docker-compose.yml               # full stack
├── docker-compose.infra.yml         # infra only (for IDE dev)
└── .env.example
```
