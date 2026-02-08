# **BookSwap MarketPlace**

Welcome to **BookSwap** – a microservices-based marketplace where readers trade books using BookCoins.

This repository contains several domain services (**Catalog**, **Media**, **Valuation**, **Swap**, **Wallet**, *
*Notification**, **Email**, and a **BFF**) plus infra connectors.
Each service runs independently, communicates through Kafka events, and contributes to a fully event-driven swap flow.

---

## **Project Snapshot**

* **Goal:** Build a reliable, event-driven market for book swapping with clear ownership, valuations, and a lightweight
  BookCoin currency.
* **Architecture:**

    * Small, focused microservices
    * **Kafka** for event communication
    * **Outbox pattern** for reliable publishing
    * **REST** for direct reads
    * **WebSockets** for real-time notifications
    * **BFF** for orchestration and aggregation

---

## **Services & Responsibilities**

### **Catalog Service**

* Owns all **book records** and their lifecycle (draft → listed → unlisted/swapped).
* Stores metadata, valuation snapshots, media references, and owner info.
* Publishes key events like `BOOK_CREATED`, `BOOK_MEDIA_FINALIZED`, and `BOOK_UNLISTED`.
* Provides REST endpoints for creating, retrieving, and managing books.

---

### **Media Service**

* Handles **image uploads** using pre-signed S3 flows.
* Stores metadata (file name, mimeType, size, owner, etc.).
* Publishes `MEDIA_STORED` events once uploads are confirmed.
* Provides view URLs for book images when requested by other services or the BFF.

---

### **Valuation Service**

* **No REST endpoints.**
* Subscribes to `BOOK_MEDIA_FINALIZED` events from the Catalog Service.
* For each event, retrieves the book data from Kafka, fetches image URLs from Media Service, and calls **Gemini API** to
  estimate the book’s value.
* Publishes the resulting price and confidence back to Kafka as a `VALUATION_READY` event.
* Catalog consumes this valuation and updates the book snapshot.

---

### **Swap Service**

* Manages the entire **swap lifecycle** (create, cancel, accept).
* Coordinates with:

    * **Catalog** (to reserve/unreserve books)
    * **Wallet** (to reserve/confirm BookCoins)
* Uses database transactions and pessimistic locking for reliability.
* Publishes domain events: `SWAP_CREATED`, `SWAP_CANCELLED`, `SWAP_COMPLETED`.
* Forms the central link between book ownership and BookCoin settlement.

---

### **Wallet Service**

* Handles **BookCoin balance and settlement**.
* Supports reservation, confirmation, and release of funds during swap flows.
* Ensures idempotent operations per `(userId, swapId)`.
* Processes `SWAP_COMPLETED` events to transfer coins between users.

---

### **Notification Service**

* Provides **in-app notifications** and real-time WebSocket updates.
* Consumes events from Swap and Catalog (e.g. `SWAP_CREATED`, `SWAP_COMPLETED`, `BOOK_UNLISTED`).
* Stores short-lived notification records and tracks unread counts.
* Pushes notifications live to connected users via WebSocket.

---

### **Email Service**

* **No REST endpoints.**
* Subscribes to:

    * `BOOK_CREATED` — saves the book owner’s email in its internal database.
    * `SWAP_COMPLETED` — retrieves both users involved and sends confirmation emails to each with swap details.
* Ensures reliable, idempotent email sending with retry and dead-letter handling.

---

### **BFF (Backend-for-Frontend)**

* Orchestrates and simplifies all client-facing flows.
* Combines data from Catalog, Media, Wallet, Notification, and Swap into optimized payloads.
* Powers all main frontend pages:

    * Home feed (recent books)
    * My Books
    * Book details & matches
    * Add Book (create + upload flow)
    * Swap Center (sent, received, book-specific requests)
* Handles authentication relay, ownership checks, and downstream timeouts.

---

## **Event Flow Overview (Simplified)**

1. **Book Created** → Catalog publishes `BOOK_CREATED`
   → Email stores owner email
   → Valuation waits for `BOOK_MEDIA_FINALIZED`.

2. **Media Uploaded** → Media publishes `MEDIA_STORED`
   → Catalog publishes `BOOK_MEDIA_FINALIZED`
   → Valuation fetches book, computes value, publishes `VALUATION_READY`.

3. **Swap Completed** → Swap publishes `SWAP_COMPLETED`
   → Wallet settles BookCoins
   → Notification & Email notify both users.

---

## **Development Notes**

* All services use **Kafka + Outbox pattern** for reliable, exactly-once event publishing.
* Authentication relayed through BFF (JWT).
* Observability includes structured logs and request tracing across services.
* Containerization & orchestration planned via **Docker Compose** for local and **Kubernetes (AWS)** for deployment.

### **Next Steps**

* Integrate a **Eureka Server** for **service discovery** and attach all microservices to it.
  This will allow each service to register itself dynamically and enable inter-service communication without hard-coded
  URLs.
  The BFF, Catalog, Swap, Wallet, and Notification services will discover each other through Eureka to simplify scaling
  and orchestration.

## Docker Commands

### Start Infrastructure (Postgres, Kafka)

```bash
cd infra
docker compose up -d
```

### Start Microservices

From the **root project directory**:

```bash
docker compose up -d --build
```

This will:

* Build catalog, media, valuation images
* Start all service containers
* Connect them to `bookswap-net`

### Stop All Microservice Containers (Keep Them)

```bash
docker compose stop
```

### Restart Stopped Containers

```bash
docker compose start
```

### Bring Down Microservices (Remove Containers)

```bash
docker compose down
```

> Note: External network and infrastructure containers remain running.

### Remove Containers AND Images

```bash
docker compose down --rmi all
```

### Rebuild After Code Changes

```bash
docker compose up -d --build
```

Or rebuild only one service:

```bash
docker compose up -d --build valuation-service
```

### View Logs

All services:

```bash
docker compose logs -f
```

Specific service:

```bash
docker compose logs -f valuation-service
```