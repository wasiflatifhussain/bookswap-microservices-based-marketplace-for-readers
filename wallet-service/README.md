# Wallet Service API Reference

## HTTP Endpoints

### User-facing

- **GET /api/wallet/me/balance**
    - Returns: `{ available, reserved, total }` (resolve user_id from JWT; return zeros if missing)

### Manual/Backoffice (Testing)

- **POST /api/wallet/{userId}/add**
    - Body: `{ amount }` → available += amount
- **POST /api/wallet/{userId}/delete**
    - Body: `{ bookId, amount }` → available -= amount (409 if insufficient)

### Swap Workflow (Direct S2S from Swap)

- **POST /api/wallet/{userId}/reserve**
    - Body: `{ amount, swapId, bookId }` -- create hold for swap
- **POST /api/wallet/{userId}/release**
    - Body: `{ amount, swapId, bookId }` -- cancel hold
- **POST /api/wallet/{userId}/capture**
    - Body: `{ amount, swapId, bookId }` -- removed: due to swap success or use deletion

Responses for mutating calls: `{ available, reserved, total, status }`
Auth: user JWT for /me/balance; service tokens (Swap/Catalog/admin) for others.

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
    - Topic(s): `catalog.events` BOOK_VALUATION_COMPUTED
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

- Catalog/Valuation emits BOOK_VALUATION_COMPUTED.
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

- Reserve: Swap → /reserve → available -= amount, reserved += amount, wallet_reservation: ACTIVE.
- Release: Swap → /release → reserved -= amount, available += amount, reservation → RELEASED.
- Capture: Swap → /capture → reserved -= amount, reservation → CAPTURED (final debit).