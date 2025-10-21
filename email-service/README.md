# Email Service — BookSwap Microservices

## Overview

The **Email Service** listens to events from Kafka and performs specific actions for user communication in the BookSwap
platform.

It handles:

* `BOOK_CREATED` — updates user email information in the database.
* `SWAP_COMPLETED` — sends a shared email thread to both users so they can coordinate their swap.

---

## Domains and Responsibilities

**Domain:** User communication and coordination
**Consumes Events From:**

* **Catalog Service** → `BOOK_CREATED`
* **Swap Service** → `SWAP_COMPLETED`

**Responsibilities**

* Maintain a record of user emails for future lookups.
* When a swap is completed, retrieve both users’ emails and send them a shared thread to discuss their trade.

---

## Event Objects

### BookCreatedEvent

| Field         | Description                        |
|---------------|------------------------------------|
| `bookId`      | ID of the created book             |
| `ownerUserId` | ID of the user who listed the book |
| `ownerEmail`  | Email of the book owner            |

**Action:**
Does **not** send any email.
Upserts `(user_id, email_address)` into the database so the service can later contact users during swaps.

---

### SwapCompletedEvent

| Field     | Description                       |
|-----------|-----------------------------------|
| `userAId` | ID of the first swap participant  |
| `userBId` | ID of the second swap participant |
| `bookAId` | Book owned by User A              |
| `bookBId` | Book owned by User B              |

**Action:**

* Retrieves both users’ emails from the `emails` table.
* Sends one **shared email** using both addresses in the **TO** field.
* Allows them to coordinate directly about meeting, trading, or exchanging books.

---

## Data and Privacy

* The service stores only user IDs and email addresses for lookup.
* No other personal or book data is persisted.
* Emails are used solely for coordination purposes and not exposed to other users or services.

---

## Email Sending Methods

### Option 1 — SMTP (Development)

* Uses standard mail configuration (`spring-boot-starter-mail`).
* Suitable for local or simple environments.
* Requires basic SMTP credentials.

**Pros:** easy setup, no external dependency.
**Cons:** limited reliability and observability.

---

### Option 2 — Provider API (Production)

* Uses cloud-based email APIs such as **AWS SES**, **SendGrid**, **Mailgun**, or **Postmark**.
* Recommended for production environments for reliability, deliverability, and tracking.

**Pros:** high deliverability, reputation management, bounce handling, analytics.
**Cons:** requires API setup and keys.

---

## Summary of Actions

| Event            | Source Service  | Action                                                                 |
|------------------|-----------------|------------------------------------------------------------------------|
| `BOOK_CREATED`   | Catalog Service | Upserts `(user_id, email)` into DB (no email sent)                     |
| `SWAP_COMPLETED` | Swap Service    | Looks up both users’ emails and sends one shared email with both in TO |

---

## Checklist

* [ ] Kafka topics and group IDs configured correctly.
* [ ] Database table `emails` exists with `user_id` as the primary key.
* [ ] SMTP or API provider configured for sending.
* [ ] Privacy policy aligned with email storage.

---

This README precisely documents the Email Service’s purpose, event handling, and operational modes without unnecessary
implementation details.
