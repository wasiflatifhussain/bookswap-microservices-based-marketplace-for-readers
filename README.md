# bookswap-microservices-based-marketplace-for-readers

Welcome to **BookSwap** – a smart trade marketplace for readers to swap novels using the novelty currency **BookCoins**.

---

## 📚 BookSwap – Feature Plan

### 1. Browsing & Catalog

- ✅ **Browse catalog:** Anyone (logged-in or not) can browse/search available books.
- ✅ **Book detail view:** Shows title, author, photos, description, current BookCoin valuation, and owner.
- ✅ **Filter/search:** By title, author, genre, valuation band, or condition _(optional for MVP)_.

### 2. User & Registration

- ✅ **User registration & login:** Required to own books or send swap requests.
- ✅ **Profile page:** Shows owned books, active requests, and swap history.

### 3. Book Registration

- ✅ **Upload a book:** Add details (title, author, year, description, optional ISBN, notes).
- ✅ **Upload photos:** Cover + condition photos (front, spine, pages).
- ✅ **AI Valuation:**
    - Condition detection (photos + metadata like “mint”, “signed”).
    - Assigns BookCoins (band or exact value).
- ✅ **Add to catalog:** Once valued, the book is visible in the universal catalog.

### 4. Swap Requests

- ✅ **Send swap request:** Offer BookCoins for someone else’s book.
- ✅ **Notification:** Target user is notified (in-app + email).
- ✅ **Accept/decline:** Target user can accept or reject.
- ✅ **On accept:**
    - Both users receive email with contact details.
    - Both books are removed from the catalog.
    - Any other pending swap requests involving those books are auto-deleted.

### 5. BookCoins System

- ✅ **Wallet:** Each user has a BookCoin balance.
- ✅ **Earn:** When you list/register a book → you receive BookCoins equal to valuation _(optional: only after a
  successful swap)_.
- ✅ **Spend:** When you swap → you pay with BookCoins.
- ✅ **Auto-updates:** Accepting/declining swaps updates BookCoin balances.

### 6. Notifications & Updates

- ✅ **In-app notifications:** For swap requests, acceptances, rejections.
- ✅ **Email notifications:** On swap acceptance → exchange contact info.

### Plan: Making the Microservices

- Plan the methods to put inside each microservice
- Where the microservice(s) stores their data
- How to implement event-driven system for microservice interactions

---

#### **Services Needed**

##### **Connectors**

- API Gateway Service
- Eureka Server Service
- Config Server Service
- Auth/User Service

##### **Domain Services**

- Catalog Service
- Media Service (for pics)
- AI Valuation Service
- Swap Service
- Wallet Service
- Notification Service

---

### **Catalog Service**

_Owns: books (business states: `DRAFT → LISTED → UNLISTED (SWAPPED OWNER_ACTION)`)_

- **POST** `/api/catalog/books`  
  Create a new book entry (state = DRAFT).  
  _Emits:_ `book.created`  
  _Returns:_ book object with `book_id`, `state`, `metadata`.

- **DELETE** `/api/catalog/books/{bookId}`  
  Unlists a book (logical delete → UNLISTED).  
  _Emits:_ `book.unlisted`  
  _Returns:_ `{ book_id, state: "UNLISTED", reason }`

- **GET** `/api/catalog/books/{bookId}`  
  Fetch one book (with media URLs + valuation if ready).  
  _Returns:_ full book detail object.

- **GET** `/api/catalog/books/user/{userId}`  
  Fetch list of books (with media URLs + valuation if ready) for that userId.  
  _Returns:_ full list of book detail objects.

- **GET** `/api/catalog/books/recent?limit=20`  
  Most recent LISTED books for homepage feed.  
  _Returns: List of book detail objects.

- **GET** `/api/catalog/books/matches?book-id={id}&tolerance=0.15`  
  Suggests books from others with similar valuation to book-id value.  
  _Returns:_ List of book detail objects.

---

### **Swap Service**

_Manages: All swap actions for the users_

- **POST** `/swap/requests`  
  Create a swap request for a target book.  
  _Body:_ `{ target_book_id, offer_coins }`  
  _Returns:_ swap object `{ swap_id, state: "PENDING", buyer_id, seller_id, target_book_id, offer_coins, created_at }`  
  _Emits:_ `swap.created`

