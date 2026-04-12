# BookSwap Frontend

Frontend web app for BookSwap, built with Next.js App Router and a BFF-first architecture.

## What This App Includes

- Authentication (login/signup) with Firebase Auth
- Global authenticated app shell with Navbar
- Home feed of available books
- Book detail page with image carousel and related matches
- Library page with create book flow and delete listing
- Swap Center with sent/received tabs and actions
- Create Swap flow with modal carousel selectors

## Tech Stack

- Next.js 16 (App Router)
- React 19 + TypeScript
- Tailwind CSS 4
- shadcn/radix UI primitives
- Firebase Authentication

## Architecture

- Frontend only talks to the BFF via `/api/bff/*`
- Next.js rewrite proxies `/api/bff/*` to `http://localhost:8080/api/bff/*` in local dev
- Business orchestration and authorization are handled downstream by BFF/microservices
- Server Components are used by default; client components only where interaction is needed

## App Routes

Public/auth routes:

- `/auth/login`
- `/auth/signup`

Authenticated app routes:

- `/` (home feed)
- `/book/[bookId]`
- `/library`
- `/library/create`
- `/swap`
- `/swap/create`

## BFF Endpoints Used by Frontend

Navbar/session:

- `GET /api/bff/navbar/snapshot`
- `GET /api/bff/navbar/notifications?unreadOnly=true`
- `POST /api/bff/navbar/notifications/read`
- `POST /api/bff/auth/login`
- `POST /api/bff/auth/logout`

Catalog/home:

- `GET /api/bff/home/feed?limit=`
- `GET /api/bff/books/get/{bookId}`
- `GET /api/bff/books/matches/{bookId}?tolerance=`

Library:

- `GET /api/bff/books/me/get`
- `DELETE /api/bff/books/me/delete/{bookId}`
- `POST /api/bff/books/create/init`
- `POST /api/bff/books/create/complete`

Swap:

- `GET /api/bff/swap/me/sent`
- `GET /api/bff/swap/me/received`
- `POST /api/bff/swap/create`
- `POST /api/bff/swap/cancel/{swapId}`
- `POST /api/bff/swap/accept/{swapId}`

## Project Structure

```text
frontend/
├─ app/
│  ├─ (app)/
│  │  ├─ page.tsx
│  │  ├─ book/[bookId]/page.tsx
│  │  ├─ library/page.tsx
│  │  ├─ library/create/page.tsx
│  │  ├─ swap/page.tsx
│  │  └─ swap/create/page.tsx
│  ├─ (auth)/auth/login/page.tsx
│  ├─ (auth)/auth/signup/page.tsx
│  └─ layout.tsx
├─ features/
│  ├─ navbar/
│  ├─ home/
│  ├─ catalog/
│  ├─ library/
│  └─ swap/
├─ components/
│  ├─ ui/
│  └─ layout/
├─ lib/
└─ next.config.ts
```

## Environment Variables

Create `frontend/.env.local`:

```env
NEXT_PUBLIC_FIREBASE_API_KEY=...
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=...
NEXT_PUBLIC_FIREBASE_PROJECT_ID=...
```

Notes:

- BFF base URL is handled through Next.js rewrite config in `next.config.ts`
- If your BFF host/port differs from `localhost:8080`, update the rewrite destination

## Run Locally

From `frontend/`:

```bash
npm install
npm run dev
```

Open:

- `http://localhost:3000`

## Build and Lint

```bash
npm run lint
npm run build
npm run start
```

## Current Product State (Implemented)

- Auth flow and session bootstrap are working
- Home, Book Detail, Library, Swap Center, and Create Swap flows are implemented
- Library supports create listing with media upload init/complete sequence
- Swap flow supports create/accept/cancel and book detail navigation from swap cards

## Known Follow-ups

- Add pagination/infinite loading for home and library feeds
- Add explicit real-time notifications (polling/WebSocket)
- Improve responsive behavior of large swap selector modals on very small screens
