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

Flow with Outbox + Kafka

Catalog (book created)

Save Book row with:

valuation = null

mediaIds = []

status = DRAFT

In same DB TX, enqueue book.created into outbox.

Relay publishes to Kafka with the whole Book payload (or at least the fields Media/Valuation need).

Media Service (consumer)

Subscribes to book.created.

Stores files to S3 / DB metadata.

Emits media.uploaded { bookId, mediaIds[], urls[], thumbs[] }.

Valuation Service (consumer)

Subscribes to book.created.

Runs valuation job.

Emits valuation.updated { bookId, valuation }.

Catalog (consumer)

Listens to media.uploaded: updates the mediaIds column for that book.

Listens to valuation.updated: updates the valuation field.

Once both are filled, transitions status = LISTED.

Notification/Search

Can also consume book.created or book.listed to notify users or index feeds.

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