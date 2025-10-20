# 0) Tech baseline (Initializr picks)

* Spring Web
* Spring Security (OAuth2 Resource Server)
* Spring Data JPA
* PostgreSQL Driver
* Spring for Apache Kafka
* Spring Data Redis (Access + Driver)
* Validation
* Actuator
* Lombok
* Spring Configuration Processor

> Dev DB schema: use `spring.jpa.hibernate.ddl-auto=update` (dev-only). No migrations.

---

# 1) Minimal project skeleton

```
notification-service
└─ src/main/java/com/bookswap/notification
   ├─ NotificationServiceApplication.java
   ├─ config/                # only lightweight config beans
   │   ├─ KafkaConfig.java
   │   └─ SecurityConfig.java
   ├─ domain/
   │   ├─ Notification.java        
   │   ├─ NotificationType.java
   │   └─ NotificationRepository.java
   ├─ service/
   │   ├─ UnreadCounterService.java  # Redis INCR/DECR
   │   └─ NotificationWriter.java    # DB write + counters
   ├─ messaging/
   │   └─ KafkaEventListener.java    # @KafkaListener; maps events → notifications
   └─ api/
       ├─ NotificationController.java # list, unread-count, mark-read
       └─ dto/
          ├─ NotificationItemDto.java
          └─ MarkReadRequest.java
```

# 3) The only model needed

**Entity**

* `id : UUID` (PK)
* `userId : UUID` (recipient)
* `type : NotificationType` (`SWAP_CREATED|SWAP_CANCELLED|SWAP_COMPLETED|BOOK_UNLISTED`)
* `title : String (≤128)`
* `body : String (≤512, nullable)`
* `metadataJson : String` (JSON text; small blob like `{ "swapId": "...", "bookId": "...", "deeplink": "/swaps/..." }`)
* `createdAt : Instant` (default now)
* `readAt : Instant?` (null = unread)
* `sourceEventId : UUID UNIQUE` (idempotency per upstream event)

**Repository (methods we’ll use immediately)**

* `Page<Notification> findByUser(userId, unreadOnly, Pageable)`
* `int markRead(userId, ids)` (returns rows updated)
* `int markAllRead(userId)`
* `long countByUserIdAndReadAtIsNull(userId)`

> With `ddl-auto=update`, the unique constraint on `sourceEventId` is created from the annotation. No Flyway needed.

---

# 4) Redis usage (tiny and fast)

* Key: `unread:{userId}` → integer

    * On create: `INCR unread:{userId}`
    * On mark read: `DECRBY unread:{userId} N` (clamp to ≥0)
    * On mark all: `SET unread:{userId} 0`
* No storing notifications in Redis—only counters.

`UnreadCounterService` wraps `StringRedisTemplate` with 3 methods: `incr`, `decr(n)`, `reset`, `get`.

---

# 5) Kafka consumption (event → one or more notification rows)

**Topics**

* `swap.events` (key=`swap_id`), `catalog.events` (key=`book_id`)

**Expected envelope (keep it flexible via Map/record)**

```json
{
  "event_id": "uuid",
  "event_type": "SWAP_CREATED|SWAP_CANCELLED|SWAP_COMPLETED|BOOK_UNLISTED",
  "occurred_at": "2025-10-19T04:35:12Z",
  "entities": {
    "swap_id": "...",
    "book_id": "...",
    "requester_id": "...",
    "responder_id": "...",
    "book_title": "...",
    "thumbnail_url": "..."
  }
}
```

**Recipient rules**

* `SWAP_CREATED` → responderId
* `SWAP_CANCELLED` → the other party
* `SWAP_COMPLETED` → requesterId & responderId
* `BOOK_UNLISTED` → owner (optional now)

**Processing flow**

1. Derive recipients (1 or 2).
2. Build `Notification` (title/body/metadataJson).
3. **Idempotent insert**: `sourceEventId` unique; if duplicate → skip.
4. If inserted → `INCR` unread counter for that user.
5. Commit Kafka offset **after** DB write succeeds.

