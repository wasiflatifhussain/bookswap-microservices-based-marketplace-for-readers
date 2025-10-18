# Catalog Service

The Catalog Service manages book records, their metadata, and their state in the marketplace. It exposes REST endpoints
for CRUD operations and participates in the event-driven workflow via Kafka.

## Endpoints

### POST /api/catalog/books

- Create a new book entry (initial state: DRAFT).
- **Auth:** OAuth2 Bearer Token required.
- **Request Body:**
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
- **Response:**
  ```json
  {
    "bookId": "uuid",
    "status": "DRAFT",
    "mediaIds": [],
    "valuation": null
  }
  ```
- **Publishes Kafka Event:** `BOOK_CREATED` (topic: `catalog-events`)

---

### GET /api/catalog/books/{bookId}

- Fetch details for a single book, including media and valuation if available.
- **Auth:** OAuth2 Bearer Token required.
- **Response:**
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

- Unlist (logically delete) a book.
- **Auth:** OAuth2 Bearer Token required.
- **Response:**
  ```json
  {
    "bookId": "uuid",
    "status": "UNLISTED",
    "reason": "User deleted"
  }
  ```
- **Publishes Kafka Event:** `BOOK_UNLISTED` (topic: `catalog-events`)

---

### GET /api/catalog/books/user/{userId}

- List all books owned by a user.
- **Auth:** OAuth2 Bearer Token required.
- **Response:** List of book detail objects (see above).

---

### GET /api/catalog/books/recent?limit=20

- List most recent listed books for homepage feed.
- **Auth:** Not required.
- **Response:** List of book detail objects.

---

### GET /api/catalog/books/matches?book-id={id}&tolerance=0.15

- Suggests books from others with similar valuation to book-id value.
- **Auth:** Optional.
- **Response:** List of book detail objects.

---

### POST /api/catalog/books/bulk

- Bulk-fetch book details for a list of book IDs in the requested order.
- **Auth:** OAuth2 Bearer Token required.
- **Request Body:** `{"bookIds": ["id1","id2", ...]}`
- **Response:** Ordered list of book objects with media references (used by other services to bulk-resolve books).

---

### POST /api/catalog/books/{bookId}/reserve

- Reserve a book for a pending swap (mark it temporarily RESERVED so others cannot request it).
- **Auth:** Service-to-service token (Swap Service).
- **Behavior:** Sets the book's transient status to RESERVED; returns Boolean success.
- **Use-case:** Called during swap creation to prevent concurrent requests for the same requester book.

---

### POST /api/catalog/books/{bookId}/unreserve

- Unreserve a previously reserved book and make it AVAILABLE again.
- **Auth:** Service-to-service token (Swap Service).
- **Behavior:** Reverts reservation (idempotent) and returns Boolean success.
- **Use-case:** Called when swap creation fails or a pending swap is cancelled.

---

### POST /api/catalog/books/confirm/swap

- Confirm and finalize a swap between two books (Catalog.confirmSwap).
- **Auth:** Service-to-service token (Swap Service).
- **Parameters:** `requesterBookId` and `responderBookId` as request parameters.
- **Behavior:** Performs the canonical ownership/state transition for both books (or unlists them) and returns a Boolean
  indicating success. The endpoint is called by the Swap Service during accept with a 10s timeout expectation.

---

## Kafka Events

### Publishes:

- `BOOK_CREATED`  
  Emitted when a new book is created.
- `BOOK_MEDIA_FINALIZED`  
  Emitted after the service receives a `MEDIA_STORED` event from the Media Service, updates the book's media references,
  and the book is ready for valuation.
- `BOOK_UNLISTED`  
  Emitted when a book is unlisted (deleted).

### Consumes:

- `MEDIA_STORED` (from `media.events`)  
  On receiving this event, updates the book's media list. If the book now has all required media, emits
  `BOOK_MEDIA_FINALIZED`.
- `VALUATION_READY` (from `valuation.events`)  
  Updates the book's valuation fields.

---

## Notes

- All endpoints (except recent books) require authentication.
- Media and valuation fields are updated asynchronously via Kafka events.
- The Catalog Service does not store or serve images directly; it stores media IDs and fetches signed URLs from the
  Media Service as needed.