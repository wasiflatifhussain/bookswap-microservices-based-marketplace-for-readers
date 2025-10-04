Our current project will calls endpoint -> AWS SDK -> S3 Client

Note:
The S3 User we are currently using is set to have access permission of Local Code only.
When using the code in Prod, change the S3 User to the one that has access permission of Prod Code.

under main file runner, add config:
AWS_ACCESS_KEY=<your-aws-access-key>; AWS_SECRE_ACCESS_KEY=<your-aws-secret-access-key>; S3_BUCKET_NAME=<
your-s3-bucket-name>;

# to dos:

for each user, maybe later we just make folders in s3 so easier to fetch media for that user?

```
src/
└─ main/
   ├─ java/
   │  └─ com/bookswap/media_service/
   │     ├─ MediaServiceApplication.java
   │     │
   │     ├─ config/
   │     │  ├─ S3Config.java                 // S3/MinIO client, endpoint/region creds
   │     │  ├─ KafkaConfig.java              // producer template, topics, DLQ
   │     │  ├─ AppProperties.java            // @ConfigurationProperties for bucket, CDN, limits
   │     │  └─ WebConfig.java                // CORS (allow PUT to S3 origin if presign flow)
   │     │
   │     ├─ domain/
   │     │  ├─ Media.java                    // JPA entity: media_id, book_id, owner, kind, urls, status
   │     │  ├─ Upload.java                   // JPA entity: upload_id, media_id, expected_size, mime, expires_at, status
   │     │  └─ model/
   │     │     ├─ MediaKind.java             // COVER | CONDITION | OTHER
   │     │     └─ MediaStatus.java           // PENDING | UPLOADED | PROCESSED | FAILED
   │     │
   │     ├─ repository/
   │     │  ├─ MediaRepository.java
   │     │  └─ UploadRepository.java
   │     │
   │     ├─ dto/
   │     │  ├─ request/
   │     │  │  ├─ UploadInitRequest.java     // files:[{name,size,mime,kind}]
   │     │  │  └─ CompleteUploadRequest.java // (optional) if you send checksum, etc.
   │     │  └─ response/
   │     │     ├─ UploadInitResponse.java    // tickets: uploadId, mediaId, presignedUrl, headers...
   │     │     └─ MediaResponse.java
   │     │
   │     ├─ service/
   │     │  ├─ StorageService.java           // low-level S3 ops (put/get/head, presign)
   │     │  ├─ UploadService.java            // business flow: init/complete, validate, persist
   │     │  ├─ MediaService.java             // read/delete media, cascade S3 delete, emit events
   │     │  ├─ ThumbnailService.java         // async thumb worker (can be scheduled/consumer)
   │     │  └─ EventPublisher.java           // wraps KafkaTemplate, outbox if you add it
   │     │
   │     ├─ controller/
   │     │  ├─ UploadController.java         // POST /media/books/{bookId}/uploads:init
   │     │  │                                // POST /media/uploads/{uploadId}/complete
   │     │  └─ MediaController.java          // GET/DELETE /media/{mediaId}, (optional) POST /media/.../images for proxy mode
   │     │
   │     ├─ messaging/
   │     │  ├─ event/                        // payload classes: MediaUploadedEvent, MediaProcessedEvent
   │     │  ├─ producer/                     // MediaEventsProducer (Kafka)
   │     │  └─ consumer/                     // (empty for now; add if media listens to others)
   │     │
   │     ├─ security/
   │     │  └─ JwtAuthConfig.java            // resource server config when you hook Keycloak
   │     │
   │     ├─ exception/
   │     │  ├─ GlobalExceptionHandler.java
   │     │  ├─ NotOwnerException.java
   │     │  └─ ValidationException.java
   │     │
   │     └─ util/
   │        ├─ ObjectKeyBuilder.java         // books/{bookId}/{mediaId}/original, thumb.jpg
   │        └─ Checksums.java                // MD5/base64 helpers (optional)
   │
   └─ resources/
      ├─ application.yml
      └─ db/migration/                       // (optional) Flyway/Liquibase for media + upload tables
         └─ V1__init.sql

---
```

## Media + Catalog: Final Notes (Sequential + Pre-Signed URLs)

### Core Principles

- Do not store pre-signed URLs. Store only stable identifiers: mediaId, objectKey, plus metadata.
- Generate short-lived URLs on demand (5–10 min TTL) for uploads (PUT) and views (GET).
- HTTP for queries, Kafka for events. Media emits MEDIA_STORED on successful upload completion; Catalog consumes it to
  set primaryMediaId (first image wins, or your own rule).

### Data Model (Media Service)

**Table: media**

- media_id (PK, UUID string)
- book_id (FK to Catalog book)
- owner_user_id
- object_key (e.g., books/{bookId}/{mediaId}/original)
- mime_type (nullable until verified)
- size_bytes (nullable until verified)
- status (PENDING | STORED | DELETED)
- created_at, updated_at (server timestamps)

**Indexes**

- idx_media_book (book_id)
- idx_media_owner (owner_user_id)

**State transitions**

- PENDING → (on complete + HEAD success) → STORED
- PENDING → (sweeper timeout w/o object) → optional cleanup
- STORED → DELETED (future: soft delete)

### S3/MinIO Rules

- Private bucket only. All reads/writes via pre-signed URLs.
- Required headers when you sign PUT must be sent by the browser (e.g., Content-Type, optional Content-MD5).
- Object keys deterministic: books/{bookId}/{mediaId}/original (+ optional variants later like thumb_256.webp).

### Kafka (Events-Only)

