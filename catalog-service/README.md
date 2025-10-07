```
catalog-service/
└── src/
└── main/
├── java/com/bookswap/catalog/
│    ├── CatalogServiceApplication.java   # main entry
│
│    ├── config/                          # service-level configs
│    │     ├── SecurityConfig.java
│    │     ├── WebConfig.java
│    │     └── OpenApiConfig.java
│
│    ├── controller/                      # REST controllers
│    │     └── BookController.java
│
│    ├── dto/                             # request/response DTOs
│    │     ├── request/
│    │     │     ├── CreateBookRequest.java
│    │     │     └── UpdateBookRequest.java
│    │     └── response/
│    │           └── BookResponse.java
│
│    ├── domain/                          # models & enums
│    │     ├── Book.java
│    │     ├── BookStatus.java
│    │     └── Condition.java
│
│    ├── repository/                      # Spring Data JPA repositories
│    │     └── BookRepository.java
│
│    ├── service/                         # business logic
│    │     ├── BookService.java
│    │     └── BookServiceImpl.java
│
│    ├── external/                     # external service clients
│    │     ├── ValuationClient.java
│    │     └── MediaClient.java
│
│    ├── events/                          # outbox + kafka publisher
│    │     ├── BookEvent.java
│    │     ├── OutboxEntity.java
│    │     └── EventPublisher.java
│
│    ├── security/                        # auth checks, JWT utils
│    │     └── AuthUtil.java
│
│    └── util/                            # helper classes (mappers, constants)
│          └── BookMapper.java
│
└── resources/
├── application.yml
├── bootstrap.yml                    # for Config Server
└── db/migration/
└── V1__init.sql                # Flyway migration
```

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