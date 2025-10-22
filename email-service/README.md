# Email Service — BookSwap Microservices

## 1. Overview

The **Email Service** listens to events from Kafka and performs specific actions for user communication in the BookSwap
platform.

It handles:

* `BOOK_CREATED` — updates user email information in the database
* `SWAP_COMPLETED` — sends a shared email thread to both users so they can coordinate their swap

---

## 2. Architecture

**Domain:** User communication and coordination

**Consumes Events From:**

* **Catalog Service** → `catalog.events` topic (group: `email-catalog-consumers`)
* **Swap Service** → `swap.events` topic (group: `email-swap-consumers`)

**Technology Stack:**

* Spring Boot with Kafka integration
* PostgreSQL database for email storage
* Resend API for email delivery
* Spring Retry for fault tolerance

---

## 3. Configuration

### Environment Variables

| Variable                  | Description                   | Example            |
|---------------------------|-------------------------------|--------------------|
| `RESEND_API_KEY`          | Resend API authentication key | `re_xxxxxxxxxxxxx` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses        | `localhost:9092`   |

### Database Schema

#### `emails` Table

| Column          | Type      | Constraints | Description               |
|-----------------|-----------|-------------|---------------------------|
| `user_id`       | VARCHAR   | PRIMARY KEY | User identifier           |
| `email_address` | VARCHAR   | NOT NULL    | User's email address      |
| `created_at`    | TIMESTAMP | NOT NULL    | Record creation timestamp |
| `updated_at`    | TIMESTAMP | NOT NULL    | Last update timestamp     |

**Upsert Logic:**

```sql
INSERT INTO emails (user_id, email_address, created_at, updated_at)
VALUES (:ownerUserId, :ownerEmail, NOW(), NOW())
ON CONFLICT (user_id)
DO UPDATE SET email_address = EXCLUDED.email_address, updated_at = NOW()
```

---

## 4. Email Provider — Resend

### Domain Setup

1. **Domain Purchase**: Purchased `bookswap.work` from Namecheap (1 year)
2. **Resend Configuration**:

    * Obtained API key from Resend dashboard
    * Configured as environment variable: `RESEND_API_KEY`
    * Added domain `bookswap.work` to Resend
3. **DNS Configuration**:

    * Resend provided **MX** and **TXT** records for verification
    * Added these records to Namecheap dashboard
    * Verified domain ownership in Resend
4. **Email Configuration**:

    * From address: `BookSwap <no-reply@bookswap.work>`
    * Custom headers: `List-Id` and `X-BookSwap-Swap-Id` for tracking

### Implementation Details

**Resend Client Bean:**

```java

@Bean
public Resend resend(@Value("${email.resend.api-key}") String apiKey) {
  return new Resend(apiKey);
}
```

**Email Sending Flow:**

1. Constructs HTML and plain text email body
2. Sends via Resend API with both user emails in TO field
3. Returns boolean success/failure status
4. Exceptions are caught and returned as `false` to trigger retry logic

---

## 5. Event Processing

### BookCreatedEvent (from Catalog Service)

| Field         | Description                        |
|---------------|------------------------------------|
| `bookId`      | ID of the created book             |
| `ownerUserId` | ID of the user who listed the book |
| `ownerEmail`  | Email of the book owner            |

**Action:**

* Does **not** send any email
* Performs an **upsert** operation: `INSERT ... ON CONFLICT DO UPDATE`
* Stores `(user_id, email_address)` in the `emails` table for future swap coordination
* Updates email address if user already exists

---

### SwapCompletedEvent (from Swap Service)

| Field             | Description                          |
|-------------------|--------------------------------------|
| `swapId`          | ID of the completed swap             |
| `requesterUserId` | ID of the requester swap participant |
| `responderUserId` | ID of the responder swap participant |
| `requesterBookId` | Book owned by Requester              |
| `responderBookId` | Book owned by Responder              |

**Action:**

* Retrieves both users' emails from the `emails` table
* Sends one **shared email** with both addresses in the **TO** field
* Includes swap details and safety reminders
* Implements retry logic for delivery guarantees

---

## 6. Email Template

When a swap is completed, both users receive an email with:

* **Subject:** "BookSwap – Swap {swapId} is SUCCESSFUL!"
* **Content:**

    * Confirmation message with swap ID
    * Both participants' email addresses
    * Instructions to use Reply-All for coordination
    * Safety reminders (public meeting place, verify items)
    * BookSwap branding and footer
* **Format:** HTML with plain text fallback

---

## 7. Retry Mechanism

The service implements **Spring Retry** with exponential backoff to handle transient failures.

### Configuration

* **Max Attempts:** 3
* **Initial Delay:** 1000ms (1 second)
* **Multiplier:** 2x
* **Max Delay:** 5000ms (5 seconds)
* **Retry Schedule:** 0s → 1s → 2s

### Retry Triggers

**Database Read Failures:**

* Connection timeouts
* Query execution errors
* Transient database issues

**Email Send Failures:**

* Resend API errors
* Network timeouts
* Rate limiting responses

### Recovery

After 3 failed attempts, the `@Recover` method is invoked:

* Logs final failure with swap ID and error details
* Could be extended to publish dead-letter events for manual intervention

---