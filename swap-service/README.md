## swap model

Each row in the swap table represents a single swap proposal between two users. It keeps track of who initiated the
offer, which books are involved, their respective values at the time of creation, and the current status of the swap.

**Swap Table Schema:**

- `swap_id` UUID PK
- `requester_user_id` UUID NOT NULL // the user who sent the swap offer
- `responder_user_id` UUID NOT NULL // the owner of the book being offered to (Book B)
- `requester_book_id` UUID NOT NULL // Book A, belonging to the requester
- `responder_book_id` UUID NOT NULL // Book B, belonging to the responder
- `status` TEXT NOT NULL in {PENDING, ACCEPTED, COMPLETED, REJECTED, CANCELED}
- `user_book_amount` NUMERIC(18,2) NOT NULL // snapshot of Book A’s value at the moment of reservation
- `my_book_amount` NUMERIC(18,2) NOT NULL // snapshot of Book B’s value at the same moment
- `created_at` TIMESTAMPTZ NOT NULL // creation timestamp
- `updated_at` TIMESTAMPTZ NOT NULL // last update timestamp

> (No optimistic locking required – book status updates will be handled by the Catalog Service)

---

## Endpoints

These endpoints allow users to view their outgoing and incoming swap requests and inspect all requests for a specific
book.

### List requests I sent

`GET /api/swaps/sent?userId={me}&status={PENDING}`

- Returns all swap requests created by the given user.
- Used by the requester to see all the offers they have made to other users.

### List requests made to me (on my books)

`GET /api/swaps/received?userId={me}&status={PENDING}`

- Returns all swap requests where the given user is the responder (other people want to trade for their books).

### List all requests for one of my books

`GET /api/swaps/forBook?userId={me}&bookId={myBookId}`

- Returns all swap requests targeting a specific book owned by the current user.

---

## States and Transitions (no expiry)

- `PENDING` → (responder accepts) → `ACCEPTED` → (trade done) → `COMPLETED`
- `PENDING` → (responder rejects) → `REJECTED`
- `PENDING` → (requester cancels) → `CANCELED`

Only one swap proposal for a given Book B can ever reach COMPLETED. When one swap for Book B is accepted and completed,
all other pending swaps for the same Book B must automatically be rejected.

---

## External Services and Contracts

### Wallet Service (sync)

- `POST /api/wallet/{userId}/reserve` (include swapId, bookId, amount)
- `POST /api/wallet/{userId}/confirm` (include swapId)
- `POST /api/wallet/{userId}/release` (include swapId)

Each operation is idempotent by (userId, swapId, mutationType). The wallet service is responsible for reserving,
confirming, and releasing the virtual currency linked to each swap.

### Catalog Service (sync)

- `GET /api/catalog/books/{bookId}` (validate ownership and status LISTED)
- `POST /api/catalog/books/trade` with `{ bookAId, bookBId, swapId }`
    - This endpoint atomically marks both books as SWAPPED only if they are still tradable.
    - Returns 200 on success or 409 if either book was already swapped.

---

## Swap Service Endpoints and Behavior

1. **Create Swap**
    - Called when a user proposes a swap.
    - Validates both books through the Catalog Service (GET book A and book B).
    - Calls Catalog Service to reserve the requester’s book (Book A).
    - Uses the returned book data to build a WalletMutationRequest and calls Wallet Service /reserve.
    - Stores the swap record in its own database with status PENDING.
    - Publishes a SWAP_CREATED event to Kafka for the Notification Service to consume.

2. **Accept Swap**
    - Triggered by the responder when they accept an offer.
    - Calls Catalog Service /books/trade to mark both books as SWAPPED. Catalog Service then publishes to Kafka so the
      Email Service can notify both users.
    - Calls Wallet Service /confirm for both users to finalize the fund transfer.
    - Publishes SWAP_ACCEPTED and SWAP_COMPLETED events to Kafka for notifications.
    - Rejects all other pending swaps for the same responder_book_id:
        - Calls Wallet Service /release for each rejected requester.
        - Calls Catalog Service to change those requester books back to AVAILABLE.
        - Publishes SWAP_REJECTED events to Kafka for Notification Service.

3. **Reject Swap**
    - Triggered by the responder to decline an offer.
    - Calls Wallet Service /release to free the requester’s reserved funds.
    - Calls Catalog Service to set the requester’s book back to AVAILABLE.
    - Publishes SWAP_REJECTED to Kafka for Notification Service.

4. **Cancel Swap**
    - Triggered by the requester to withdraw a pending offer.
    - Calls Catalog Service to set their book back to AVAILABLE.
    - Calls Wallet Service /release to restore the reserved balance.
    - Publishes SWAP_CANCELED to Kafka for Notification Service.