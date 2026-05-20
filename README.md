# Digital Bank Platform

A microservices-based digital banking backend built with Java 21 and Spring Boot 3. Covers user management, bank accounts and financial transactions, secured with Keycloak and communicating asynchronously through Kafka.

---

## Features

- User registration with automatic default account creation (event-driven)
- Bank account management — open, block, close; balance tracking
- Financial transactions — DEPOSIT, WITHDRAWAL, TRANSFER
- Async transaction processing via Kafka: transaction-service saves `PENDING`, account-service validates and applies the balance change, then emits `balance.updated` to mark it `COMPLETED` or `FAILED`
- Idempotent transaction API — retry safely without double-processing
- JWT authentication via Keycloak; role-based access (ADMIN / customer)
- Rate limiting at the gateway (Redis token bucket)
- Centralized configuration and service discovery
- Observability: Prometheus metrics + pre-provisioned Grafana dashboard

---

## Tech Stack

| | |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.3, Spring Cloud 2023 |
| **Security** | Spring Security, OAuth2 Resource Server, Keycloak 24 |
| **Persistence** | PostgreSQL 16, Spring Data JPA, Flyway |
| **Messaging** | Apache Kafka 3.7 (KRaft — no Zookeeper) |
| **Cache** | Redis 7 |
| **Service discovery** | Netflix Eureka |
| **Centralized config** | Spring Cloud Config Server |
| **API documentation** | SpringDoc OpenAPI 2 (Swagger UI) |
| **Observability** | Micrometer, Prometheus, Grafana |
| **Build** | Maven (multi-module) |
| **Containerization** | Docker, Docker Compose |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      Docker network                       │
│                                                           │
│  ┌──────────┐    ┌───────────┐    ┌──────────────────┐  │
│  │  Client  │───▶│  Gateway  │───▶│  user-service    │  │
│  └──────────┘    │  :8080    │    │  account-service  │  │
│                  └─────┬─────┘    │  transaction-svc  │  │
│                        │ JWT      └────────┬─────────┘  │
│                        ▼                   │             │
│                  ┌──────────┐   ┌──────────▼──────────┐ │
│                  │ Keycloak │   │        Kafka         │ │
│                  │  :8180   │   │  (KRaft, no ZK)      │ │
│                  └──────────┘   └─────────────────────┘ │
│                                                           │
│  config-server :8888 · discovery-service :8761           │
│  PostgreSQL · Redis · Prometheus :9090 · Grafana :3000   │
└──────────────────────────────────────────────────────────┘
```

Each service owns its own database (`userdb`, `accountdb`, `transactiondb`). Schema migrations are managed by Flyway.

### Services

**`user-service`** — User registration and profile management. Publishes `user.created` on registration; account-service consumes it to open a default checking account automatically.

**`account-service`** — Manages bank accounts and balances. Consumes `transaction.created` events, applies balance changes, and emits `balance.updated`.

**`transaction-service`** — Handles DEPOSIT, WITHDRAWAL and TRANSFER. Transactions are created as `PENDING` and updated asynchronously. Calls account-service via Feign for a pre-validation check before persisting.

**`api-gateway`** — Single entry point. Validates JWTs, injects `X-Correlation-ID`, and routes to services via Eureka.

**`config-server`** — Serves per-service YAML configuration. Each service fetches its config on startup.

**`discovery-service`** — Eureka server. Services register on startup; the gateway resolves names to instances.

---

## Security

All services are OAuth2 Resource Servers — each one validates JWTs independently (defense in depth). The gateway validates first so downstream services only receive authenticated requests.

Roles are extracted from the `realm_access.roles` claim:

| Role | Access |
|------|--------|
| `ADMIN` | Full access — list users, deactivate accounts, block accounts |
| `customer` | Authenticated endpoints — own profile, own accounts, transactions |

Feign clients between services forward the original `Authorization` header so inter-service calls are also authenticated.

**Default Keycloak users (pre-configured in the realm export):**

| User | Password | Role |
|------|----------|------|
| admin@digitalbank.com | admin123 | ADMIN |
| customer1@digitalbank.com | admin123 | customer |

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `user.created` | user-service | account-service | Open default account after registration |
| `transaction.created` | transaction-service | account-service | Apply balance changes |
| `balance.updated` | account-service | transaction-service | Mark transaction COMPLETED or FAILED |
| `transfer.completed` | transaction-service | — | Reserved for future notification consumers |

Consumers use `@RetryableTopic` — 3 attempts with 2 s / 4 s backoff. On exhaustion, events land on a `.dlt` topic.

---

## Observability

Every service exposes `/actuator/prometheus`. Prometheus scrapes all of them; Grafana has a pre-provisioned dashboard tracking HTTP request rate, latency percentiles, JVM memory, DB connections and Kafka consumer lag.

Access Grafana at `http://localhost:3000` (admin / admin123).

