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

## Authentication

All endpoints (except health/docs) require a valid access token.

- Each request must have `Authorization: Bearer <token>`.
- The token is introspected with Keycloak using the backend client.
- Only active tokens are accepted; others get `401/403 Unauthorized`.

See `security/KeycloakIntrospectionFilter.java` for details.

## Book Creation & Outbox Event Flow

When a user wants to save a new book, the following flow is executed:

### 1. User Submits Book Details

**Frontend:**  
User sends a multipart POST request to store book details and images.

- **Endpoint:** `POST /api/catalog/books` (JSON metadata)

**Catalog Service:**

- **BEGIN TRANSACTION**
    - `INSERT INTO books (bookId, status=DRAFT, valuation=NULL, mediaIds=[])`
    - `INSERT INTO outbox_events (eventType=BOOK_CREATED, payload={book metadata})`
- **COMMIT**

- **CatalogRelay (background):**
    - Detects new outbox row
    - Publishes to Kafka topic `book.created`
    - Marks outbox row as SENT

---

### 2. User Uploads Book Images

- **Endpoint:** `POST /api/media/books/{bookId}/images` (multipart photos)

**Media Service:**

- Stores files in S3/MinIO
- Inserts media records in its DB
- `INSERT INTO outbox_events (eventType=MEDIA_UPLOADED, payload={bookId, mediaIds, urls, thumbs})`
- **MediaRelay:**
    - Publishes Kafka `media.uploaded`
    - Marks outbox row as SENT

---

### 3. Valuation Service Consumes Events

- **Consumes:** `book.created` and `media.uploaded` from Kafka
- Once it has book metadata and at least one image URL:
    - Runs valuation job
    - `INSERT INTO outbox_events (eventType=VALUATION_UPDATED, payload={bookId, valuation})`
- **ValuationRelay:**
    - Publishes Kafka `valuation.updated`
    - Marks outbox row as SENT

---

### 4. Catalog Service Consumes Events

- **Consumes:**
    - `media.uploaded` → `UPDATE books SET mediaIds = [...]`
    - `valuation.updated` → `UPDATE books SET valuation = 12.5`

- When both `valuation != NULL` **and** `mediaIds` is not empty:
    - `UPDATE books SET status = LISTED`
    - *(Optional)* Enqueue outbox event `BOOK_LISTED`
    - **Relay:** Publishes Kafka `book.listed`

---

### 5. Other Consumers

- **Notification/Search Services:**  
  Can consume `book.created` or `book.listed` to notify users or index feeds.

---

```
When user wants to save new book, frontend does a multipart POST to store book details + images.
User → POST /api/catalog/books (JSON metadata)
   Catalog:
      BEGIN TX
         INSERT INTO books (bookId, status=DRAFT, valuation=NULL, mediaIds=[])
         INSERT INTO outbox_events (eventType=BOOK_CREATED, payload={book metadata})
      COMMIT
   → CatalogRelay (background) sees new outbox row
      → publishes to Kafka topic "book.created"
      → marks outbox row SENT

User → POST /api/media/books/{bookId}/images (multipart photos)
   Media Service:
      - Stores files in S3/MinIO
      - Inserts media records in its DB
      - INSERT INTO outbox_events (eventType=MEDIA_UPLOADED, payload={bookId, mediaIds, urls, thumbs})
   → MediaRelay publishes Kafka "media.uploaded"
      → marks outbox row SENT

Valuation Service ← Kafka "book.created"
Valuation Service ← Kafka "media.uploaded"
   - Once it has book metadata + at least one image URL:
      - Runs valuation job
      - INSERT INTO outbox_events (eventType=VALUATION_UPDATED, payload={bookId, valuation})
   → ValuationRelay publishes Kafka "valuation.updated"
      → marks outbox row SENT

Catalog Consumer ← Kafka "media.uploaded"
   → UPDATE books SET mediaIds = [...]

Catalog Consumer ← Kafka "valuation.updated"
   → UPDATE books SET valuation = 12.5

Catalog:
   - When both valuation != NULL AND mediaIds not empty:
       → UPDATE books SET status = LISTED
       → (optional) enqueue outbox event BOOK_LISTED
       → Relay publishes Kafka "book.listed"
```

## Current Progress & TODOs

### Catalog Service

- Outbox pattern and event publishing to Kafka are implemented.
- **Event listeners for valuation and media events are not yet implemented.**
    - These will be added after the Valuation and Media services are developed.

---

### Planned Next Steps

- [ ] Build Valuation and Media services.
- [ ] Implement event listeners in Catalog Service:
    - Listen to `valuation.events` (`BOOK_VALUATED`) → update `Book.valuation`
    - Listen to `media.events` (`MEDIA_PROCESSED`) → update `Book.mediaId` / `thumbnailUrl`
- [ ] Ensure idempotency using `outboxEventId` from incoming events.
- [ ] Use `catalog-service` as the Kafka consumer group ID.
- [ ] Test event-driven updates and document event payloads.

---
