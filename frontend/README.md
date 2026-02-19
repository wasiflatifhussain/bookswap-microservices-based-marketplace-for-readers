# BookSwap Frontend

This is the **frontend application** for **BookSwap**, a microservices-based marketplace for trading books.

The frontend is intentionally designed to be **lean and production-focused**, delegating all business logic, orchestration, and security to a **Backend-for-Frontend (BFF)** layer.

---

## Tech Stack

- **Next.js (App Router)**
- **TypeScript**
- **Tailwind CSS**
- **shadcn/ui** (Radix-based UI primitives)
- **Firebase Authentication**
- **BFF-based API communication**

---

## Core Design Principles

- **Frontend stays dumb**
  No direct microservice calls. No orchestration logic.

- **BFF-first architecture**
  All API calls go through `/api/bff/*`.

- **Server Components by default**
  Client Components are used only where interactivity is required.

- **Feature-based structure**
  Each domain owns its UI, server calls, and types.

---

## Folder Structure

```bash
bookswap-web/
├── app/
│   ├── (public)/
│   │   └── page.tsx                    # Home (feed)
│   │
│   ├── library/
│   │   └── page.tsx                    # My Library
│   │
│   ├── book/
│   │   └── [bookId]/
│   │       └── page.tsx                # Book detail page
│   │
│   ├── swap/
│   │   ├── page.tsx                    # Swap Center (sent / received)
│   │   └── create/
│   │       └── page.tsx                # Create swap flow
│   │
│   ├── layout.tsx                      # App shell (Navbar)
│   └── globals.css
│
├── features/
│   ├── navbar/
│   │   ├── components/
│   │   │   ├── Navbar.tsx
│   │   │   ├── NotificationBell.tsx
│   │   │   └── NotificationList.tsx
│   │   ├── server/
│   │   │   └── navbar.api.ts
│   │   └── types.ts
│   │
│   ├── home/
│   │   ├── components/
│   │   │   └── HomeFeed.tsx
│   │   ├── server/
│   │   │   └── home.api.ts
│   │   └── types.ts
│   │
│   ├── catalog/
│   │   ├── components/
│   │   │   ├── BookCard.tsx
│   │   │   ├── BookList.tsx
│   │   │   ├── BookCarousel.tsx
│   │   │   └── BookActions.tsx
│   │   ├── server/
│   │   │   └── catalog.api.ts
│   │   └── types.ts
│   │
│   ├── library/
│   │   ├── components/
│   │   │   └── MyBooksSection.tsx
│   │   ├── server/
│   │   │   └── library.api.ts
│   │   └── types.ts
│   │
│   ├── swap/
│   │   ├── components/
│   │   │   ├── SwapCard.tsx
│   │   │   ├── SwapList.tsx
│   │   │   ├── SwapTabs.tsx
│   │   │   └── SwapBookSelector.tsx
│   │   ├── server/
│   │   │   └── swap.api.ts
│   │   └── types.ts
│   │
│   ├── wallet/
│   │   ├── components/
│   │   │   └── WalletBalance.tsx
│   │   ├── server/
│   │   │   └── wallet.api.ts
│   │   └── types.ts
│
├── components/
│   ├── ui/                             # shadcn only
│   │   ├── button.tsx
│   │   ├── card.tsx
│   │   ├── dialog.tsx
│   │   ├── badge.tsx
│   │   └── carousel.tsx
│   │
│   └── layout/
│       └── PageContainer.tsx
│
├── lib/
│   ├── bff-client.ts
│   ├── fetcher.ts
│   └── auth.ts
│
├── middleware.ts
├── tailwind.config.ts
└── package.json

```

---

## Authentication Model

- **Firebase Authentication** is used for user login.
- Firebase issues an **ID token (JWT)**.
- The token is forwarded to the **BFF**, which:
  - validates it
  - derives user identity
  - enforces authorization downstream

- The frontend **never validates tokens** itself.

Auth state is used only for:

- rendering user info in the Navbar
- protecting routes via middleware

---

## Environment Variables

Create a `.env.local` file:

```env
NEXT_PUBLIC_BFF_URL=http://localhost:8080/api/bff
```

This file is ignored by Git.

---

## Frontend Development Roadmap

The frontend is built **incrementally**, validating architecture at each step.

---

### Phase 1 — Infrastructure (Done)

- [x] Next.js App Router setup
- [x] Tailwind CSS
- [x] shadcn/ui initialization
- [x] Feature-based folder structure
- [x] Central BFF fetch wrapper
- [x] Environment configuration

---

### Phase 2 — Navbar & Session (Next)

**Endpoints**

- `GET /navbar/snapshot`
- `GET /navbar/notifications`
- `POST /navbar/notifications/read`

**Deliverables**

- Global Navbar in `app/layout.tsx`
- User email + wallet balance
- Notification bell with unread count
- Notification list (dropdown / sheet)
- Mark notifications as read when opened

This phase validates:

- BFF connectivity
- auth token forwarding
- Server vs Client component boundaries

---

### Phase 3 — Home Feed

**Endpoint**

- `GET /home/feed`

**Deliverables**

- Home feed page
- `BookCard` + `BookList` components
- Server-rendered book list

---

### Phase 4 — Book Detail Page

**Endpoints**

- `GET /books/get/{bookId}`
- `GET /books/matches/{bookId}`

**Deliverables**

- Book detail page
- Image carousel
- Book metadata
- Context-aware actions (swap / delete if owner)
- Related book matches

---

### Phase 5 — My Library

**Endpoints**

- `GET /books/me/get`
- `DELETE /books/me/delete/{bookId}`
- Create book flow (`init` → `complete`)

**Deliverables**

- My Library page
- User-owned book list
- Delete / unlist actions
- Reuse of `BookCard` components

---

### Phase 6 — Swap Center

**Endpoints**

- `GET /swap/me/sent`
- `GET /swap/me/received`
- `GET /swap/book/{bookId}/requests`

**Deliverables**

- Swap Center page
- Sent / Received tabs
- Swap request cards
- Status-based actions (accept / cancel)

---

### Phase 7 — Create Swap Flow

**Endpoint**

- `POST /swap/create`

**Deliverables**

- Dedicated create-swap page
- Clear visual separation:
  - requested book (target)
  - offered book (user selection)

- Book selector (carousel-style)
- Final confirmation step

### Phase 8 — Polling/WebSockets for Notifications

---

## Running the App

```bash
npm run dev
```

Visit:
[http://localhost:3000](http://localhost:3000)

---

### Important Future Updates

- Add pagination to home feed and library pages - fetch incrementally - might require BFF support
