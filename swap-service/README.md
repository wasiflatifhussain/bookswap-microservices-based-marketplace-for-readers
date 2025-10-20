# Swap Service

## Data Model

Each row in the swap table represents a single swap proposal between two users. It keeps track of who initiated the
offer, which books are involved, their respective values at the time of creation, and the current status of the swap.

### Swap Table Schema

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

## API Endpoints

These endpoints allow users to view their outgoing and incoming swap requests and inspect all requests for a specific
book.

### List Requests I Sent

```
GET /api/swaps/sent?userId={me}&status={PENDING}
```

- Returns all swap requests created by the given user
- Used by the requester to see all the offers they have made to other users

**Implementation Note**: The endpoint retrieves all swap IDs for a user and makes bulk calls to the catalog service to
get all the requested books - including ID, title, description, primary media, author, and book condition. It returns
both the user's book and the books they requested so the frontend gets complete information.

**Note: When the frontend calls this API via BFF, the BFF will get these values and then call the media service to get
presigned URLs for thumbnails. This maintains separation of concerns and reduces tight coupling.**

### List Requests Made to Me

```
GET /api/swaps/received?userId={me}&status={PENDING}
```

- Returns all swap requests where the given user is the responder (other people want to trade for their books)

### List All Requests for One of My Books

```
GET /api/swaps/forBook?userId={me}&bookId={myBookId}
```

- Returns all swap requests targeting a specific book owned by the current user

---

## State Transitions

The swap lifecycle follows these transitions:

- `PENDING` → (responder accepts) → `ACCEPTED` → (trade done) → `COMPLETED`
- `PENDING` → (responder rejects) → `REJECTED`
- `PENDING` → (requester cancels) → `CANCELED`

**Important**: Only one swap proposal for a given Book B can ever reach COMPLETED. When one swap for Book B is accepted
and completed, all other pending swaps for the same Book B must automatically be rejected.

---

## External Service Integrations

### Wallet Service (Synchronous)

| Endpoint                            | Description                                               |
|-------------------------------------|-----------------------------------------------------------|
| `POST /api/wallet/{userId}/reserve` | Reserve funds for a swap (include swapId, bookId, amount) |
| `POST /api/wallet/{userId}/confirm` | Confirm a previously reserved amount (include swapId)     |
| `POST /api/wallet/{userId}/release` | Release previously reserved funds (include swapId)        |

Each operation is idempotent by (userId, swapId, mutationType). The wallet service is responsible for reserving,
confirming, and releasing the virtual currency linked to each swap.

## Core Operations

### 1. Create Swap

- Called when a user proposes a swap
- Implementation sequence:
    1. Fetch and validate both books through the Catalog Service (GET book A and book B) and ensure they are AVAILABLE.
    2. Call Catalog Service to reserve the requester's book (Book A).
    3. Persist the swap row in the Swap DB with status PENDING (snapshot pricing is stored).
    4. Call Wallet Service to reserve the requester's funds using the saved swapId (idempotent by userId+swapId).
        - If the Wallet reservation fails, the service performs best-effort rollback: unreserve the Catalog reservation
          and delete the swap row.
    5. Publish a `SWAP_CREATED` event to Kafka (outbox) for downstream notification processing.

**Transaction Handling**: While the local database operations are protected by `@Transactional`, external API calls are
not covered by this mechanism. If any external API call fails during the create swap process, the service performs
explicit rollback operations for any successful API calls that were already made. This ensures data consistency across
microservices even when some operations fail.

### 2. Accept Swap

- Triggered by the responder when they accept an offer.
- Implementation sequence (reflects actual code in `SwapService.acceptSwapRequest`):
    1. Acquire a DB lock on the swap row using a `FOR UPDATE` read (`findBySwapIdForUpdate`) to prevent concurrent
       modifications.
    2. Verify the request's `responderUserId` matches the swap row; reject otherwise.
    3. Call Catalog Service to confirm the trade (`confirmSwap(requesterBookId, responderBookId)`) and block up to 10s
       for a Boolean response. Failure to confirm causes the operation to abort.
    4. Call Wallet Service to confirm the previously reserved amounts for both users:
        - Confirm for the requester via `confirmSwapSuccessForRequester(...)`
        - Confirm for the responder via `confirmSwapSuccessForResponder(...)`
        - Each call is blocked up to 10s and validated by checking the returned message contains `"confirmed"` (
          case-insensitive).
          Failures abort the operation.
    5. Cancel all other pending swaps that target the same responder book:
        - For each pending swap, call Wallet Service `release` for the requester (idempotent by `(userId, swapId)`),
          call Catalog Service to unreserve the requester book (set back to AVAILABLE), delete the swap row, and publish
          a cancellation event for notifications.
    6. Delete the accepted swap row from the Swap DB (the service deletes the row instead of updating it to
       ACCEPTED/COMPLETED).
    7. Publish a `SWAP_COMPLETED` (outbox) event so downstream notification/email services can notify both parties.

