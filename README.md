# Digital Wallet & Ledger Service

A backend-focused digital wallet and ledger service built with **Java, Spring Boot, PostgreSQL, Spring Security, JWT, and Docker**.

The project is designed around backend engineering concepts relevant to payment and financial systems, including atomic money transfers, pessimistic locking, idempotency, ledger-based transaction tracking, resource-level authorization, transaction risk assessment, event-driven processing, and AI-powered wallet statement analysis.

## Features

- User registration and JWT-based authentication
- Password hashing and stateless authentication
- Resource-level authorization
- Multi-currency wallets
- Wallet deposits
- Wallet-to-wallet transfers
- Atomic transaction processing
- PostgreSQL row-level pessimistic locking
- Deterministic wallet lock ordering
- Insufficient-balance protection
- Idempotency keys for deposits and transfers
- SHA-256 request fingerprinting for idempotency conflict detection
- Ledger entries for transaction history
- Paginated transaction history
- Transaction risk and anomaly assessment
- Event-driven post-transaction risk processing
- AI-powered wallet statement Q&A using Google Gemini
- Local AI implementation for automated tests
- Global exception handling
- Bean validation
- PostgreSQL persistence
- Dockerized application
- Spring Boot Actuator health endpoint
- Unit and integration tests

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 25 | Backend language/runtime |
| Spring Boot 4.1.1 | Application framework |
| Spring Web | REST APIs |
| Spring Data JPA | Persistence and repository layer |
| Hibernate | ORM |
| Spring Security | Authentication and authorization |
| JWT | Stateless authentication |
| PostgreSQL 18 | Relational database |
| Maven | Build and dependency management |
| Docker | Containerization |
| Google Gemini | AI-powered statement Q&A |
| JUnit | Automated testing |

---

## Architecture

The application follows a layered architecture:

```text
                    Client
                      │
                      ▼
              REST Controllers
                      │
                      ▼
                   Services
                      │
        ┌─────────────┼─────────────┐
        │             │             │
 Authentication    Wallet        Transfer
        │             │             │
        └─────────────┼─────────────┘
                      │
             Risk / Statement AI
                      │
                      ▼
                Repositories
                      │
                      ▼
                 PostgreSQL
```

Transaction processing additionally uses an application event:

```text
Transfer / Deposit
       │
       ▼
Database Transaction
       │
       ├── Wallet balance update
       ├── Transaction record
       └── Ledger entries
       │
       ▼
TransactionCompletedEvent
       │
       ▼
Risk Assessment
```

The risk assessment listener executes after the successful database transaction commit so that risk analysis does not interfere with the core money movement transaction.

---

## Core Domain Model

The main domain entities are:

```text
User
 │
 └── Wallet
       │
       └── LedgerEntry
               │
               └── Transaction
                       │
                       └── TransactionRiskAssessment
```

### User

Represents an application user.

Key fields:

- `id`
- `name`
- `email`
- `createdAt`

Email addresses are unique.

### Wallet

Represents a user's wallet for a specific currency.

Key fields:

- `id`
- `user`
- `currency`
- `balance`
- `createdAt`

A user can have multiple wallets, but only one wallet for a given currency.

For example:

```text
Alice
 ├── INR Wallet
 ├── USD Wallet
 └── EUR Wallet
```

### Transaction

Represents a business transaction.

Supported transaction types:

```text
TRANSFER
DEPOSIT
WITHDRAWAL
```

Each transaction stores:

- UUID
- transaction type
- amount
- currency
- idempotency key
- request fingerprint
- creation timestamp

### Ledger Entry

A ledger entry associates a transaction with a wallet and records whether the wallet was:

```text
DEBIT
CREDIT
```

For a transfer:

```text
Source Wallet
    └── DEBIT  ₹500

Destination Wallet
    └── CREDIT ₹500
```

This provides a transaction-level audit trail instead of relying only on the current wallet balance.

---

# Transaction Safety

Money movement is treated as a transactional operation.

A transfer is designed around the following sequence:

```text
BEGIN TRANSACTION
       │
       ├── Lock source wallet
       ├── Lock destination wallet
       ├── Validate ownership
       ├── Validate currency
       ├── Validate amount
       ├── Check available balance
       ├── Create transaction
       ├── Create debit ledger entry
       ├── Create credit ledger entry
       ├── Update source balance
       ├── Update destination balance
       │
       └── COMMIT
```