- **POST** `/swap/requests/{swapId}/accept`  
  Owner accepts request → triggers settlement via Wallet + unlist via Catalog.  
  _Returns:_ `{ swap_id, state: "ACCEPTED" }`  
  _Emits:_ `swap.accepted { swap_id, buyer_id, seller_id, coins, target_book_id }`

- **POST** `/swap/requests/{swapId}/decline`  
  Owner declines request.  
  _Returns:_ `{ swap_id, state: "DECLINED" }`  
  _Emits:_ `swap.declined`

- **POST** `/swap/requests/{swapId}/cancel`  
  Requester cancels a still-pending request.  
  _Returns:_ `{ swap_id, state: "CANCELLED" }`  
  _Emits:_ `swap.cancelled`

- **GET** `/swap/requests/{swapId}`  
  Get one swap request.  
  _Returns:_ swap detail `{ swap_id, state, buyer_id, seller_id, target_book_id, offer_coins, created_at, updated_at }`

- **GET** `/swap/requests?mine=true&role={buyer|seller}&state={PENDING|ACCEPTED|...}&limit=&cursor=`  
  List my swaps (as buyer/seller), optionally by state.  
  _Returns:_ `{ items: [...], next_cursor }`

---

### **Media Service**

#### POST `/api/media/books/{bookId}/uploads:init`

**Initialize an upload and get a presigned S3 URL.**

**Request:**

- **Headers:**
    - `X-User-Id`: `<ownerUserId>`
- **Body:**
  ```json
  {
    "filename": "cover.jpg",
    "kind": "cover" // or "condition"
  }
  ```

**Response:**

```json
{
  "requestId": "...",
  "bookId": "...",
  "results": [
    {
      "clientRef": "...",
      "status": "READY",
      "mediaId": "...",
      "objectKey": "{bookId}/{mediaId}.jpg",
      "presignedPutUrl": "...",
      "requiredHeaders": {
        "contentType": "image/jpeg"
      },
      "expiresAt": "...",
      "errorCode": null,
      "errorMessage": null
    }
  ]
}
```

**Upload Step:**

- Make a `PUT` request to `presignedPutUrl` with the file as the body.
- Set header: `Content-Type: image/jpeg` (or as specified in `requiredHeaders`).

**S3 Storage:**

- Files are stored as:  
  `{bookId}/{mediaId}.jpg`  
  (No `books/` prefix, no mediaId folder.)

#### GET `/api/media/{mediaId}`

Fetch metadata for one uploaded image.

**Response:**

```json
{
  "media_id": "...",
  "book_id": "...",
  "owner_id": "...",
  "kind": "...",
  "url": "...",
  "thumb_url": "...",
  "created_at": "..."
}
```

#### DELETE `/api/media/{mediaId}`

Delete a media file (only by owner).

**Response:**

- `204 No Content`

**Events:**

- `media.uploaded { book_id, urls[], thumbs[] }`

---

### **Valuation Service**

_Maintain valuation and conversion rate for BookCoin to Book Price and so on; Keep this in Valuation Service as a
versioned policy._

- **POST** `/valuation/jobs`  
  Trigger (or retrigger) valuation for a book.  
  _Body:_ `{ book_id }` (service also auto-triggers on `book.created` / `media.uploaded`)  
  _Returns:_ `{ job_id, book_id, status: "QUEUED" }`  
  _Emits:_ none (job created only).

- **GET** `/valuation/books/{bookId}`  
  Fetch current valuation result (if any).  
  _Returns:_ Book valuation

- **POST** `/valuation/books/{bookId}/recompute`  
  Force a fresh run (e.g., after better photos).  
  _Returns:_ `{ job_id, book_id, status: "QUEUED" }`

- **GET** `/valuation/jobs/{jobId}`  
  Check job status.  
  _Returns:_ `{ job_id, book_id, status: "QUEUED|RUNNING|SUCCEEDED|FAILED", error? }`

---

### **Wallet Service**

_Maintain a wallet with user's BookCoin balance. When user adds a new book to his catalog, or swaps a book, the wallet
valuation is adjusted._

- **GET** `/wallet/me`  
  Get my current BookCoin balance.  
  _Returns:_ `{ user_id, available_coins, pending_coins, updated_at }`

- **GET** `/wallet/ledger?limit=&cursor=`  
  List my wallet transactions.  
  _Returns:_ `{ items: [{ ledger_id, delta_coins, balance_after, ref_type, ref_id, created_at }], next_cursor }`

