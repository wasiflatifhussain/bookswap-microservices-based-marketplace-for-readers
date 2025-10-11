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
    1. Validates both books through the Catalog Service (GET book A and book B)
        - Ensures that both books are "AVAILABLE"
    2. Calls Catalog Service to reserve the requester's book (Book A)
    3. Uses the returned book data to build a WalletMutationRequest and calls Wallet Service /reserve
    4. Stores the swap record in its own database with status PENDING
    5. Publishes a SWAP_CREATED event to Kafka for the Notification Service to consume

**Transaction Handling**: While the local database operations are protected by `@Transactional`, external API calls are
not covered by this mechanism. If any external API call fails during the create swap process, the service performs
explicit rollback operations for any successful API calls that were already made. This ensures data consistency across
microservices even when some operations fail.

### 2. Accept Swap

- Triggered by the responder when they accept an offer
- Implementation sequence:
    1. Calls Catalog Service /books/trade to mark both books as SWAPPED
        - Catalog Service then publishes to Kafka so the Email Service can notify both users
    2. Calls Wallet Service /confirm for both users to finalize the fund transfer
    3. Publishes SWAP_ACCEPTED and SWAP_COMPLETED events to Kafka for notifications
    4. Rejects all other pending swaps for the same responder_book_id:
        - Calls Wallet Service /release for each rejected requester
        - Calls Catalog Service to change those requester books back to AVAILABLE
        - Publishes SWAP_REJECTED events to Kafka for Notification Service

### 3. Reject Swap

- Triggered by the responder to decline an offer
- Implementation sequence:
    1. Calls Wallet Service /release to free the requester's reserved funds
    2. Calls Catalog Service to set the requester's book back to AVAILABLE
    3. Publishes SWAP_REJECTED to Kafka for Notification Service

### 4. Cancel Swap

- Triggered by the requester to withdraw a pending offer
- Implementation sequence:
    1. Calls Catalog Service to set their book back to AVAILABLE
    2. Calls Wallet Service /release to restore the reserved balance
    3. Publishes SWAP_CANCELED to Kafka for Notification Service

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
    - (and similarly for `wallet.read` / `wallet.write` when connecting to Wallet)
3. On the service-service-comms client, create an Audience mapper and include the target client (e.g. bookswap-api) so
   that it appears under the `aud` claim in the token
4. Keep "Add to access token" and "Add to token introspection" enabled

Once this setup is done, Swap can securely communicate with other microservices without manual token passing or role
checks in code — permissions are handled entirely through Keycloak service roles and audience configuration.