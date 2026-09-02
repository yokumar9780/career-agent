#!/usr/bin/env bash
# =============================================================================
# Career Agent — Local Development Startup Script
# =============================================================================
# Starts PostgreSQL, backend, and frontend for local E2E testing.
#
# Usage:
#   ./scripts/start-local.sh          # Start all services
#   ./scripts/start-local.sh --stop   # Stop all services
#
# Prerequisites:
#   - Docker (for PostgreSQL)
#   - Java 25 + Maven
#   - Node.js 22 + pnpm
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[career-agent]${NC} $1"; }
ok()   { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1"; }

PID_DIR="$PROJECT_ROOT/.pids"
mkdir -p "$PID_DIR"

# ---------------------------------------------------------------------------
# Stop all services
# ---------------------------------------------------------------------------
stop_all() {
    log "Stopping all services..."

    if [ -f "$PID_DIR/backend.pid" ]; then
        BACKEND_PID=$(cat "$PID_DIR/backend.pid")
        if kill -0 "$BACKEND_PID" 2>/dev/null; then
            kill "$BACKEND_PID" 2>/dev/null || true
            ok "Backend stopped (PID $BACKEND_PID)"
        fi
        rm -f "$PID_DIR/backend.pid"
    fi

    if [ -f "$PID_DIR/frontend.pid" ]; then
        FRONTEND_PID=$(cat "$PID_DIR/frontend.pid")
        if kill -0 "$FRONTEND_PID" 2>/dev/null; then
            kill "$FRONTEND_PID" 2>/dev/null || true
            ok "Frontend stopped (PID $FRONTEND_PID)"
        fi
        rm -f "$PID_DIR/frontend.pid"
    fi

    cd "$PROJECT_ROOT"
    docker compose stop postgres 2>/dev/null && ok "PostgreSQL stopped" || true

    log "All services stopped."
}

if [ "${1:-}" = "--stop" ]; then
    stop_all
    exit 0
fi

# ---------------------------------------------------------------------------
# 1. Start PostgreSQL
# ---------------------------------------------------------------------------
log "Starting PostgreSQL..."
cd "$PROJECT_ROOT"
docker compose up -d postgres

log "Waiting for PostgreSQL to be ready..."
RETRIES=30
until docker compose exec postgres pg_isready -U career_agent -q 2>/dev/null; do
    RETRIES=$((RETRIES - 1))
    if [ $RETRIES -le 0 ]; then
        err "PostgreSQL failed to start within 30 seconds"
        exit 1
    fi
    sleep 1
done
ok "PostgreSQL is ready"

# ---------------------------------------------------------------------------
# 2. Start Backend (Spring Boot)
# ---------------------------------------------------------------------------
log "Starting backend (Spring Boot)..."
cd "$PROJECT_ROOT/career-agent-service"
mvn spring-boot:run -q &
BACKEND_PID=$!
echo "$BACKEND_PID" > "$PID_DIR/backend.pid"

log "Waiting for backend to be ready..."
RETRIES=60
until curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; do
    RETRIES=$((RETRIES - 1))
    if [ $RETRIES -le 0 ]; then
        err "Backend failed to start within 60 seconds"
        exit 1
    fi
    sleep 1
done
ok "Backend is ready at http://localhost:8080"

# ---------------------------------------------------------------------------
# 3. Start Frontend (Next.js)
# ---------------------------------------------------------------------------
log "Starting frontend (Next.js)..."
cd "$PROJECT_ROOT/career-agent-ui"
pnpm dev &
FRONTEND_PID=$!
echo "$FRONTEND_PID" > "$PID_DIR/frontend.pid"

log "Waiting for frontend to be ready..."
RETRIES=30
until curl -sf http://localhost:3000 > /dev/null 2>&1; do
    RETRIES=$((RETRIES - 1))
    if [ $RETRIES -le 0 ]; then
        warn "Frontend may still be starting — check http://localhost:3000"
        break
    fi
    sleep 1
done
ok "Frontend is ready at http://localhost:3000"

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  Career Agent — All services running!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo ""
echo -e "  Frontend:       ${CYAN}http://localhost:3000${NC}"
echo -e "  Backend API:    ${CYAN}http://localhost:8080${NC}"
echo -e "  Health Check:   ${CYAN}http://localhost:8080/actuator/health${NC}"
echo -e "  Swagger UI:     ${CYAN}http://localhost:8080/swagger-ui.html${NC}"
echo -e "  PostgreSQL:     ${CYAN}localhost:5432${NC}"
echo ""
echo -e "  Stop all:       ${YELLOW}./scripts/start-local.sh --stop${NC}"
echo ""

trap stop_all EXIT INT TERM
wait