- Topic: media.events
- Key: mediaId
- Event: MEDIA_STORED
- Fields: eventId, occurredAt, media block (mediaId, bookId, ownerUserId, objectKey, mimeType, sizeBytes)
- Catalog Consumer Behavior: if book has no primaryMediaId, set it to this mediaId. (Your rule; keep it idempotent.)

### HTTP Contracts

#### Media Service

**Init Uploads (sequential)**

- POST /media/books/{bookId}/uploads:init
- Body: { files: [{ name, size, mime }] }
- Creates one media row per file with status=PENDING and deterministic objectKey.
- Response:

```json
{
  "uploads": [
    {
      "mediaId": "...",
      "presignedPutUrl": "...",
      "headers": {
        "Content-Type": "<mime>"
      },
      "expiresAt": "ISO8601"
    }
  ]
}
```

**Complete Upload**

- POST /media/uploads/{mediaId}/complete
- Behavior:
    - HEAD object by objectKey
    - If present: update status=STORED, set mime_type, size_bytes
    - Emit MEDIA_STORED
- Response: { mediaId, bookId, status: "STORED" }
- Errors:
    - 404 if mediaId unknown
    - 409/422 if object not found (i.e., PUT didn’t happen)

**Get View URL (single)**

- GET /media/{mediaId}/view-url
- Looks up objectKey, returns short-lived pre-signed GET URL
- Response: { mediaId, url, expiresAt }
- 404 if not STORED (or you allow for PENDING → 409)

**Get View URLs (batch)**

- POST /media/view-urls:batch
- Body: { mediaIds: ["..."] }
- Response: { items: [{ mediaId, url, expiresAt }] }
- Optimizes N calls on list/detail pages.

**List by Book (no URLs)**

- GET /media/by-book/{bookId}
- Response: { items: [{ mediaId, mimeType, sizeBytes, status }] }
- Frontend (or Catalog) calls view-url endpoints for actual renders.

#### Catalog Service

- Create Book: POST /catalog/books → { bookId }
- Get Book (server-composed): GET /catalog/books/{bookId}
- Catalog returns finalized view object including embedded short-lived URLs (see composition below).

### Runtime Composition (Your Chosen Path)

- Frontend calls Catalog for book details.
- Catalog extracts primaryMediaId (and any other media to show), then:
    - Calls Media: POST /media/view-urls:batch with needed mediaIds.
    - Embeds the returned url + expiresAt into its response payload to Frontend.
- Result: FE gets a fully renderable book object in a single request to Catalog.

### End-to-End Flows

**Upload Flow (sequential)**

1. FE → Catalog: POST /catalog/books → bookId
2. FE → Media: POST /media/books/{bookId}/uploads:init → pre-signed PUT URLs (5–10 min TTL)
3. FE → S3: PUT bytes to each presignedPutUrl with required headers
4. FE → Media: POST /media/uploads/{mediaId}/complete
5. Media HEADs object
6. Update row → STORED, set metadata
7. Emit MEDIA_STORED (Kafka)
8. Catalog (consumer): on MEDIA_STORED, set primaryMediaId if not already set

**View Flow (server-to-server)**

1. FE → Catalog: GET /catalog/books/{bookId}
2. Catalog gathers mediaIds (at least primaryMediaId), calls:
    - Catalog → Media: POST /media/view-urls:batch
3. Catalog embeds URLs in its book response to FE (short-lived GETs)
4. FE renders images immediately

### Error & Recovery Notes

- User closes tab before “complete”: Item remains PENDING.
- Add sweeper job (e.g., every 15 min) to HEAD PENDING older than X minutes:
    - If object exists → flip to STORED, emit MEDIA_STORED
    - Else → leave or purge per policy
- Expired GET while viewing: If FE receives 403/Signature error, FE (or Catalog on reload) requests a fresh view URL.

### Idempotency

- uploads:init: harmless to re-call; you’ll add new rows—client should not re-init the same files unintentionally.
- complete: safe to call twice; second time is a no-op if already STORED.

### Validation

- Enforce mime allowlist (e.g., images only).
- Optional Content-MD5 for integrity.

### Performance & Caching

- Batch sign to avoid N*HTTP on list/detail pages.
- Short cache inside Media (e.g., Redis ~30–60s) for pre-signed GETs to handle bursts:
    - Key: signed:get:{objectKey} → { url, expiresAt }
    - If remaining TTL < 60s, refresh.
- TTLs: PUT 5–10 min; GET ~5 min (tune per UX).

### Security & Auth

- Private bucket only.
- Media endpoints require a valid user (Keycloak/JWT) and authorization:
    - uploads:init and complete: user must own bookId.
    - view-url: allow if the book is publicly viewable or the requester is the owner (depends on your Catalog visibility
      rules).
- Service-to-service (Catalog→Media) uses client credentials (machine token) or mTLS.

### Non-Goals (for now)

- No CDN/signed cookies yet (future optimization).
- No virus scanning/thumbnail pipeline in the critical path (can be added off S3 events later).

---

### “Done When” Checklist

**Media:**

- media table created; writes store objectKey, no presigned URLs.
- uploads:init, complete, view-url, view-urls:batch, by-book live.
- HEAD on complete updates row to STORED with mime_type, size_bytes.
- MEDIA_STORED event emitted on successful complete.
- Optional sweeper job for stale PENDING.

**Catalog:**

- Consumes MEDIA_STORED and sets primaryMediaId deterministically.
- GET /catalog/books/{bookId} calls Media view-urls:batch server-to-server for any media it returns and embeds
  short-lived GET URLs.

**End-to-end:**

- FE can create book → upload via pre-signed PUT → complete → see the cover/image via Catalog response without extra
  FE→Media calls.
- Expired URL refresh path tested (re-request from Catalog triggers fresh sign under the hood).