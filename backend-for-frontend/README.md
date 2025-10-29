# BFF: Public Endpoints (only what the client uses)

## 0) Session & Navbar

### GET `/bff/me`

**Purpose:** navbar snapshot (user, wallet, unread notifications)

**Downstream (order):**

1. `GET /api/wallet/me/balance` (Wallet)
2. `GET /api/notifications/unread-count` (Notification)

## 1) Home Feed

### GET `/bff/feed/recent?limit=&cursor=`

**Purpose:** homepage cards (title, author, valuation, primary thumbnail)

**Downstream (order):**

1. `GET /api/catalog/books/recent?limit=` (Catalog)
2. `POST /api/media/view-urls:batch` (Media; with mediaIds from 1)

---

## 2) Book Details

### GET `/bff/books/{bookId}`

**Purpose:** one book’s full page (all images, valuation, description)

**Downstream (order):**

1. `GET /api/catalog/books/{bookId}` (Catalog)
2. `POST /api/media/view-urls:batch` (Media; with mediaIds from 1)

---

## 3) My Books

### GET `/bff/me/books?limit=&cursor=`

**Purpose:** list my books (+ primary thumbnail)

**Downstream (order):**

1. `GET /api/catalog/books/user/{userId}` (Catalog)
2. `POST /api/media/view-urls:batch` (Media; with mediaIds from 1)

### DELETE `/bff/me/books/{bookId}`

**Purpose:** unlist a book (if not reserved)

**Downstream (order):**

1. `DELETE /api/catalog/books/{bookId}` (Catalog)
   *(Catalog/Swap/Wallet side-effects happen via their own flows/events; BFF just forwards the result)*

---

## 4) Add a Book (orchestrated but thin)

### POST `/bff/me/books`

**Purpose:** create book metadata (DRAFT)

**Downstream (order):**

1. `POST /api/catalog/books` (Catalog)

### POST `/bff/me/books/{bookId}/uploads:init`

**Purpose:** get presigned PUT URLs

**Downstream (order):**

1. `POST /api/media/uploads/{bookId}/init` (Media)

### POST `/bff/me/books/{bookId}/uploads:complete`

**Purpose:** finalize uploaded objects

**Downstream (order):**

1. `POST /api/media/uploads/{bookId}/complete` (Media)

---

## 5) Swap Center

### GET `/bff/me/swaps/sent?status=&cursor=`

**Purpose:** list swaps I sent (enriched with book cards)

**Downstream (order):**

1. `GET /api/swaps/sent?userId=&status=` (Swap)
2. `POST /api/catalog/books/bulk` (Catalog; with all bookIds from 1)
3. `POST /api/media/view-urls:batch` (Media; with mediaIds from 2)

### GET `/bff/me/swaps/received?status=&cursor=`

**Purpose:** list swaps I received (enriched)

**Downstream (order):**

1. `GET /api/swaps/received?userId=&status=` (Swap)
2. `POST /api/catalog/books/bulk` (Catalog)
3. `POST /api/media/view-urls:batch` (Media)

### POST `/bff/swaps`

**Purpose:** create swap request

**Downstream (order):**

1. `POST /swap/requests` (Swap)

### POST `/bff/swaps/{swapId}/cancel`

**Purpose:** requester cancels

**Downstream (order):**

1. `POST /swap/requests/{swapId}/cancel` (Swap)

### POST `/bff/swaps/{swapId}/decline`

**Purpose:** responder declines

**Downstream (order):**

1. `POST /swap/requests/{swapId}/decline` (Swap)

### POST `/bff/swaps/{swapId}/accept`

**Purpose:** responder accepts → swap completion

**Downstream (order):**

1. `POST /swap/requests/{swapId}/accept` (Swap)
2. *(Optional enrich after success)* `POST /api/catalog/books/bulk` + `POST /api/media/view-urls:batch` to return nice
   UI payload

---

## 6) Notifications (navbar + page)

### GET `/bff/notifications/unread-count`

**Purpose:** navbar badge

**Downstream (order):**

1. `GET /api/notifications/unread-count` (Notification)

### GET `/bff/notifications?unreadOnly=&page=&size=`

**Purpose:** list notifications

**Downstream (order):**

1. `GET /api/notifications/get?unreadOnly=&page=&size=` (Notification)

### POST `/bff/notifications/read`

**Purpose:** mark some/all as read

**Downstream (order):**

1. `POST /api/notifications/read` (Notification)

---

## 7) Wallet (navbar quick fetch)

### GET `/bff/wallet/balance`

**Purpose:** show BookCoins balance

**Downstream (order):**

1. `GET /api/wallet/me/balance` (Wallet)

---

# Execution Notes (orders & policies)

* **Token Relay everywhere**: BFF validates the JWT and forwards the same `Authorization: Bearer …` to every downstream.
* **Timeouts & retries**: Reads (Catalog/Media/Notif/Wallet) 1–2s timeouts + 1 retry with jitter; writes (Swap
  create/accept/cancel, Catalog delete) no auto-retries unless idempotency-key is present.
