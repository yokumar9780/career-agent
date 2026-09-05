# Career Agent — Project-Wide Rules

These rules apply across all sub-projects in this monorepo.

## Session Rules

1. **Provide a GitHub commit message** at the end of every session summarizing all changes.

2. **Verify builds pass** before claiming work is complete:
   - Backend: `mvn compile` then `mvn test`
   - Frontend: `pnpm build`

3. **Run existing tests** after every code change to ensure nothing is broken.

## Documentation Rules

4. **Update documentation** when architecture, APIs, or infrastructure change. Affected files:
   - `requirements.md`, `design.md`, `tasks.md` (in `.kiro/specs/career-agent/`)
   - `README.md` files (root, backend, frontend)
   - `IMPLEMENTATION_GUIDE.md`
   - `ARCHITECTURE_DECISIONS.md`
   - `.env.example`

5. **Keep the Implementation Guide in sync** with completed tasks (update status column).

## Infrastructure

6. **Docker Compose** manages all services: PostgreSQL, MinIO, Qdrant, backend, frontend, browser automation.

7. **Start infrastructure for local dev:** `docker compose up -d postgres minio`

8. **All secrets** go in `.env` (gitignored). Template is `.env.example`.

## Sub-Project Rules

See project-specific rules:
- [Backend Rules](./career-agent-service/AGENTS.md)
- [Frontend Rules](./career-agent-ui/AGENTS.md)