- **POST** `/wallet/transfer` (admin/test only)  
  Credit/debit BookCoins manually (for seeding/testing).  
  _Body:_ `{ to_user_id, coins, reason }`  
  _Returns:_ `{ transfer_id, status: "APPLIED" }`  
  _Emits:_ `wallet.credited` or `wallet.debited`

- **(Internal) POST** `/wallet/settlements/swap`  
  Used when a swap is accepted → debit buyer, credit seller atomically.  
  _Body:_ `{ swap_id, buyer_id, seller_id, coins, request_key }`  
  _Returns:_ `{ swap_id, status: "SETTLED", debit_ledger_id, credit_ledger_id }`  
  _Emits:_ `wallet.debited`, `wallet.credited`, (optional) `swap.settled`

---

### **Notification Service**

_Owns: notifications (in-app only for MVP)_

- **GET** `/notify/stream` (WebSocket)  
  Subscribe to real-time notifications for the signed-in user.  
  _Returns:_ WS stream of messages `{ id, type, title, body, data, created_at }`

- **GET** `/notify?unreadOnly=true&limit=&cursor=`  
  Fetch my recent notifications.  
  _Returns:_ `{ items: [{ id, type, title, body, data, read, created_at }], next_cursor }`

- **POST** `/notify/read`  
  Mark notifications as read.  
  _Body:_ `{ ids: ["n1","n2"] }` or `{ all: true }`  
  _Returns:_ `{ updated: 2 }`

- **(Internal) POST** `/notify/publish`  
  Enqueue a simple in-app notification.  
  _Body:_ `{ user_id, type, title, body, data }`  
  _Returns:_ `{ id, status: "QUEUED" }`

---

### Data Stores by Service (MVP)

---

#### **Auth (Keycloak)**

- **DB:** Postgres (Keycloak’s own)
- **Stores:** Users, credentials, roles, sessions
- **Your services store:** Only `user_id` (from JWT `sub`) as foreign refs

---

#### **API Gateway / Service Registry / Config**

- **DB:** None (stateless)
- **Optional:** Redis for rate-limit counters; Git/Config-Server for configs

---

#### **Catalog Service**

- **DB:** Postgres
- **Stores:**
    - books (title, author, year, desc, isbn, owner_id)
    - state (`DRAFT` | `LISTED` | `UNLISTED`)
    - valuation snapshot (valuation_coins, confidence, policy_ver, asof)
    - media refs (media_ids/URLs)
    - basic facets (genre, condition_band, language)
- **Why SQL:** filtering, pagination, matching by valuation range

---

#### **Media Service**

- **Blob Store:** S3/MinIO bucket (actual images + thumbnails)
- **DB:** Postgres (metadata)
- **Stores:**
    - media_id, book_id, owner_id
    - kind (`COVER` | `COND`)
    - url, thumb_url, checksums, created_at

---

#### **Valuation Service**

- **DB:** None (MVP)
- **Emits:** `valuation.ready` with `{ coins, confidence, policy_ver, asof }`
- **Optional (later, Postgres):**
    - valuation_audit (inputs, outputs, model/policy versions)
    - valuation_policy (if you want versioned pricing rules)

---

#### **Swap Service**

- **DB:** Postgres
- **Stores:**
    - swaps (swap_id, buyer_id, seller_id, target_book_id, offer_coins, state `PENDING` | `ACCEPTED` | `DECLINED` |
      `CANCELLED` | `SETTLED`, timestamps, idempotency key)
- **Why SQL:** state machine integrity + history

---

#### **Wallet Service**

- **DB:** Postgres
- **Stores:**
    - wallet_balances (per user)
    - wallet_ledger (immutable double-entry with delta_coins, balance_after, ref_type/ref_id, request_key)
- **Why SQL:** atomic debit/credit, strong consistency, auditability

---

#### **Notification Service (simple in-app)**

- **DB:** Redis (AOF enabled) for fast append/read of user notification lists  
  _(Option: Postgres if you want durable history from day one)_
- **Stores:**
    - `{ id, user_id, type, title, body, data, read, created_at }`

---

### 1) Communication Style

