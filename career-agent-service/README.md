# Career Agent Service

Spring Boot backend for the Career Agent — an AI-powered job search assistant that discovers, matches, and helps apply to jobs.

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 25 | Runtime |
| Spring Boot | 4.1.0 | Application framework |
| Spring AI | 2.0.1 | AI agent orchestration (ChatClient, VectorStore, EmbeddingModel) |
| Spring Security | 7.x | JWT authentication, authorization |
| Spring Data JPA | 4.x | PostgreSQL data access |
| Flyway | 12.x | Database migrations |
| PostgreSQL | 17 | Relational database |
| Qdrant | 1.14 | Vector similarity search |
| jjwt | 0.12.6 | JWT token generation/validation |
| springdoc-openapi | 3.0.1 | API documentation (Swagger UI) |

## Prerequisites

- Java 25 (JDK)
- Maven 3.9+
- Docker (for PostgreSQL and Qdrant)

## Getting Started

### 1. Start infrastructure

```bash
# From the project root (parent of this directory)
docker compose up -d postgres
```

### 2. Run the service

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

### 3. Run with dev profile (verbose logging)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## API Endpoints

### Authentication (public)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register a new candidate |
| POST | `/api/v1/auth/login` | Login and receive JWT token |

### Health & Monitoring (public)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/actuator/health` | Service health check |
| GET | `/actuator/metrics` | Application metrics |
| GET | `/swagger-ui.html` | Swagger UI (API docs) |
| GET | `/v3/api-docs` | OpenAPI spec (JSON) |

### Protected endpoints (require `Authorization: Bearer <token>`)

All `/api/v1/*` endpoints (except auth) require a valid JWT token.

## Project Structure

```
src/main/java/com/careeragent/
├── CareerAgentApplication.java     # Main class
├── api/                            # REST controllers & DTOs
│   ├── AuthController.java
│   ├── GlobalExceptionHandler.java
│   ├── dto/                        # Request/response records
│   └── exception/                  # Custom exceptions
├── domain/                         # JPA entities & enums
├── repository/                     # Spring Data repositories
├── infrastructure/
│   ├── config/                     # CORS, Flyway, Web config
│   ├── security/                   # JWT, auth filter, Spring Security
│   ├── llm/                        # LLM rate limiter, ChatClient config
│   └── observability/              # Correlation ID, metrics
├── integration/
│   ├── portal/                     # Job portal adapter interfaces
│   │   └── linkedin/               # LinkedIn adapter implementation
│   ├── vector/                     # Qdrant embedding service
│   ├── okf/                        # OKF knowledge bundle writers
│   ├── email/                      # IMAP email listener
│   ├── browser/                    # Browser automation client
│   └── session/                    # Portal session encryption
├── service/                        # Business logic services
├── agent/                          # AI agents (profile, matching, etc.)
├── workflow/                       # Workflow engine
├── scheduler/                      # Scheduled tasks
└── tool/                           # Spring AI tools

src/main/resources/
├── application.yml                 # Base configuration
├── application-dev.yml             # Dev profile (verbose logging)
├── db/migration/                   # Flyway SQL migrations
│   ├── V1__init.sql
│   └── V2__candidate_profile.sql
└── prompts/                        # AI agent system prompts (OKF format)
```

## Configuration

All configuration is externalized via environment variables. See `../.env.example` for the full list.

Key variables:

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `career_agent` | Database name |
| `DB_USERNAME` | `career_agent` | Database user |
| `DB_PASSWORD` | `career_agent` | Database password |
| `JWT_SECRET` | (placeholder) | JWT signing key (change in production!) |
| `JWT_EXPIRATION_MS` | `86400000` | Token expiry (24h) |
| `OPENAI_API_KEY` | (placeholder) | OpenAI API key |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Allowed frontend origins |
| `QDRANT_HOST` | `localhost` | Qdrant host |
| `QDRANT_GRPC_PORT` | `6334` | Qdrant gRPC port |

## Database Migrations

Managed by Flyway. Migrations run automatically on startup.

```bash
# Migrations are in:
src/main/resources/db/migration/

# Naming convention:
V{version}__{description}.sql
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PasswordValidationPropertyTest

# Run with verbose output
mvn test -Dtest=JwtTokenProviderTest -Dsurefire.useFile=false
```

### Test categories

| Type | Framework | Location |
|---|---|---|
| Property-based | jqwik 1.9.2 | `*PropertyTest.java` |
| Unit | JUnit 5 + Mockito | `*Test.java` |

## Profiles

| Profile | Activation | Use case |
|---|---|---|
| (default) | No profile set | Production-like, INFO logging |
| `dev` | `-Dspring-boot.run.profiles=dev` | Verbose logging: SQL, Security, HTTP, Flyway |

## Docker

```bash
# Build the image
docker build -t career-agent-service .

# Run via Docker Compose (from project root)
docker compose up career-agent-service
```
