# BookSwap MarketPlace

Welcome to **BookSwap** – a microservices-based marketplace where readers trade books using BookCoins.

This repository contains several domain services (Catalog, Media, Valuation, Swap, Wallet, Notification, Email, and a
BFF) plus infra connectors. Each service has its own README with endpoint and implementation details — this root README
provides a concise overview of responsibilities, the important cross-service workflows, and a compact list of the main
service endpoints with what they do.

---

## Project snapshot (short)

- Goal: Build a reliable, event-driven market for swapping books with clear ownership, valuations, and a lightweight
  BookCoin currency.
- Architecture: Small, focused microservices; Kafka used for events; Outbox pattern for reliable publishing; REST for
  point reads; WebSockets for real-time notifications.

---

## Services & responsibilities (summarized)

- **Catalog Service** — owns book records and lifecycle (draft → listed → unlisted/swapped). Stores book metadata,
  valuation snapshot, media refs, and owner info (including planned owner email field to support the Email Service).

- **Media Service** — handles image uploads (pre-signed S3 flows) and media metadata. Emits media-stored events consumed
  by Catalog and Valuation.

- **Valuation Service** — computes BookCoin valuations (rule-based or ML) and publishes valuation-ready events; Catalog
  consumes these to attach valuation snapshots. (Valuation currently implemented as a service layer worker — may not
  expose many public controllers.)

- **Swap Service** — manages swap proposals and the swap lifecycle (create, cancel, accept/confirm). Uses DB
  transactions + pessimistic locking for swap rows, coordinates Catalog and Wallet for reservations and confirmations,
  and publishes outbox events for downstream consumers.

- **Wallet Service** — manages BookCoin balances, reservations, confirmations, releases, and settlements. Exposes
  idempotent operations keyed by `(userId, swapId)`.

- **Notification Service** — in-app notification hub: consumes swap/catalog events and pushes real-time messages over
  WebSocket; keeps lightweight unread counts and short-lived notification records.

- **Email Service (planned)** — persistent email store and outbound emails: subscribes to book-finalized events to
  capture owner emails (Catalog model will include owner email), and to `SWAP_COMPLETED` events to send transactional
  emails to involved parties.

- **BFF (Backend-for-Frontend)** — a small composition layer that aggregates calls, simplifies complex client flows (
  e.g., book creation + media + valuation polling), and provides optimized payloads for the frontend.

---

## Service Endpoints (high-level)

Below is a compact list of the primary endpoints for each microservice and what they do (no request/response shapes
here — see each service README for full API contracts).

### Catalog Service (key endpoints)

- **POST /api/catalog/books**
    - Create a new book entry in DRAFT state. Triggers an outbox event `BOOK_CREATED` for downstream services (
      media/valuation).
    - Requires authentication. The service persists a snapshot of metadata and owner info.

- **GET /api/catalog/books/{bookId}**
    - Retrieve full book details (metadata, media references, valuation snapshot, owner info).
    - Used by frontend and other services to validate book state.

- **POST /api/catalog/books/{bookId}/reserve** (or similar internal reserve endpoint)
    - Mark a book as reserved (for a swap requester). Intended for coordination with Swap Service during create-swap
      flow.
    - Publishes or updates local state so other services see this temporary reservation.

- **POST /api/catalog/books/{bookId}/unreserve**
    - Revert a previous reservation (used when swap creation fails or is cancelled) and make the book AVAILABLE again.

- **POST /api/catalog/books/confirm-swap** (Catalog.confirmSwap)
    - Finalize ownership/state changes when a swap is accepted. The Swap Service calls this during accept flow and
      expects a Boolean confirmation (timeout-protected).
    - This endpoint is responsible for the canonical transfer (or unlisting) of involved book records.

> Operational note: Catalog will include owner contact (email) in the model so the Email Service can persist owner
> emails when books become finalized/listed.

### Media Service (key endpoints)

- **POST /api/media/uploads/{bookId}/init**
    - Initialize one or more media uploads and return pre-signed S3 URLs. Creates media records in PENDING state.
    - Requires authentication and validates upload metadata (mimeType, size).