- **Client ↔ Services:** REST (via API Gateway, OAuth2 PKCE/JWT)
- **Service ↔ Service, write-path coupling:** Kafka events (choreography/sagas)
- **Point lookups (reads):** REST (e.g., UI asks Catalog; services avoid synchronous cross-calls unless absolutely
  needed)
- **Real-time UI:** Notification service pushes over WebSocket

---

### 2) Kafka: Topics & Event Flow

- Use domain topics with typed `type` fields, or one-per-domain:
    - `catalog-events`
    - `media-events`
    - `valuation-events`
    - `swap-events`
    - `wallet-events`
    - `notification-events` (optional audit)

**All messages share an envelope (example):**

```json
{
  "id": "uuid",
  "type": "swap.accepted",
  "source": "swap-service",
  "time": "2025-09-27T04:10:00Z",
  "subject": "swap_id",
  "trace_id": "correlation-uuid",
  "data": {
    ...
    domain
    payload
    ...
  }
}
```

**Partition keys:** choose to preserve ordering per entity

- Books → `book_id`
- Swaps → `swap_id`
- Wallet ops → `user_id` (payer) or `swap_id` (settlement)

---

#### Who Publishes / Who Consumes

**Catalog Service**

- **Publishes:**
    - `book.created`, `book.listed`, `book.unlisted` → `catalog-events`
- **Consumes:**
    - `valuation.ready` (attach snapshot)
    - `media.uploaded` (attach URLs)

**Media Service**

- **Publishes:**
    - `media.uploaded { book_id, urls[] }` → `media-events`
- **Consumes:** none

**Valuation Service**

- **Publishes:**
    - `valuation.ready { book_id, coins, confidence, policy_ver }` → `valuation-events`
- **Consumes:**
    - `book.created`, `media.uploaded` (to (re)compute)

**Swap Service**

- **Publishes:**
    - `swap.created`, `swap.accepted`, `swap.declined`, `swap.cancelled` → `swap-events`
- **Consumes:**
    - (Option A) `wallet.settled` or observe `wallet.debited`/`credited` to mark SETTLED (your choice)

**Wallet Service**

- **Publishes:**
    - `wallet.debited`, `wallet.credited` (and optionally `wallet.settled { swap_id }`) → `wallet-events`
- **Consumes:**
    - `swap.accepted { swap_id, buyer_id, seller_id, coins }` → perform atomic DEBIT/CREDIT

**Notification Service (simple in-app)**

- **Publishes:** (optional audit) `notification.sent`
- **Consumes:**
    - `swap.created` (notify owner)
    - `swap.accepted` (notify both)
    - `swap.declined`/`swap.cancelled` (notify requester)
    - `book.unlisted` (optional)

---

### 3) Two Core Sagas (Choreography over Kafka)

#### **Saga A: List a Book**

1. REST Client → Catalog: `POST /catalog/books` → Catalog saves & emits `book.created`
2. Media upload → Media stores S3 & emits `media.uploaded`
3. Valuation consumes events, computes → emits `valuation.ready`
4. Catalog consumes `valuation.ready` → stores snapshot (coins/conf)
5. (Optional) Client → Catalog: `POST /catalog/books/{id}/list` → emits `book.listed`

#### **Saga B: Accept a Swap**

1. REST Client → Swap: `POST /swap/{id}/accept` → Swap checks states → emits `swap.accepted`
2. Wallet consumes `swap.accepted` → atomic debit/credit → emits `wallet.debited`/`credited` (and/or `wallet.settled`)
3. Swap observes wallet events → transitions to SETTLED
4. Catalog (either on `swap.accepted` or `wallet.settled`) unlists the book → emits `book.unlisted`
5. Notification consumes swap events → push in-app messages (WS)

---

### Event Reliability: Outbox Pattern

**TL;DR:**  
Whenever a request changes state in our DB and we need to publish an event (e.g., `book.created`), we first write the
change and an event row into the same database transaction (the outbox table). A background relay reads the outbox and
publishes to Kafka, marking rows as published.

---

#### **Why We Use Outbox**

- **Avoid dual-write bugs:** Prevents “DB committed but event lost” and “event published but DB rolled back.”
- **Reliability with retries:** The relay retries until Kafka accepts; events aren’t lost during broker outages.
- **Idempotency:** Every event has a stable `event_id`; consumers dedupe on that.
- **Operability:** Easy to inspect/rehydrate—events are queryable in SQL.

---

#### **How It Works (Brief)**