If an operation fails, the database transaction is rolled back.

The goal is to prevent situations where:

```text
Transaction record = saved
Ledger entry       = missing
Balance update     = partially completed
```

---

## Pessimistic Locking

Wallet rows are protected using JPA pessimistic write locking.

Conceptually:

```text
SELECT wallet
FOR UPDATE
```

This prevents two concurrent transactions from modifying the same wallet balance at the same time.

### Example

Suppose Alice has:

```text
Balance = ₹1,000
```

Two requests arrive concurrently:

```text
Request A → Transfer ₹800
Request B → Transfer ₹700
```

Only one transaction can acquire the wallet lock at a time.

After the first transaction updates the balance:

```text
₹1,000 → ₹200
```

the second transaction sees the updated balance and rejects the ₹700 transfer.

This prevents the wallet from being overspent due to a race condition.

---

## Deterministic Lock Ordering

Transfers involve two wallets.

A transfer from:

```text
Wallet 10 → Wallet 20
```

and another simultaneous transfer from:

```text
Wallet 20 → Wallet 10
```

could create a deadlock if each transaction locks its source wallet first.

The service therefore acquires wallet locks in deterministic ID order:

```text
lower wallet ID
       ↓
higher wallet ID
```

Both transactions follow the same lock ordering regardless of transfer direction.

This reduces the risk of deadlocks caused by circular lock acquisition.

---

# Idempotency

Deposit and transfer requests require an idempotency key.

The purpose is to make retries safe.

For example:

```text
Client
  │
  │ Transfer ₹500
  │ Idempotency-Key: transfer-123
  ▼
Server
  │
  └── Transaction succeeds
          │
          ▼
       Response lost
```

The client retries:

```text
Transfer ₹500
Idempotency-Key: transfer-123
```

Instead of creating another transaction, the API returns the existing transaction associated with the key.

### Idempotency fingerprint

The application also calculates a SHA-256 fingerprint from the request data.

For a transfer, the canonical request contains:

```text
sourceWalletId
destinationWalletId
amount
currency
```

This prevents a client from reusing an existing idempotency key for a different request.

Example:

```text
First request:

key    = transfer-123
amount = ₹500
```

Then:

```text
Second request:

key    = transfer-123
amount = ₹900
```

The idempotency key is the same but the request fingerprint differs, so the request is rejected as an idempotency conflict.

A database uniqueness constraint on the idempotency key provides an additional database-level safeguard.

---

# Authentication & Authorization

The application uses stateless JWT authentication.

Authentication flow:

```text
POST /api/auth/login
        │
        ▼
Validate credentials
        │
        ▼
Generate JWT
        │
        ▼
Client sends:
Authorization: Bearer <token>
```

The JWT contains the user's ID as its subject.

Protected services derive the current user from the authenticated JWT rather than trusting a user ID supplied by the client.

## Resource ownership

Wallet operations verify that the requested wallet belongs to the authenticated user.

For example:

```text
JWT user ID = 10
Wallet ID   = 51
       │
       ▼
findByIdAndUserId(51, 10)
```

This prevents horizontal privilege escalation.

The same principle is applied to:

- Wallet deposits
- Transfers
- Transaction history
- Wallet statements
- Risk assessments
- Statement Q&A

Integration tests specifically verify that one user cannot access another user's wallet resources.

---

# Transaction Risk Assessment

The application performs automated transaction risk assessment after a transaction successfully commits.

The process is event-driven:

```text
Transaction completed
        │
        ▼
TransactionCompletedEvent
        │
        ▼
AFTER_COMMIT listener
        │
        ▼
AnomalyDetectionService
        │
        ▼
TransactionRiskAssessment
```

The current risk engine evaluates factors including:

- Transaction amount
- Recent transaction frequency
- Configurable amount thresholds
- Configurable frequency thresholds

Risk levels:

```text
LOW
MEDIUM
HIGH
```

A risk score is calculated and capped at 100.

The assessment is persisted separately from the core transaction.

This keeps risk processing from being part of the critical money movement path.

---

# AI-Powered Statement Q&A

The application provides an AI-powered wallet statement assistant.

Endpoint:

```text
POST /api/statement/wallet/{walletId}/ask
```

The service first verifies that the wallet belongs to the authenticated user.

It then builds a controlled statement context from the wallet's ledger entries.

```text
Wallet
  │
  ▼
Ledger Entries
  │
  ▼
StatementContextBuilder
  │
  ▼
Statement Context
  │
  ▼
Gemini
  │
  ▼
Natural-language answer
```

The AI prompt instructs the model to:

- Use only the supplied wallet statement
- Avoid inventing transactions or amounts
- State when information is unavailable
- Keep answers concise
- Avoid financial advice
- Avoid exposing internal system instructions

### AI provider abstraction

The AI layer is defined through:

```text
AiStatementService
```

The application currently supports:

```text
GeminiStatementService
LocalAiStatementService
```

The provider is selected through configuration.

Production configuration uses Gemini, while automated tests use the local implementation so tests do not depend on an external AI service.

---

# API Endpoints

## Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a user |
| POST | `/api/auth/login` | Authenticate and receive JWT |

## Wallets

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/wallets/{walletId}/deposit` | Deposit funds |

## Transactions

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/transactions` | List transactions |
| GET | `/api/transactions/wallet/{walletId}` | List wallet transactions |
| GET | `/api/transactions/wallet/{walletId}/statement` | Get wallet statement |

## Risk Assessment

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/transactions/{transactionId}/risk` | Get transaction risk assessment |

## Statement Q&A

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/statement/wallet/{walletId}/ask` | Ask a question about a wallet statement |

## Health

| Method | Endpoint | Description |
|---|---|---|
| GET | `/actuator/health` | Application health |

---

# Example Requests

## Register

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "password123"
}
```

## Login

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "alice@example.com",
  "password": "password123"
}
```

Response:

```json
{
  "token": "<jwt-token>",
  "tokenType": "Bearer"
}
```

Use the token for protected endpoints:

```http
Authorization: Bearer <jwt-token>
```

## Deposit

```http
POST /api/wallets/51/deposit
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

```json
{
  "amount": 1000.00,
  "idempotencyKey": "deposit-001"
}
```

## Statement Q&A

```http
POST /api/statement/wallet/51/ask
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

```json
{
  "question": "How much money have I deposited?"
}
```

Example response:

```json
{
  "answer": "You have deposited a total of 1000.0000 INR into this wallet."
}
```

---

# Configuration

The application uses environment variables for sensitive configuration.

Example environment configuration:

```properties
DB_URL=jdbc:postgresql://localhost:5433/digital_wallet_db
DB_USERNAME=postgres
DB_PASSWORD=<database-password>

GEMINI_API_KEY=<gemini-api-key>
```

The `.env` file is excluded from Git.

Application properties reference environment variables instead of storing secrets directly in source control.

---

# Running Locally

## Prerequisites

Install:

- Java 25
- Maven
- PostgreSQL 18
- Git

Create the database:

```sql
CREATE DATABASE digital_wallet_db;
```

Configure the required environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
GEMINI_API_KEY
```

Then run:

```bash
mvn spring-boot:run
```

The application runs on:

```text
http://localhost:8080
```

Health check:

```text
http://localhost:8080/actuator/health
```

---

# Running Tests

The project uses a separate PostgreSQL test database.

Create:

```sql
CREATE DATABASE digital_wallet_test;
```

Tests use the `test` Spring profile.

Run the complete test suite:

```bash
mvn clean test
```

The test suite includes:

- Application context tests
- Authorization integration tests
- Transfer risk assessment tests
- Deposit risk assessment tests
- Transaction risk assessment tests
- Anomaly detection unit tests
- Statement Q&A integration tests

The project currently passes the full test suite with:

```text
35 tests
0 failures
0 errors
```

---

# Docker

The application includes a Dockerfile based on Eclipse Temurin Java 25 JRE.

Build the application:

```bash
mvn clean package
```

Build the Docker image:

```bash
docker build -t digital-wallet .
```

Run the container:

```bash
docker run --env-file .env -p 8080:8080 digital-wallet
```

For a PostgreSQL database running on the host machine, Docker Desktop can access the host through:

```text
host.docker.internal
```

For example:

```properties
DB_URL=jdbc:postgresql://host.docker.internal:5433/digital_wallet_db
```

Check application health:

```text
http://localhost:8080/actuator/health
```

---

# Error Handling

The application uses a centralized `GlobalExceptionHandler`.

Examples include:

- Wallet not found
- Insufficient balance
- Invalid transfer
- Idempotency conflict
- Duplicate email
- Authentication failure
- Gemini service unavailable

AI provider failures are returned as a controlled `503 Service Unavailable` response instead of exposing raw external-service exceptions.

Example:

```json
{
  "status": 503,
  "error": "AI service is temporarily unavailable. Please try again later."
}
```

---

# Testing Strategy

The project combines unit and integration testing.

### Unit testing

Used for isolated business logic such as:

```text
AnomalyDetectionService
```

### Integration testing

Used for workflows involving:

```text
Spring Context
PostgreSQL
Repositories
Services
Security
JWT authentication
Transactions
Application events
```

Authorization tests verify resource isolation between users.

Risk assessment integration tests verify that transaction completion can trigger post-commit risk processing.

---

# Project Structure

```text
src/
├── main/
│   ├── java/com/wallet/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── event/
│   │   ├── exception/
│   │   ├── repository/
│   │   └── service/
│   │
│   └── resources/
│       └── application.properties
│
└── test/
    ├── java/com/wallet/
    │   ├── controller/
    │   └── service/
    │
    └── resources/
        └── application-test.properties