* **Idempotency**: Accept `Idempotency-Key` on `POST /bff/swaps`, `/accept`, `/cancel`, `/decline`, `/me/books`,
  `/uploads:complete`.
* **Pagination**: Prefer `cursor` over page/size for lists; BFF can translate page/size → cursor internally if
  downstream doesn’t support cursors yet.

---

# Do you need Request/Response DTOs everywhere?

**Short answer:**

* **Yes for the BFF public boundary.** Always define **your own BFF Request/Response DTOs** (what the frontend sees).
  This keeps the UI contract stable even if downstream services evolve.
* **For downstream calls, use separate small client DTOs per service** (CatalogClientDTO, MediaClientDTO, etc.) to
  decouple from their models. Map to/from your BFF DTOs.

**Practical rules:**

1. **Public BFF API = your DTOs (mandatory).**

    * These are your “view models” shaped for the UI (e.g., include pre-signed URLs, merged fields, etc.).
    * Never expose internal IDs/headers you don’t want clients to rely on.

2. **Downstream Clients = service-scoped DTOs (recommended).**

    * Create tiny DTOs that match what you actually read/write for each service call.
    * Keep them **package-scoped** inside a `client/catalog`, `client/media`, … module so changes are localized.

3. **Mapping layer (thin).**

    * Centralize mapping in `mappers/` (manual mappers or MapStruct).
    * Name them clearly: `CatalogToBffMapper`, `MediaToBffMapper`, etc.

4. **When is pass-through acceptable?**

    * Only for *temporary* plumbing or admin-only internal tools. For your public BFF endpoints, pass-through couples
      your UI to microservice internals and will bite you later.

5. **Error DTO (one shape).**

    * Standardize the BFF error response:

      ```json
      { "error": { "code": "UPSTREAM_TIMEOUT", "message": "Catalog timed out", "details": {"service":"catalog"} } }
      ```
    * Map downstream errors into your codes (`CATALOG_NOT_FOUND`, `SWAP_CONFLICT`, etc.).

6. **Validation DTOs.**

    * For write endpoints, keep BFF request DTOs minimal and validate early (Bean Validation).
    * Example: `CreateSwapRequest{ requesterBookId, responderBookId }`.

**Tiny example (Java records)**

*Public BFF DTOs*

```java
public record FeedItemDto(String bookId, String title, String author,
                          String condition, ValuationDto valuation,
                          String thumbnailUrl, String ownerId) {
}

public record ValuationDto(double coins, double confidence) {
}
```

*Downstream client DTOs (per service)*

```java
// catalog client
record CatalogRecentBook(String bookId, String title, String author,
                         String condition, Double coins, Double confidence,
                         String primaryMediaId, String ownerId) {
}

// media client
record MediaViewUrl(String mediaId, String url, Instant expiresAt) {
}
```

*Mapper (sketch)*

```java
FeedItemDto toFeedItem(CatalogRecentBook b, Map<String, MediaViewUrl> mediaMap) {
  var url = mediaMap.getOrDefault(b.primaryMediaId(), null);
  return new FeedItemDto(
          b.bookId(), b.title(), b.author(), b.condition(),
          new ValuationDto(nz(b.coins()), nz(b.confidence())),
          url != null ? url.url() : null,
          b.ownerId()
  );
}
```

---


com.bookswap.backend_for_frontend
├─ api/ # controllers
│ ├─ MeController.java
│ ├─ FeedController.java
│ ├─ BooksController.java
│ ├─ SwapsController.java
│ └─ NotificationsController.java
├─ service/ # orchestration
│ ├─ MeService.java
│ ├─ FeedService.java
│ ├─ BooksService.java
│ ├─ SwapsService.java
│ └─ NotificationsService.java
├─ client/ # one per downstream service
│ ├─ CatalogClient.java
│ ├─ MediaClient.java
│ ├─ SwapClient.java
│ ├─ WalletClient.java
│ └─ NotificationClient.java
├─ dto/ # BFF-public DTOs (what FE sees)
│ └─ ...
├─ mapper/ # maps client DTOs -> BFF DTOs
│ └─ ...
└─ config/ # security, cors, webclient, properties


---


page shaped api layer + service layer pairs:
home feed only has books by diff users
my books page has only my books
swap center has requests by me and requests to me
add book page is for adding my books
when click book, takes to book page

- also need page or feature where user selects one of their books and gets matching books with similar valuation (
  endpoint alrdy exists in catalog service)

so yea i realized that, maybe better to make a feeditem dto and also i realized that, maybe better to fetch one image
each from media service

add book page on frontend:
User clicks Add Book
Frontend → BFF: sends create init request
BFF: creates book in Catalog, calls Media Service, gets presigned URLs, returns them to FE
Frontend: uploads images directly to storage via PUT using those URLs
Frontend → BFF: sends complete upload request after uploads finish
BFF: confirms upload with Media Service and finalizes creation
Frontend: receives success response and redirects to the book page