1. **In the service transaction:**  
   Persist domain change **and** insert an outbox row (`event_id`, `aggregate_type`, `aggregate_id`, `type`, `payload`,
   `occurred_at`, `published_at` = NULL).
2. **TX commits:**  
   Both records are durable.
3. **Relay process:**  
   Polls the outbox, publishes to Kafka, then sets `published_at = NOW()` (idempotent).

---

#### **Scope**

- **Enabled:** Catalog, Swap, Wallet (event-driven, cross-service side effects)
- **Optional/Deferred:** Media, Notifications, Valuation, User-Profile (can add later if/when they emit domain events)

---

### Design/Architecture for each Microservice

---

#### **Connectors**

**API Gateway Service — Layered**

- Thin routing, auth offload (Keycloak resource server), rate limiting, request logging.
- Keep controllers skinny; no domain logic.
- Add circuit breakers/timeouts on outbound calls.

**Eureka Server Service — N/A (infra component)**

- Run it as-is; no app logic.
- Externalize config only.

**Config Server Service — N/A (infra component)**

- Centralized config, profiles per env, secrets via environment/secret store.
- No business logic.

**Auth/User Service — Layered (thin)**

- If using Keycloak for identities, this holds only app-profile fields (display name, prefs).
- Controllers → Service → Repo.
- Keep it simple.
- No events for MVP (add Outbox later if it ever emits).

---

#### **Domain Services**

**Catalog Service — Layered (+ Outbox)**

- CRUD + list/detail endpoints.
- Service layer owns transactions and rules (e.g., who can edit, status changes).
- Repos for persistence and optional query-repos for complex reads.
- Write Outbox rows on create/update so downstream (Search/Notifications/Valuation) get events reliably.
- Optional short-TTL Redis cache for “browse” lists.

**Media Service (for pics) — Layered (IO-focused)**

- Controller → Service → Repo (metadata) + S3 adapter (uploads/thumbnails).
- Keep image processing async via a simple internal queue or just synchronous for MVP.
- Emit events later if needed; Outbox can be deferred.

**AI Valuation Service — Layered (stateless worker)**

- HTTP endpoint for on-demand estimate plus a background consumer if you later do async.
- Keep the “model” behind a small interface so you can swap rule-based → ML later.
- No DB needed beyond a run log.
- Outbox optional (emit valuation.completed only if others rely on it).

**Swap Service — Layered (disciplined) + Outbox**

- Impose the state machine in the service layer (Requested → Accepted → InTransit → Fulfilled/Cancelled) with guards (
  ownership, expiry).
- Persist swaps; write Outbox events (swap.requested/accepted/fulfilled) in the same tx.
- Add idempotency keys on risky mutations (accept/cancel).
- Consider a small “policy” helper class to keep transition rules tidy.

**Wallet Service — Hexagonal (Ports & Adapters) + Outbox**

- Money = correctness. Keep a pure domain: WalletAccount, LedgerEntry, Money, Hold.
- Ports: LedgerRepository, EventPublisher, IdempotencyStore, optionally Clock.
- Infra adapters: Postgres (append-only ledger), Outbox relay to Kafka, Redis (idempotency).
- Expose a read projection (wallet_balance) for quick UI.

**Notification Service — Hexagonal (event in → channels out)**

- Driving adapter: Kafka consumer (listens to catalog/swap/wallet events).
- Domain: simple routing/rate-limit policies, template selection.
- Ports: EmailSender, WsBroadcaster, TemplateRepository.
- Infra adapters implement SMTP/SendGrid, WebSocket hub, template store.
- Outbox usually not needed here (it’s a sink), but keep retries and dead-letter handling.

Note:
for keycloak, to login or register with postman,
go to postman and bookswap/keycloak folder

- we have a web-frontend keycloak client who handles login/register
- go to auth and click get new token and use those params already there
- pop-up will have both login and register features
- in our system, each microservice will call keycloak introspect to verify
- token; so we made an endpoint for it on postman
- for it, we need under auth -> basic auth -> username=client-id, password=client secret
- we made a client called bookswap-backend for this purpose
- then for this request, under body, use formdata and token = user's jwt token
- shud return 200 for success with user info
- for logout, we made a temp logout endpoint on postman
- it shud have no auth, and body shud have client_id=web-frontend
- and shud be given refresh_token for that user (NOT token)