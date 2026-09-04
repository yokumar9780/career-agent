# Career Agent

AI-powered job search assistant that discovers, matches, and helps apply to jobs across multiple portals.

## Architecture

```
career-agent/
├── career-agent-service/       # Spring Boot 4.1 backend (Java 25)
├── career-agent-ui/            # Next.js 16 frontend (TypeScript)
├── browser-automation-service/ # Playwright service (planned)
├── docker-compose.yml          # All services + PostgreSQL + Qdrant
├── scripts/
│   └── start-local.sh          # One-command local dev startup
└── .env.example                # Environment variables template
```

| Service | Port | Tech |
|---|---|---|
| Backend API | 8080 | Spring Boot 4.1, Java 25, Spring AI 2.0.1 |
| Frontend | 3000 | Next.js 16, React 19, MUI 9 |
| PostgreSQL | 5432 | v17 (relational data) |
| Qdrant | 6333/6334 | v1.14 (vector embeddings) |
| MinIO | 9000/9001 | Object storage (documents, CVs) |
| Browser Automation | 4000 | Express + Playwright (planned) |

## Quick Start

### Prerequisites

- Java 25 (JDK), Maven 3.9+
- Node.js 22+, pnpm 11+
- Docker

### 1. Clone and configure

```bash
cp .env.example .env
# Edit .env with your API keys
```

### 2. Start everything

```bash
# Start infrastructure
docker compose up -d postgres minio

# Start backend
cd career-agent-service && mvn spring-boot:run &

# Start frontend
cd career-agent-ui && pnpm install && pnpm dev
```

Or use the startup script:

```bash
./scripts/start-local.sh
```

### 3. Open the app

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
- Health: http://localhost:8080/actuator/health

## Documentation

- **[Implementation Guide](./IMPLEMENTATION_GUIDE.md)** � Step-by-step walkthrough of what we're building and why, with Git branch references.
- **[Architecture Decision Records](./ARCHITECTURE_DECISIONS.md)** � Key architectural decisions with context, alternatives considered, and rationale (e.g., why email ingestion over LinkedIn API, why browser automation for auto-apply, why Qdrant over pgvector).

## Development

See individual project READMEs:
- [Backend (career-agent-service)](./career-agent-service/README.md)
- [Frontend (career-agent-ui)](./career-agent-ui/README.md)

## License

Private — not open source.