> Keep mapper logic in `NotificationWriter` so `@KafkaListener` stays thin and testable.

---

# 6) REST API (MVP)

All endpoints require JWT (subject = `userId`).

* `GET /notify?unreadOnly=true&size=20&page=0`

    * Returns paged list (or switch to keyset later).
    * Response item = `{ id, type, title, body?, metadata, createdAt, read }`.

* `GET /notify/unread-count`

    * Reads Redis. On cache miss, fall back to `repo.countByUserIdAndReadAtIsNull()` and **repair** Redis.

* `POST /notify/read`

    * Body either `{ "ids": ["uuid1","uuid2"] }` or `{ "all": true }`.
    * Update DB (`readAt=now()`), compute `updatedCount`, then `DECRBY` Redis by that count (or `SET 0` for all).
    * Return `{ "updated": n, "unread": current }`.

> Frontend flow: page loads → call `/notify/unread-count` for badge, `/notify` for list. Clicking “mark all” or
> individual items calls `/notify/read`.

---

# 7) WebSocket (optional first, simple later)

Add WS in a later iteration:

* `GET /notify/stream` (JWT on connect).
* On each DB insert (step 3 above), if also maintain a simple in-memory `Map<userId, sessions>` (or Redis set when
  scale), push a tiny message:

```json
{
  "type": "NOTIFICATION",
  "id": "...",
  "title": "Swap completed",
  "unreadCount": 5
}
```

* If no session → do nothing; REST still covers it.

> Start without WS. Add once REST + counters + Kafka are stable.

---

# 8) Observability & ops (tiny)

* Actuator: `/actuator/health`, `/actuator/metrics`.
* Useful counters/timers:

    * notifications_inserted_total{type}
    * kafka_events_consumed_total
    * unread_counter_repairs_total
* Log `event_id`, `user_id`, and insert outcome (inserted/duplicate).

---

# 9) Implementation order (tickets)

1. **Boot skeleton**

    * Initializr setup, `application.yml`, Actuator up.

2. **Domain + JPA**

    * `NotificationType`, `Notification`, `NotificationRepository`.
    * Bring app up once; ensure table created (`ddl-auto=update`).

3. **Redis counters**

    * `UnreadCounterService` (+ config).
    * Manual test via a dummy `CommandLineRunner` that increments/decrements.

4. **NotificationWriter**

    * `create(recipient, type, title, body?, metadataJson, sourceEventId)`
    * Idempotent save + counter increment.

5. **REST API**

    * `GET /notify`, `GET /notify/unread-count`, `POST /notify/read`.
    * Local curl tests.

6. **Kafka listener**

    * `@KafkaListener` on `swap.events` (and `catalog.events` if needed).
    * Map events → recipients → `NotificationWriter.create(...)`.

7. **Security (JWT)**

    * Resource server config; extract `userId` from token (subject/claim).

8. **(Optional) WebSocket**

    * Basic `/notify/stream` and in-JVM session map.

9. **Hardening**

    * Retry on transient DB errors; commit offsets only after success.
    * Clamp Redis counter ≥ 0; repair on `/unread-count` cache miss.

---

# 10) Tiny “contract” for the frontend

**List:**

```json
GET /notify?unreadOnly=true&size=20&page=0
{
"items": [
{
"id": "…",
"type": "SWAP_COMPLETED",
"title": "Swap completed 🎉",
"body": "You swapped 'The Hobbit'",
"metadata": { "swapId": "…", "bookId": "…", "deeplink": "/swaps/..."},
"createdAt": "2025-10-19T04:35:12Z",
"read": false
}
],
"page": 0,
"size": 20,
"total": 17
}
```

**Badge:**

```json
GET /notify/unread-count
{
  "unread": 3
}
```

**Mark read:**

```json
POST /notify/read
{
  "ids": [
    "uuid1",
    "uuid2"
  ]
}
-- or --
{
  "all": true
}

Response:
{
"updated": 2,
"unread": 1
}
```

---
