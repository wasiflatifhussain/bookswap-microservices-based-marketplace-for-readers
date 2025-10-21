# Notification Service

This README documents the notification microservice (purpose, tech baseline, data model, Redis usage, Kafka consumption
rules, REST contract, ops notes and a recommended implementation order). It has been updated to match the service code
in this repo (`com.bookswap.notification_service.*`) and the controller endpoints currently implemented.

# 1) Minimal project skeleton

```
notification-service
└─ src/main/java/com/bookswap/notification_service
   ├─ NotificationServiceApplication.java
   ├─ config/                # lightweight config beans
   │   ├─ KafkaConfig.java
   │   └─ RedisConfig.java
   ├─ controller/            # REST controllers
   │   └─ NotificationController.java
   ├─ domain/
   │   ├─ Notification.java
   │   ├─ BookSnapshot.java
   │   ├─ NotificationType.java
   │   └─ NotificationRepository.java
   ├─ service/
   │   ├─ UnreadCounterService.java  # Redis INCR/DECR
   │   ├─ NotificationService.java   # business logic + DB writes
   │   └─ BookSnapshotService.java   # store/delete snapshots from catalog events
   ├─ messaging/
   │   └─ KafkaSwapEventsListener.java
   │   └─ KafkaCatalogEventsListener.java
   └─ dto/                     # event/response DTOs
```

---

# 2) Concise model view

Entity: Notification (see `domain/Notification.java`)

* `notificationId : UUID` (PK, generated)
* `userId : String` (recipient id, stored as string)
* `notificationType : NotificationType` (`SWAP_CREATED|SWAP_CANCELLED|SWAP_COMPLETED|BOOK_UNLISTED`)
* `title : String`
* `readStatus : ReadStatus` (`UNREAD|READ`)
* `description : String` (Lob)
* `createdAt`, `updatedAt`

Entity: BookSnapshot (see `domain/BookSnapshot.java`)

* `bookId : String` (PK)
* `title, author, bookCondition, valuation, ownerUserId`
* `createdAt`, `updatedAt`

Note: earlier design mentioned storing a `sourceEventId` for idempotency — the current code does not include it. Adding
a unique `sourceEventId` column is recommended if idempotent processing of upstream events is required in a future
iteration.

---

# 3) Redis usage (simple counter cache)

* Key: `unread:{userId}` → integer

    * On create: `INCR unread:{userId}`
    * On mark read: `DECRBY unread:{userId} N` (clamp to ≥0)
    * On mark all: `SET unread:{userId} 0`

`UnreadCounterService` is a thin wrapper around `StringRedisTemplate` with methods: `increment`, `decrement`, `reset`,
`getIfPresent`.

---

# 4) Kafka consumption (events → notification rows)

**Topics (configured via properties)**

* `spring.kafka.consumer.services.swap-service.topic` (swap.events)
* `spring.kafka.consumer.services.catalog-service.topic` (catalog.events)

The listeners live in `messaging/` and route events to service-level handlers:

* `KafkaSwapEventsListener` → reacts to `SWAP_CREATED`, `SWAP_CANCELLED`, `SWAP_COMPLETED` and calls methods in
  `NotificationService`.
* `KafkaCatalogEventsListener` → reacts to `BOOK_VALUATION_FINALIZED` (stores book snapshot) and `BOOK_UNLISTED` (
  deletes snapshot).

Expected event envelope (keep flexible; DTOs are used to map JSON payloads):

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

Recipient rules implemented in `NotificationService`:

* `SWAP_CREATED` → notify the responder (owner of the responder book)
* `SWAP_CANCELLED` → notify the requester (owner of the requester book)
* `SWAP_COMPLETED` → notify both requester and responder
* `BOOK_UNLISTED` → delete the book snapshot (optional notification to owner)

Processing flow inside listeners/services:

1. Parse event → map to a DTO (SwapCreatedEvent, SwapCancelEvent, SwapCompletedEvent, BookFinalizedEvent,
   BookUnlistedEvent).
2. `NotificationService` loads `BookSnapshot`s (requester/responder) as needed.
3. Build `Notification` entity and save it.
4. After transaction commit, call Redis `INCR` to bump the unread counter via
   `TransactionSynchronizationManager.registerSynchronization` so offsets are committed only after DB writes succeed.

---

# 5) REST API (MVP)

All endpoints expect JWT authentication (the controller extracts `authentication.getName()` as the `userId`). Current
base path: `/api/notifications`.

* `GET /api/notifications/get?unreadOnly=true&page=0&size=20`

    * Returns paged list (or switch to keyset later).
    * Response item = `{ id, type, title, body?, metadata, createdAt, read }`.

* `GET /api/notifications/unread-count`

    * Reads Redis. On cache miss, fall back to `repo.countByUserIdAndReadAtIsNull()` and **repair** Redis.

* `POST /api/notifications/read`

    * Body either `{ "ids": ["uuid1","uuid2"] }` or `{ "all": true }`.
    * Update DB (`readAt=now()`), compute `updatedCount`, then `DECRBY` Redis by that count (or `SET 0` for all).
    * Return `{ "updated": n, "unread": current }`.

> Frontend flow: page loads → call `/api/notifications/unread-count` for badge, `/api/notifications/get` for list.
> Clicking “mark all” or
> individual items calls `/api/notifications/read`.

---

# 7) WebSocket (future implementation)

Add WS in later iteration:

* `GET /api/notifications/stream` (JWT on connect).
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

# 9) Future notes / optional improvements

* Add a WebSocket endpoint `/api/notifications/stream` to push live notifications.

---
