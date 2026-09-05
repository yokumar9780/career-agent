# Career Agent - Project-Wide Rules

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

## File Writing Rules

9. **Never use PowerShell `Set-Content` or here-strings (`@""@`) to write source files.** PowerShell corrupts Unicode characters (arrows, dashes, special chars) into mojibake (`Ã¢â€ â€™`). Always use the dedicated file editing tools provided by the IDE/agent instead of terminal commands for file creation and modification.

10. **When terminal file writes are unavoidable**, always use `[System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))` to write without BOM. Never use `Set-Content -Encoding UTF8` (adds BOM that breaks Java compilation).

11. **Avoid Unicode special characters in source files written via terminal.** Use ASCII equivalents: `->` instead of `→`, `--` instead of `—`, `>=` instead of `≥`. Reserve Unicode for files written by the dedicated file editing tools.

## Sub-Project Rules

See project-specific rules:
- [Backend Rules](./career-agent-service/AGENTS.md)
- [Frontend Rules](./career-agent-ui/AGENTS.md)