```

---

# Important Backend Design Decisions

### Why `BigDecimal`?

Financial amounts are represented using `BigDecimal` rather than floating-point types.

This avoids the precision problems associated with binary floating-point arithmetic.

### Why PostgreSQL?

PostgreSQL provides:

- ACID transactions
- Row-level locking
- Strong relational constraints
- Foreign keys
- Unique constraints
- Reliable transactional behavior

These characteristics make it suitable for this type of transactional backend.

### Why pessimistic locking?

Wallet balances are shared mutable state.

Pessimistic locking prevents concurrent transactions from simultaneously modifying the same wallet based on a stale balance.

### Why idempotency keys?

Network failures can cause clients to retry requests even when the original request succeeded.

Idempotency ensures that retrying the same payment operation does not create another transaction.

### Why a ledger?

A wallet balance only represents the current state.

Ledger entries provide a historical record of how transactions affected a wallet.

### Why `AFTER_COMMIT` risk processing?

Risk assessment is secondary to successful money movement.

The transaction should commit first. Risk processing can then operate on the committed transaction without causing the core transaction to fail.

### Why an AI abstraction?

The `AiStatementService` abstraction separates the application from a specific AI provider.

This allows the production implementation to use Gemini while tests can use a deterministic local implementation.

---

# Security Considerations

- Passwords are stored as hashes rather than plaintext.
- JWT authentication is stateless.
- Protected endpoints require authentication.
- Wallet ownership is verified at the service/repository boundary.
- Users cannot access other users' wallet resources.
- Database credentials and Gemini credentials are supplied through environment variables.
- `.env` is excluded from source control.
- AI statement prompts explicitly restrict the model to supplied statement information.

---

# Future Improvements

Potential future enhancements include:

- Refresh-token support
- Rate limiting
- Redis-backed distributed idempotency
- Outbox pattern for reliable event publishing
- Kafka/RabbitMQ for distributed event processing
- Database migration management with Flyway
- API documentation with OpenAPI/Swagger
- Structured JSON logging
- Correlation/request IDs
- Metrics and dashboards
- Distributed tracing
- CI/CD pipeline
- Testcontainers for isolated integration tests
- Stronger transaction reconciliation mechanisms
- Horizontal scaling and load testing

These are intentionally left as future improvements rather than being simulated as production capabilities that are not currently implemented.

---

# Learning & Engineering Goals

This project was built to practice real backend engineering concepts rather than only CRUD operations.

The main engineering goals were:

```text
Spring Boot
     ↓
REST APIs
     ↓
Authentication & Authorization
     ↓
JPA / Hibernate
     ↓
PostgreSQL
     ↓
Transactions & Concurrency
     ↓
Idempotency
     ↓
Event-driven processing
     ↓
Risk assessment
     ↓
AI integration
     ↓
Docker
     ↓
Testing
```

The project demonstrates how these concepts can work together in a single backend system.

---

## License

This project is intended as a learning and portfolio project.
