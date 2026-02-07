# Catalog Service

The Catalog Service manages book records, their metadata, and their state in the marketplace.
It exposes REST endpoints for CRUD operations and participates in the event-driven workflow via Kafka.

The service uses **JWT-based authentication** and acts as an **OAuth2 Resource Server**,
validating **Firebase-issued JWT ID tokens locally** (no token introspection).

---

## Authentication & Security Model

* Authentication is performed using **Firebase Authentication**.
* Clients obtain a **Firebase ID token (JWT)** after signing in.
* JWTs are **validated locally** by the service using Google’s public signing keys via OIDC discovery (
  `securetoken.google.com`).
* Each request is authenticated independently (stateless, zero-trust).
* No runtime calls are made to Firebase for token validation.
* User identity is derived from the JWT `sub` claim (Firebase UID).
* Role-based authorization is currently **not enforced**; ownership checks are handled at the application/data layer.

---

## Endpoints

### POST /api/catalog/books

* Create a new book entry (initial state: DRAFT).
* **Auth:** OAuth2 Bearer JWT required.
* **Request Body:**

  ```json
  {
    "title": "Book Title",
    "author": "Author Name",
    "year": 2020,
    "description": "Description...",
    "genre": "FANTASY",
    "condition": "GOOD",
    "isbn": "optional",
    "notes": "optional"
  }
  ```
* **Response:**

  ```json
  {
    "bookId": "uuid",
    "status": "DRAFT",
    "mediaIds": [],
    "valuation": null
  }
  ```
* **Publishes Kafka Event:** `BOOK_CREATED` (topic: `catalog-events`)

---

### GET /api/catalog/books/{bookId}

* Fetch details for a single book, including media and valuation if available.
* **Auth:** OAuth2 Bearer JWT required.
* **Response:**

  ```json
  {
    "bookId": "uuid",
    "title": "...",
    "author": "...",
    "year": 2020,
    "description": "...",
    "genre": "...",
    "condition": "...",
    "status": "LISTED",
    "media": [
      {
        "mediaId": "uuid",
        "url": "https://...",
        "expiresAt": "ISO8601"
      }
    ],
    "valuation": {
      "coins": 12.5,
      "confidence": 0.95,
      "policyVersion": "v1"
    },
    "ownerId": "user-uuid"
  }
  ```

---

### DELETE /api/catalog/books/{bookId}

* Unlist (logically delete) a book.
* **Auth:** OAuth2 Bearer JWT required.
* **Response:**

  ```json
  {
    "bookId": "uuid",
    "status": "UNLISTED",
    "reason": "User deleted"
  }
  ```
* **Publishes Kafka Event:** `BOOK_UNLISTED` (topic: `catalog-events`)
* **NOTE:** This endpoint (or its corresponding logic in the Swap Service) must ensure that when a book is unlisted,
  all related swap requests are removed and any reserved funds are re-allocated to requesters.

---

### GET /api/catalog/books/user/{userId}

* List all books owned by a user.
* **Auth:** OAuth2 Bearer JWT required.
* **Response:** List of book detail objects (see above).

---

### GET /api/catalog/books/recent?limit=20

* List most recent listed books for homepage feed.
* **Auth:** Not required.
* **Response:** List of book detail objects.

---

### GET /api/catalog/books/matches?book-id={id}&tolerance=0.15

* Suggests books from others with similar valuation to the given book.
* **Auth:** Optional.
* **Response:** List of book detail objects.

---

### POST /api/catalog/books/bulk

* Bulk-fetch book details for a list of book IDs in the requested order.
* **Auth:** OAuth2 Bearer JWT required.
* **Request Body:** `{"bookIds": ["id1","id2", ...]}`
* **Response:** Ordered list of book objects with media references (used by other services to bulk-resolve books).

---

### POST /api/catalog/books/{bookId}/reserve

* Reserve a book for a pending swap (mark it temporarily RESERVED).
* **Auth:** **Service-to-service JWT** (Swap Service).
* **Behavior:** Sets the book status to RESERVED; returns Boolean success.
* **Use-case:** Prevents concurrent swap requests for the same book.

---

### POST /api/catalog/books/{bookId}/unreserve

* Unreserve a previously reserved book and make it AVAILABLE again.
* **Auth:** **Service-to-service JWT** (Swap Service).
* **Behavior:** Reverts reservation (idempotent).
* **Use-case:** Called when swap creation fails or is cancelled.

---

### POST /api/catalog/books/confirm/swap

* Confirm and finalize a swap between two books.
* **Auth:** **Service-to-service JWT** (Swap Service).
* **Parameters:** `requesterBookId`, `responderBookId`
* **Behavior:** Performs the canonical ownership/state transition for both books and returns Boolean success.
* **SLA Expectation:** Called synchronously by Swap Service with a ~10s timeout.

---

## Kafka Events

### Publishes

* `BOOK_CREATED`
  Emitted when a new book is created.
* `BOOK_MEDIA_FINALIZED`
  Emitted after media references are finalized and the book is ready for valuation.
* `BOOK_UNLISTED`
  Emitted when a book is unlisted.

### Consumes

* `MEDIA_STORED` (from `media.events`)
  Updates the book’s media list and emits `BOOK_MEDIA_FINALIZED` when complete.
* `VALUATION_READY` (from `valuation.events`)
  Updates valuation fields on the book.

---

## Notes

* All endpoints (except recent books) require authentication.
* Authentication uses **JWT validation**, not token introspection.
* Media and valuation updates are handled asynchronously via Kafka.
* The Catalog Service does not store or serve images directly; it stores media IDs and resolves signed URLs via the
  Media Service.

```bash
docker build -t bookswap-catalog:latest .

docker run -d \
--name bookswap-catalog \
--network bookswap-net \
-p 8081:8081 \
-e DB_HOST=bookswap-postgres \
-e DB_PORT=5432 \
-e DB_USERNAME=bookswap \
-e DB_PASSWORD=bookswap \
-e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
bookswap-catalog:latest

```