- **POST /api/media/uploads/{bookId}/complete**
    - Confirms the media upload for a specific book. Marks provided media IDs as STORED and publishes a `MEDIA_STORED`
      event to Kafka so Catalog/Valuation can react.
    - Requires OAuth2 authentication.

- **GET /api/media/downloads/{bookId}/view**
    - Returns presigned URLs for viewing all stored media for a book (used by frontend to render images).

### Valuation Service (key operations)

- **(Trigger) POST /valuation/jobs** (optional)
    - Request a valuation recompute for a book. The service layer computes BookCoin valuation either synchronously or as
      a queued job and publishes `VALUATION_READY` when done.
    - In most flows Valuation is triggered by domain events (e.g., `BOOK_MEDIA_FINALIZED`) rather than explicit
      controller calls.

- **(Read) GET /valuation/books/{bookId}**
    - Fetch the latest valuation snapshot for a book (coins, confidence, policy version).

> Note: Valuation may primarily be a worker/service-layer consumer of events (no heavy public controller surface) — the
> important detail is it emits `VALUATION_READY` for Catalog to persist a snapshot.

### Swap Service (key endpoints)

- **POST /swap/requests**
    - Create a swap request for a target book. Coordinates with Catalog to validate/ reserve the requester book and with
      Wallet to reserve requester funds (idempotent by userId+swapId). Persists a PENDING swap and enqueues a
      `SWAP_CREATED` outbox event.

- **POST /swap/requests/{swapId}/accept**
    - Accept an existing swap. This is the critical accept/confirm workflow (see the canonical workflow section above):
      DB locking (FOR UPDATE), Catalog.confirmSwap, Wallet confirm calls for requester/responder, cancel other pending
      swaps for responder book, delete accepted swap row, and publish `SWAP_COMPLETED` event via outbox.

- **POST /swap/requests/{swapId}/decline**
    - Responder rejects the offer. Releases reservations: call Wallet `/release` for requester, unreserve Catalog for
      requester book, delete swap row, and publish `SWAP_REJECTED`/`SWAP_CANCELLED` events for notifications.

- **POST /swap/requests/{swapId}/cancel**
    - Requester cancels their pending offer. Validates requester identity, releases wallet reservation, unreserves
      Catalog, deletes swap row, and publishes cancellation event (idempotent-safe).

- **GET /swap/requests/{swapId}** and listing endpoints
    - Read endpoints for UI: list sent/received requests and all requests for a book. These endpoints aggregate Catalog
      book details (bulk calls) so the UI gets full book context.

### Wallet Service (key endpoints)

- **POST /api/wallet/{userId}/reserve**
    - Reserve funds for a swap (include swapId/bookId/amount). Idempotent by `(userId, swapId)`.

- **POST /api/wallet/{userId}/confirm**
    - Confirm a previously reserved amount (used when swap accept is in progress).

- **POST /api/wallet/{userId}/release**
    - Release previously reserved funds (used on cancel/failure). Idempotent by `(userId, swapId)`.

- **(Internal) POST /wallet/settlements/swap**
    - Settle a completed swap atomically (debit buyer, credit seller) — may be invoked by Wallet when observing
      `swap.accepted` or by Swap as part of a settlement choreography.

### Notification Service (key endpoints & behavior)

- **GET /notify/stream** (WebSocket)
    - Subscribe to real-time notifications for the signed-in user; the service pushes events consumed from Kafka (
      swap/catalog events) to connected clients.

- **GET /notify?unreadOnly=true&limit=&cursor=**
    - Fetch recent notifications and unread counts.

- **POST /notify/read**
    - Mark provided notification IDs (or all) as read.

- **(Internal) POST /notify/publish**
    - Enqueue an in-app notification (used by internal processes or for testing).

Behavior: Notification Service consumes `SWAP_CREATED`, `SWAP_CANCELLED`, `SWAP_COMPLETED`, and `BOOK_UNLISTED` (as
needed) and translates those into short-lived, user-targeted WebSocket messages and lightweight DB entries for unread
counts.

### Email Service (planned; key behavior)

The Email Service is a dedicated consumer and outbound sender.

