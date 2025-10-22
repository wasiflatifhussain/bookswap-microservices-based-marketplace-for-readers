import json
import uuid
from datetime import datetime
from kafka import KafkaProducer

# Kafka configuration
KAFKA_BROKER = "localhost:9092"  # change if running remotely
TOPIC = "catalog.events"

# Initialize Kafka producer
producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    key_serializer=lambda v: v.encode("utf-8"),
)


# Helper to build an event
def build_book_created_event(book_id, title, description, author, condition, user_id, email):
    event = {
        "bookId": book_id,
        "title": title,
        "description": description,
        "author": author,
        "bookCondition": condition,
        "ownerUserId": user_id,
        "ownerEmail": email,
    }
    headers = [
        ("aggregateId", book_id.encode()),
        ("aggregateType", b"BOOK"),
        ("eventType", b"BOOK_CREATED"),
        ("outboxEventId", str(uuid.uuid4()).encode()),
    ]
    return event, headers


# Two sample events
books = [
    (
        "Set Boundaries, Find Peace",
        "A practical self-help guide offering strategies to set healthy boundaries, improve relationships, and find emotional balance. Condition: relatively new hardback with a fresh new book smell and no damaged pages.",
        "Nedra Glover Tawwab",
        "LIKE_NEW",
        "37c578c6-d3b4-43d4-af68-90919682a2d4",
        "md.wasiflatif9859@gmail.com",
    ),
    (
        "The Psychology of Money",
        "A personal finance classic that explores how behavior affects financial decisions. Condition: well-kept paperback, minimal highlights, no torn pages.",
        "Morgan Housel",
        "VERY_GOOD",
        "44a128c9-0ac2-4a61-9b02-5f7a71ab2298",
        "wasiflh@connect.hku.hk",
    ),
]

# Produce both events
for title, desc, author, condition, user_id, email in books:
    book_id = str(uuid.uuid4())
    event, headers = build_book_created_event(book_id, title, desc, author, condition, user_id, email)
    producer.send(
        TOPIC,
        key=book_id,
        value=event,
        headers=headers,
    )
    print(f"✅ Sent BOOK_CREATED event for {title} ({book_id})")

# Flush and close
producer.flush()
producer.close()

print("🎯 All events successfully pushed to catalog.events.")
