# Career Agent UI

Next.js frontend for the Career Agent — an AI-powered job search assistant dashboard.

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Next.js | 16 | React framework (App Router) |
| React | 19 | UI library |
| TypeScript | 5.x | Type safety |
| Material UI (MUI) | 9.x | Component library |
| Emotion | 11.x | CSS-in-JS (MUI styling engine) |
| React Query | 5.x | Server state management |
| Zustand | 5.x | Client state management |
| Axios | 1.x | HTTP client |
| Zod | 4.x | Schema validation |

## Prerequisites

- Node.js 22+
- pnpm 11+

## Getting Started

### 1. Install dependencies

```bash
pnpm install
```

### 2. Configure environment

```bash
# .env.local (already created)
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 3. Start development server

```bash
pnpm dev
```

Open [http://localhost:3000](http://localhost:3000).

## Project Structure

```
src/
├── app/
│   ├── layout.tsx                # Root layout (MUI providers)
│   ├── page.tsx                  # Root redirect (auth check)
│   ├── globals.css               # Minimal CSS reset
│   ├── (auth)/
│   │   ├── layout.tsx            # Centered auth layout
│   │   ├── login/page.tsx        # Login form
│   │   └── register/page.tsx     # Registration form
│   ├── (dashboard)/
│   │   ├── layout.tsx            # AppBar + Drawer + auth guard
│   │   ├── loading.tsx           # Route-level loading skeleton
│   │   ├── error.tsx             # Error boundary (uses retry)
│   │   ├── dashboard/page.tsx    # Dashboard home (Server Component)
│   │   ├── jobs/
│   │   │   ├── page.tsx          # Jobs list with table, pagination, filters
│   │   │   ├── loading.tsx       # Jobs loading skeleton
│   │   │   └── [id]/
│   │   │       ├── page.tsx      # Job detail view
│   │   │       └── loading.tsx   # Job detail loading skeleton
│   │   └── profile/page.tsx      # Profile management (3 tabs)
│   └── api/
│       └── health/route.ts       # Health endpoint (Web API Response.json)
├── components/                   # Reusable MUI components
├── hooks/
│   ├── useProfile.ts             # Profile/preferences/document hooks
│   └── useJobs.ts                # Job list/detail/ingestion hooks
├── lib/
│   ├── api.ts                    # Axios instance (JWT interceptor)
│   ├── providers.tsx             # MUI + React Query providers
│   ├── theme.ts                  # MUI theme configuration
│   ├── errorUtils.ts             # Shared error message extraction
│   └── jobConstants.ts           # Job status colors and status list
├── store/
│   └── authStore.ts              # Zustand auth state
└── types/
    ├── auth.ts                   # Auth response/error types
    ├── common.ts                 # Shared API error types
    ├── profile.ts                # Profile/preference/document types
    └── job.ts                    # Job/ingestion response types
```

## Available Scripts

| Command | Description |
|---|---|
| `pnpm dev` | Start dev server (http://localhost:3000) |
| `pnpm build` | Production build |
| `pnpm start` | Start production server |
| `pnpm lint` | Run ESLint |

## UI Framework

This project uses **Material UI (MUI) v9** with Emotion CSS-in-JS. No Tailwind CSS.

- Theme: `src/lib/theme.ts`
- Provider: `AppRouterCacheProvider` from `@mui/material-nextjs` for SSR
- Icons: `@mui/icons-material`
- Styling: MUI `sx` prop (no className/Tailwind utilities)

## Authentication Flow

1. User registers or logs in → backend returns JWT token
2. Token stored in Zustand (in-memory, not localStorage)
3. Axios interceptor attaches `Authorization: Bearer <token>` to every API request
4. 401 response → auto-logout → redirect to `/login`
5. Dashboard layout checks `isAuthenticated` → redirects to `/login` if false

## Testing

```bash
# Run tests
pnpm vitest

# Run with coverage
pnpm vitest --coverage
```

| Type | Framework |
|---|---|
| Component tests | Vitest + Testing Library |
| Property-based | fast-check |

## Docker

```bash
# Build the image
docker build -t career-agent-ui .

# Run via Docker Compose (from project root)
docker compose up career-agent-ui
```