- Responsibilities:
    - Consume `BOOK_MEDIA_FINALIZED` / `BOOK_LISTED` (or similar finalization events) and persist the owner's email when
      a book reaches a finalized/listed state. This requires the Catalog model to include an owner email field.
    - Consume `SWAP_COMPLETED` events and send transactional emails to both parties involved in the swap with contact
      details and next steps. Ensure idempotent sending (dedupe on `event_id`).
    - Provide retry and dead-letter handling for delivery failures.

(Planned endpoints for administration / manual resend):

- **GET /emails/user/{userId}** — view persisted email addresses or email-send history.
- **POST /emails/send** — manual/administration send (optional).

### BFF (Backend-for-Frontend) endpoints (examples)

- **POST /bff/books/create**
    - Orchestrates the full book-creation UX: calls Catalog create, starts media upload flow (Media init), and
      optionally polls Valuation until a snapshot is available, returning a single composed payload the UI can use to
      show progress.

- **GET /bff/books/{bookId}/detail**
    - Aggregates Catalog, Media, Valuation, and short notification state into a single optimized payload for the
      frontend.

- **GET /bff/swaps?mine=true**
    - Combines Swap + Catalog + Media (via Catalog bulk endpoints) so the UI shows enriched swap lists with book
      thumbnails and valuations.

---

## Accept Swap — canonical workflow (implementation-accurate)

This is the authoritative workflow for how the Swap Service completes an accepted swap. See `swap-service/README.md` and
`swap-service/src/.../SwapService.java` for full implementation details.

1. **Lock & validate**
    - The responder triggers accept. Swap Service retrieves the swap row with a `FOR UPDATE` read (
      `findBySwapIdForUpdate`) to avoid concurrent modifications and validates the responder identity and PENDING state.

2. **Catalog confirmation**
    - Swap Service calls Catalog's `confirmSwap(requesterBookId, responderBookId)` and waits (up to 10s). This call
      finalizes ownership/state changes required on the Catalog side; failure aborts accept.

3. **Wallet confirmations**
    - Swap Service calls Wallet confirm operations for both the requester and the responder (separate calls, each
      blocked up to 10s). The service validates returned responses (message contains "confirmed", case-insensitive). Any
      failure aborts accept.

4. **Cancel other pending swaps**
    - For the responder book (Book B) all other pending swaps are cancelled: for each pending swap, Swap Service
      performs best-effort compensations — release wallet reservations for the requesters, unreserve their requester
      books in Catalog, hard-delete those swap rows, and publish SWAP_CANCELLED events for notifications.

5. **Finalize accepted swap**
    - The accepted swap row is deleted (hard delete) from the Swap DB instead of being transitioned to an
      ACCEPTED/COMPLETED state.
    - Swap Service writes an outbox `SWAP_COMPLETED` event for downstream consumers (Notification, Email, Catalog as
      applicable).

6. **Observability & reliability notes**
    - Local DB operations are transactional and use pessimistic locking. External calls (Catalog, Wallet) are performed
      outside the DB transaction and the service relies on best-effort compensations and idempotency guarantees from
      downstream services.
    - Timeouts are enforced (Catalog/Wallet confirmations use 10s blocking calls). The system relies on the outbox
      pattern for reliable event publishing to Kafka.

---

## Eventing & outbox (brief)

- Domain events (catalog, valuation, swap, wallet) are published via an outbox table written inside the service
  transaction. A relay publishes outbox rows to Kafka and marks them as published.
- Partition keys are chosen to preserve entity ordering: book_id for book events, swap_id for swap events, and
  user_id/swap_id for wallet events.

---

## Dev notes & next steps

- Implement the Email Service to persist owner emails (Catalog model update) and send `SWAP_COMPLETED` emails.
- Build a Notification consumer that reads Swap outbox events and pushes WebSocket notifications.
- Add more E2E tests for swap accept/cancel flows (cover happy & failure compensation paths).
- Deploy plan: containerize services and publish with Kubernetes on AWS (planned for production readiness).

---

For more detail about a specific service, open its README in the corresponding subfolder — they contain API examples,
sequence diagrams, and implementation notes.