---

## Running Locally

**Prerequisites:** Docker, Docker Compose, Java 21, Maven 3.9+

```bash
# 1. Copy env file (defaults work out of the box)
cp .env.example .env

# 2. Build
mvn clean package -DskipTests
docker compose build

# 3. Start
docker compose up -d
```

Keycloak takes 60–90 s on first boot while it imports the realm. Wait for all containers to show `healthy`:

```bash
docker compose ps
```

**Development mode** (run services from your IDE, infra only in Docker):

```bash
docker compose -f docker-compose.infra.yml up -d
```

---

## Running Tests

```bash
# All modules
mvn test

# Single service
mvn test -pl user-service
mvn test -pl account-service
mvn test -pl transaction-service
```

Tests use Mockito for unit tests and `@DataJpaTest` with H2 for repository tests.

---

## API Docs

All requests go through the gateway at `http://localhost:8080`. Swagger UI is available per service (useful during IDE development):

| Service | Swagger |
|---------|---------|
| user-service | http://localhost:8081/swagger-ui.html |
| account-service | http://localhost:8082/swagger-ui.html |
| transaction-service | http://localhost:8083/swagger-ui.html |

### Get a JWT

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/digital-bank/protocol/openid-connect/token \
  -d "client_id=digital-bank-client" \
  -d "grant_type=password" \
  -d "username=admin@digitalbank.com" \
  -d "password=admin123" | jq -r .access_token)
```

### Users

```bash
# Register (public — no token needed)
curl -s -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Maria Souza","email":"maria@email.com","cpf":"529.982.247-25","birthDate":"1990-06-15"}' | jq .

# List all users (ADMIN)
curl -s http://localhost:8080/api/v1/users -H "Authorization: Bearer $TOKEN" | jq .

# Deactivate user (ADMIN)
curl -s -X DELETE http://localhost:8080/api/v1/users/{id} -H "Authorization: Bearer $TOKEN"
```

### Accounts

```bash
# Get balance
curl -s http://localhost:8080/api/v1/accounts/{id}/balance -H "Authorization: Bearer $TOKEN" | jq .

# Block account (ADMIN)
curl -s -X PATCH http://localhost:8080/api/v1/accounts/{id}/block -H "Authorization: Bearer $TOKEN"
```

### Transactions

```bash
# Deposit
curl -s -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"sourceAccountId":"{accountId}","amount":1000.00,"type":"DEPOSIT"}' | jq .

# Transfer
curl -s -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"sourceAccountId":"{srcId}","destinationAccountId":"{dstId}","amount":250.00,"type":"TRANSFER"}' | jq .

# Bank statement (with filters)
curl -s "http://localhost:8080/api/v1/transactions/account/{accountId}/statement?from=2025-01-01&to=2025-12-31&type=TRANSFER" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Include `"idempotencyKey": "your-uuid"` in the transaction body to safely retry without double-processing.

---

## Service URLs

| | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Keycloak admin | http://localhost:8180/admin |
| Eureka dashboard | http://localhost:8761 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

---

## Project Structure

```
digital-bank-platform/
├── api-gateway/
├── config-server/
│   └── src/main/resources/config/     # per-service YAML
├── discovery-service/
├── user-service/
├── account-service/
├── transaction-service/
├── infra/
│   ├── keycloak/realm-export.json     # pre-configured realm + users
│   ├── postgres/init.sql              # creates databases on first boot
│   ├── prometheus/prometheus.yml
│   └── grafana/                       # provisioned dashboards
├── docker-compose.yml                 # full stack
├── docker-compose.infra.yml           # infra only (for IDE development)
├── .env.example
└── pom.xml                            # Maven multi-module root
```

---

## Future Improvements

- Integration tests with Testcontainers (PostgreSQL + Kafka)
- Notification service consuming `transfer.completed` (email / push)
- Daily transaction limits per account type
- Audit log service tracking who changed what and when
- CI/CD pipeline with GitHub Actions
- Account closure with balance validation

---

## License

MIT
