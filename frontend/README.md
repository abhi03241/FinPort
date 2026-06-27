# Artha — Frontend (React + Vite)

The modern SPA frontend for Artha. Replaces (and complements) the legacy
Thymeleaf server-rendered UI.

## Stack

- **Vite 5** — dev server + bundler
- **React 18 + TypeScript**
- **Tailwind CSS 3** — utility-first styling
- **TanStack Query v5** — server state
- **axios** — HTTP (with `withCredentials` for session cookies)
- **react-router-dom v6** — routing
- **react-hook-form + zod** (available; lightweight forms used directly here)

## Getting started

```bash
npm install
npm run dev
# → http://localhost:5173
```

The dev server proxies API + auth requests to the Spring Boot backend on
`http://localhost:8080` (configured in `vite.config.ts`).

Make sure the backend is running first:

```bash
cd ../FinPort
./mvnw spring-boot:run
```

## Scripts

| Script              | Purpose                                  |
|---------------------|------------------------------------------|
| `npm run dev`       | Vite dev server with HMR (port 5173)     |
| `npm run build`     | Type-check + production build to `dist/` |
| `npm run preview`   | Preview the production build             |
| `npm run typecheck` | TypeScript-only check (no emit)          |
| `npm run lint`      | ESLint                                   |

## Project layout

```
src/
├── main.tsx              # Entry; wraps App in providers
├── App.tsx               # Routes (public + protected)
├── index.css             # Tailwind directives
├── lib/
│   ├── api.ts            # axios instance + ApiError helper
│   ├── queryClient.ts    # TanStack Query client
│   ├── types.ts          # Shared TypeScript types
│   └── format.ts         # Currency / date formatting
├── hooks/
│   └── useAuth.tsx       # AuthProvider + useAuth()
├── components/
│   ├── Layout.tsx        # Authenticated shell (navbar + outlet)
│   └── Spinner.tsx
└── pages/
    ├── LoginPage.tsx
    ├── DashboardPage.tsx
    ├── CategoriesPage.tsx
    └── TransactionsPage.tsx
```

## Auth

The backend uses session cookies (`JSESSIONID`) via Spring Security form login.
The frontend POSTs to `/authenticateTheUser` with form-encoded credentials and
relies on the browser to store and send the cookie on subsequent requests.

Phase 7 of the Artha roadmap swaps this for JWT bearer tokens + OAuth2.

## Talking to the API

All API calls go through the shared axios instance in `src/lib/api.ts`, which:
- sets `baseURL: "/api/v1"`
- sends cookies (`withCredentials: true`)
- parses JSON

TanStack Query handles caching, invalidation and loading/error states.