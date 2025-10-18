# Wallet Service API Reference

## Overview

The Wallet Service manages user wallet balances and reservations for book transactions. It provides endpoints to add to
a user's wallet, view wallet balances, and handle reservations for book swaps. All operations are atomic and use
row-level locking for consistency.

---

## HTTP Endpoints

### User-facing

- **GET /api/wallet/me/balance**
    - Returns: `{ available, reserved, total }` (resolve user_id from JWT; return zeros if missing)

### Manual/Backoffice (Testing)

- **POST /api/wallet/ops/{userId}/add**
    - Body: BookFinalizedEvent (used to credit a user's wallet when a book becomes finalized/listed) → available +=
      amount
- **POST /api/wallet/ops/{userId}/delete**
    - Body: BookUnlistedEvent (used to debit a user's wallet when a book is unlisted/deleted) → available -= amount (409
      if insufficient)

> Note: these ops are exposed under `/api/wallet/ops` and are typically used by internal consumers (Catalog/Valuation)
> or backoffice tooling.

### Swap Workflow (Direct S2S from Swap)

- **POST /api/wallet/{userId}/reserve**
    - Body: `{ amount, swapId, bookId }` -- create hold for swap
- **POST /api/wallet/{userId}/release**
    - Body: `{ amount, swapId, bookId }` -- cancel hold
- **POST /api/wallet/{userId}/requester/confirm**
    - Confirm reservation for the requester (used during swap accept flow). Accepts a WalletMutationRequest and returns
      a WalletMutationResponse. The Swap Service calls this when confirming the requester side.
- **POST /api/wallet/{userId}/responder/confirm**
    - Confirm reservation (or settlement) for the responder (used during swap accept flow). Accepts a
      WalletMutationRequest and returns a WalletMutationResponse. The Swap Service calls this when confirming the
      responder side.

Responses for mutating calls: `{ available, reserved, total, status }` (see DTOs in service for exact shapes)
Auth: user JWT for `/me/balance`; service tokens (Swap/Catalog/admin) for other endpoints.

---

## Data Models

### wallet_account

- `user_id` UUID PK
- `available` NUMERIC(18,2) NOT NULL DEFAULT 0
- `reserved` NUMERIC(18,2) NOT NULL DEFAULT 0
- `created_at` TIMESTAMPTZ DEFAULT now()
- `updated_at` TIMESTAMPTZ DEFAULT now()
- Constraint: `CHECK (available >= 0 AND reserved >= 0)`
- Note: lazily create on first write; mutate inside a transaction with row lock.

### wallet_reservation

- `reservation_id` UUID PK
- `user_id` UUID NOT NULL
- `book_id` UUID NOT NULL
- `swap_id` UUID NOT NULL
- `amount` NUMERIC(18,2) NOT NULL
- `status` TEXT NOT NULL in {ACTIVE, RELEASED, CAPTURED, EXPIRED}
- `expires_at` TIMESTAMPTZ NULL (optional)
- `created_at` / `updated_at`
- Uniques/Indexes: UNIQUE (user_id, book_id); index (user_id, status); optional (swap_id)

---

## Workflows

### Book Valuation Computed (Kafka-driven)

- Catalog/Valuation publishes the event.
- Wallet consumes (from Catalog/Valuation)
    - Topic(s): `catalog.events` BOOK_VALUATION_FINALIZED
    - Wallet consumer:
        - Upsert account if needed.
        - Transaction: lock row; available += bookCoins.

### Book Unlisted/Deleted (Kafka-driven): BOOK_UNLISTED

- Catalog publishes the event (after ensuring no active swap).
- Wallet consumer:
    - Transaction: lock row; attempt available -= bookCoins (fail if insufficient).
    - Mark event processed.

### Wallet Produces (optional; for notifications/analytics)

- Topic: `wallet.events`
    - WALLET_CREDITED { eventIdRef, userId, bookId, delta:+amount, available, reserved, at }
    - WALLET_DEBITED_BY_DELETE { ... delta:-amount ... }
    - WALLET_RESERVED / RELEASED / CAPTURED / EXPIRED with { userId, bookId, swapId, delta, available, reserved, at }

---

## Flows (Concise)

### Book Add (Valuation Computed) — Kafka-driven

- Catalog/Valuation emits BOOK_VALUATION_FINALIZED.
- Wallet consumer:
    - Upsert wallet_account (if missing).
    - Lock row; available += bookCoins.
    - Mark eventId processed.
    - (Optional) emit WALLET_CREDITED.

### Book Unlist/Delete — Kafka-driven

- Catalog emits BOOK_UNLISTED (only after it verified with Swap that book isn’t in active swap).
- Wallet consumer:
    - Lock row; attempt available -= bookCoins.
    - If < 0, reject (consumer error) → Catalog should enforce preconditions.
    - Mark eventId processed.
    - (Optional) emit WALLET_DEBITED_BY_DELETE.

### Swap (Direct HTTP)

- Reserve: Swap → `/api/wallet/{userId}/reserve` → available -= amount, reserved += amount, wallet_reservation: ACTIVE.
- Release: Swap → `/api/wallet/{userId}/release` → reserved -= amount, available += amount, reservation → RELEASED.
- Confirm (accept): Swap → `/api/wallet/{userId}/requester/confirm` and `/api/wallet/{userId}/responder/confirm` → each
  side validates and marks reservation as confirmed (final debit/credit handled as part of confirm flow / settlement
  choreography).

---

## Implementation Notes

- **No Kafka Publishers (by default):**
    - The wallet service primarily listens to events (such as book valuation or swap completion) and updates the wallet
      accordingly. It does not necessarily publish events to Kafka unless configured to do so for
      analytics/notifications.

- **Database Locking:**
    - Some wallet operations (such as reserving or releasing book coins) use row-level locking during database
      transactions. This ensures atomicity and consistency, preventing concurrent modifications that could lead to
      incorrect balances or double spending.
    - Locking is achieved using transactional annotations and, where necessary, explicit locking queries.

- **Error Handling:**
    - All service methods include exception handling to log errors and return informative responses to the frontend.
    - If a wallet row does not exist for a user, the service creates one as needed.

---

**Note:** The wallet service is designed for atomic, consistent updates to user balances. All critical operations are
wrapped in transactions to ensure data integrity.