<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

# Career Agent UI — Development Rules

These rules apply to ALL code changes in the frontend. Follow them in every session without exception.

## UI Framework Rules

1. **Use Material UI (MUI) v9** for all components. No Tailwind CSS, no custom CSS classes.

2. **Use tree-shaking imports:** `import Button from "@mui/material/Button"` — NOT `import { Button } from "@mui/material"`.

3. **Use `sx` prop** for all styling. No `className` with utility classes.

4. **Use `@mui/icons-material`** for icons. No lucide-react, no heroicons.

5. **Use `AppRouterCacheProvider`** from `@mui/material-nextjs` in providers for SSR compatibility.

## State Management Rules

6. **Zustand** for client state (auth token, UI preferences). Store in `src/store/`.

7. **React Query (`@tanstack/react-query`)** for all server state (API data). Never use `useEffect` + `fetch` for API calls.

8. **Custom hooks** for API operations go in `src/hooks/`. Each hook wraps `useQuery` or `useMutation`.

9. **Invalidate queries** after successful mutations to keep the UI in sync.

## API Rules

10. **Use the Axios instance** from `src/lib/api.ts` for ALL API calls. Never create new Axios instances.

11. **JWT token** is attached automatically by the Axios request interceptor. Do not manually add auth headers.

12. **401 responses** trigger automatic logout and redirect to `/login` via the response interceptor.

## TypeScript Rules

13. **Strict TypeScript** — no `any` types. Define interfaces for all API responses in `src/types/`.

14. **Use Zod** for runtime validation of API responses where needed.

## Component Rules

15. **Mark all pages with hooks as `"use client"`** — Next.js App Router requires this for client components.

16. **Disable `prefetch`** on nav links for routes that don't exist yet: `prefetch={false}`.

17. **Show loading states** using MUI `CircularProgress` or `Skeleton` while data loads.

18. **Show errors** using MUI `Alert` or `Snackbar` components.

19. **Use MUI `TextField`** for form inputs, **`Select`** for dropdowns, **`Autocomplete`** with `freeSolo` for tag/chip inputs.

## File Structure

```
src/
├── app/              # Next.js App Router pages and layouts
│   ├── (auth)/       # Login, register (public routes)
│   ├── (dashboard)/  # Protected routes with sidebar layout
│   └── api/          # API routes (health, etc.)
├── components/       # Reusable MUI components
├── hooks/            # Custom React Query hooks
├── lib/              # API client, theme, providers
├── store/            # Zustand stores
└── types/            # TypeScript interfaces
```

## Package Manager

20. **Use `pnpm`** for all package operations. Never use npm or yarn.

## Testing Rules

21. **Vitest** for unit/component tests. **fast-check** for property-based tests.

22. **React Testing Library** for component tests. Test behavior, not implementation.

## Git Rules

23. **Provide a GitHub commit message** at the end of every session.

24. **Run `pnpm build`** after every change to verify TypeScript compilation and Next.js build passes.