- Transactional and error handling notes:
    - The method is annotated with `@Transactional` for local DB operations, and the service uses pessimistic locking
      for safety.
    - External calls (Catalog / Wallet) are not covered by the DB transaction. The implementation performs best-effort
      and explicit checks; failures throw exceptions and are propagated.
    - Timeouts used in code: Catalog and Wallet confirmation calls block with a 10-second timeout.
    - Wallet and Catalog operations are assumed idempotent; Wallet operations are idempotent by `(userId, swapId)` per
      service contract.
    - The service relies on outbox events for reliable notification publishing after the DB changes.

### 3. Reject Swap

- Triggered by the responder to decline an offer
- Implementation sequence:
    1. Calls Wallet Service `/release` to free the requester's reserved funds
    2. Calls Catalog Service to set the requester's book back to AVAILABLE
    3. Publishes `SWAP_REJECTED` to Kafka for Notification Service

### 4. Cancel Swap

- Triggered by the requester to withdraw a pending offer
- Implementation sequence (matches actual implementation):
    1. Acquire a DB lock on the swap row using `findBySwapIdForUpdate` to ensure no concurrent modifications.
    2. State guard: only allow cancel when swap status is `PENDING`.
    3. If a requesterUserId is provided in the request, validate it matches the swap row; only the requester may cancel.
    4. Call Catalog Service to set their book back to AVAILABLE (best-effort, idempotent).
    5. Call Wallet Service `/release` to restore the reserved balance (best-effort, idempotent by userId+swapId).
    6. Delete the swap row from the DB (hard delete) and publish a `SWAP_CANCELED` event to Kafka (outbox) for
       notifications.

---

## Service-to-Service Authentication

The Swap Service performs S2S communication with other domain services like Catalog, Wallet, and others.
It uses the service-service-comms client in Keycloak, which acts as a Service Roles client for authenticated internal
service calls.

### Configuration

To allow Swap to read or write to other services:

1. Go to the target service's client in Keycloak (e.g. bookswap-api for Catalog)
2. Under Service Account Roles, grant the service-service-comms client the required roles:
    - `catalog.read` for read access
    - `catalog.write` for write access
    - (and similarly for `wallet.read` / `wallet.write` / `wallet.delete` when connecting to Wallet)
3. On the service-service-comms client, create an Audience mapper and include the target client (e.g. bookswap-api) so
   that it appears under the `aud` claim in the token
4. Keep "Add to access token" and "Add to token introspection" enabled

Once this setup is done, Swap can securely communicate with other microservices without manual token passing or role
checks in code — permissions are handled entirely through Keycloak service roles and audience configuration.

## VIP Improvements needed in the future:
- P1 Task: When a responder unlists a books, then it posts BOOK_UNLISTED event. We need to
  automatically cancel all pending swaps for that book and notify the requesters. Swap Service can listen to the event and
  perform the cancellations and post SWAP_CANCELED events for notifications. 
- P1 Incident: We require rollbacks in service layer. As we are calling multiple external services, if any of them fail,
  we need to rollback the previous successful calls. Current use of @Transactional only covers local DB operations. We
  need to either implement a saga pattern with Kafka or retry mechanisms with idempotency keys. The main idea is that if
  any
  step fails, we can revert the previous steps to maintain data consistency across services.
- P2 Incident: Another issue: In some places we throw errors while in others we return messages. We should standardize
  this to always throw exceptions for error handling, which can then be caught and processed by a global exception
  handler to return consistent error responses to clients. THIS NEEDS TO EXTEND TO ALL MICROSERVICES.