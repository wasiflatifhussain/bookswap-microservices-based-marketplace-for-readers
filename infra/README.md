# BookSwap Infrastructure (Local Dev)

This folder contains the **shared infrastructure** for the BookSwap microservices, running via **Docker Compose**.

It provides:

* PostgreSQL (shared server, DB per service)
* Apache Kafka (single-broker, KRaft mode)
* Kafka topic auto-initialization
* Kafdrop (Kafka UI)

All data is **persistent across restarts** using Docker volumes.

---

## Services Overview

| Service    | Purpose                     | Host Port | Container Port |
|------------|-----------------------------|-----------|----------------|
| Postgres   | Shared database server      | 5432      | 5432           |
| Kafka      | Event streaming platform    | 19092     | 9092           |
| Kafdrop    | Kafka UI / monitoring       | 9000      | 9000           |
| kafka-init | One-shot topic creation job | —         | —              |

---

## Folder Structure

```
infra/
├── docker-compose.infra.yml
├── kafka/
│   └── init/
│       └── create-topics.sh
└── postgres/
    └── init/
        └── create-dbs.sql
```

---

## PostgreSQL Setup

### Image

* `postgres:16`

### Credentials

* **Username**: `bookswap`
* **Password**: `bookswap`
* **Maintenance DB**: `postgres`

### Databases (one per microservice)

Created automatically on first startup:

* `bookswap_catalog_db`
* `bookswap_email_db`
* `bookswap_media_db`
* `bookswap_notification_db`
* `bookswap_swap_db`
* `bookswap_valuation_db`
* `bookswap_wallet_db`

> DB creation is handled by:

```
postgres/init/create-dbs.sql
```

### Persistence

Postgres data is persisted using a Docker named volume:

```
pgdata → /var/lib/postgresql/data
```

---

## Kafka Setup

### Image

* `apache/kafka:latest`

### Mode

* **KRaft mode** (no Zookeeper)
* Single broker (dev setup)

### Ports

* **Inside Docker network**: `kafka:9092`
* **From host machine**: `localhost:19092`

> Port `9092` on host is intentionally NOT used
> (to avoid conflict with Homebrew Kafka)

---

## Kafka Topics (Auto-Created)

Topics are created automatically on startup **if they do not already exist**:

| Topic              | Partitions | Replication |
|--------------------|------------|-------------|
| `catalog.events`   | 3          | 1           |
| `media.events`     | 3          | 1           |
| `swap.events`      | 3          | 1           |
| `valuation.events` | 3          | 1           |

Topic creation is handled by a **one-shot init container**:

```
kafka-init
```

Script location:

```
kafka/init/create-topics.sh
```

This script is **idempotent**:

* Existing topics are skipped
* Topics persist across restarts via volume

### Persistence

Kafka data is persisted using:

```
kafkadata → /var/lib/kafka/data
```

---

## Kafdrop (Kafka UI)

### Access

```
http://localhost:9000
```

### Broker Connection

```
kafka:9092
```

Use Kafdrop to:

* View topics & partitions
* Inspect consumer groups
* Debug message flow

---

## Docker Volumes

Named volumes used:

* `pgdata` – PostgreSQL data
* `kafkadata` – Kafka logs & metadata

Volumes persist unless explicitly removed.

---

## Common Commands

### Start infrastructure

```bash
docker compose -f docker-compose.infra.yml up -d
```

### Stop infrastructure (keep data)

```bash
docker compose -f docker-compose.infra.yml down
```

### Stop infrastructure and DELETE ALL DATA

⚠️ **Dangerous – wipes DBs and Kafka topics**

```bash
docker compose -f docker-compose.infra.yml down -v
```

### View running containers

```bash
docker compose ps
```

### View logs

```bash
docker logs bookswap-postgres
docker logs bookswap-kafka
docker logs bookswap-kafka-init
```

### Re-run Kafka topic initialization

```bash
docker rm -f bookswap-kafka-init
docker compose -f docker-compose.infra.yml up -d kafka-init
```

---

## Connecting From Applications

### From Docker containers

* **Postgres**:

  ```
  jdbc:postgresql://bookswap-postgres:5432/<db_name>
  ```
* **Kafka**:

  ```
  kafka:9092
  ```

### From host machine

* **Postgres**: `localhost:5432`
* **Kafka**: `localhost:19092`

---

## Connecting with pgAdmin

Create a new server in pgAdmin:

* Host: `localhost`
* Port: `5432`
* Username: `bookswap`
* Password: `bookswap`
* Maintenance DB: `postgres`

---

## Design Notes

* Kafka and Postgres are **shared infra**, not per-microservice
* Each microservice owns its **own database**
* Kafka topics are shared and event-driven
* This setup mirrors production patterns and maps cleanly to Kubernetes later

---

If you want next, we can:

* add a **service-level compose** that connects to this infra
* document **event contracts**
* or convert this infra directly into **Kubernetes manifests**
