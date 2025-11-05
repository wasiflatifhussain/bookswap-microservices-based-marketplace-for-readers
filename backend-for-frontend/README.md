# **BookSwap BFF – Public API**

**Base path:** `/api/bff`
The BFF connects the frontend with Catalog, Media, Swap, Wallet, and Notification services — orchestrating requests and
shaping unified responses.

---

## **0) Navbar & Session**

### `GET /navbar/snapshot`

**Purpose:** Get user snapshot — wallet balance, unread notifications, status.
**Auth:** Required
**Downstream:**

1. `GET /wallet/me/balance`
2. `GET /notifications/unread-count`

---

### `GET /navbar/notifications?unreadOnly=&page=&size=`

**Purpose:** Fetch paginated notifications for current user.
**Params:** `unreadOnly` (default `false`), `page` (default `0`), `size` (default `20`)
**Downstream:** `GET /notifications/get?unreadOnly=&page=&size=`

---

### `POST /navbar/notifications/read`

**Purpose:** Mark notifications as read.
**Body:** `["notif-id-1", "notif-id-2", ...]`
**Downstream:** `POST /notifications/read`
**Response:** `204 No Content`

---

## **1) Home Feed**

### `GET /home/feed?limit=`

**Purpose:** Get homepage book cards (title, author, valuation, thumbnail).
**Param:** `limit` (default `20`)
**Downstream:**

1. `GET /catalog/books/recent?limit=`
2. `POST /media/view-urls:batch`

---

## **2) Books**

### `GET /books/get/{bookId}`

**Purpose:** Get full details of a single book (description, valuation, images).
**Downstream:**

1. `GET /catalog/books/{bookId}`
2. `POST /media/view-urls:batch`

---

### `GET /books/matches/{bookId}?tolerance=`

**Purpose:** Find books with similar valuation.
**Param:** `tolerance` (default `0.15`)
**Downstream:**

1. `GET /catalog/books/{bookId}/matches?tolerance=`
2. `POST /media/view-urls:batch`

---

### `GET /books/me/get`

**Purpose:** Get all books owned by current user.
**Downstream:**

1. `GET /catalog/books/user/{userId}`
2. `POST /media/view-urls:batch`

---

### `DELETE /books/me/delete/{bookId}`

**Purpose:** Delete or unlist a user’s book.
**Downstream:** `DELETE /catalog/books/{bookId}`

---

### `POST /books/create/init`

**Purpose:** Step 1 of add-book — create book metadata & init uploads.
**Downstream:**

1. `POST /catalog/books`
2. `POST /media/uploads/init`

---

### `POST /books/create/complete`

**Purpose:** Step 2 — confirm completed uploads and finalize creation.
**Downstream:** `POST /media/uploads/{bookId}/complete`

---

## **3) Swap Center**

### `GET /swap/me/sent`

**Purpose:** List swaps sent by current user.
**Downstream:** `GET /swap/requests/sent?requesterUserId=&swapStatus=`

---

### `GET /swap/me/received`

**Purpose:** List swaps received by current user.
**Downstream:** `GET /swap/requests/received?responderUserId=&swapStatus=`

---

### `GET /swap/book/{bookId}/requests`

**Purpose:** List all swaps involving a specific book (for book owner).
**Downstream:** `GET /swap/requests/for-book?userId=&bookId=`

---

### `POST /swap/create`

**Purpose:** Create a new swap request.
**Body:** `{ requesterBookId, responderBookId, responderUserId }`
**Downstream:** `POST /swap/requests/create`

---

### `POST /swap/cancel/{swapId}`

**Purpose:** Cancel a swap (by requester).
**Downstream:** `POST /swap/requests/cancel`

---

### `POST /swap/accept/{swapId}`

**Purpose:** Accept a swap (by responder).
**Downstream:** `POST /swap/requests/accept`

---

## **4) Policies & Notes**

* **Token relay:** Forward JWT in all downstream calls.
* **Timeouts:** Reads use ~1–2 s with 1 retry; writes don’t auto-retry.
* **Idempotency:** Supported via optional `Idempotency-Key` header on writes.
* **Pagination:** Notifications use page/size; other lists use `limit`.
* **Error handling:** Standardize error format if needed across downstreams.

---

## **5) Future Improvements**

### **Authentication & Ownership Checks**

Add backend verification using relayed JWT:

* Derive `userId` from JWT, not client input.
* Validate ownership before modifying or deleting resources.
* Enforce per-user access:

    * Only book owners can delete or view their swap requests.
    * Only swap requester can cancel; only responder can accept.
* Downstream services should verify the relayed token matches the claimed user.

---

## **6) Implemented Endpoints Summary**

✅ `/navbar/snapshot`  
✅ `/navbar/notifications` + `/read`  
✅ `/home/feed`  
✅ `/books/get/{bookId}`  
✅ `/books/matches/{bookId}`  
✅ `/books/me/get` + `/me/delete/{bookId}`  
✅ `/books/create/init` + `/create/complete`  
✅ `/swap/me/sent` + `/me/received`  
✅ `/swap/book/{bookId}/requests`  
✅ `/swap/create` + `/cancel/{swapId}` + `/accept/{swapId}`

---