import json
import time
from kafka import KafkaProducer

KAFKA_BROKER = "localhost:9092"
TOPIC = "catalog.events"

producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    key_serializer=lambda k: k.encode("utf-8") if k else None,
)

event = {
    "bookId": "de68f384-9d89-4ffd-9f2c-040b201499df",
    "title": "The Hobbit",
    "description": "A fantasy novel about Bilbo Baggins, a hobbit who embarks on an unexpected adventure with a group of dwarves to reclaim their mountain home from a dragon. The book is relatively old and has been read many times but has been well taken care of. It is an original copy with a soft cover, showing some wear on the spine and slight yellowing of pages, but all pages are intact with no writing or stains.",
    "genre": "FANTASY",
    "author": "J.R.R. Tolkien",
    "bookCondition": "FAIR",
    "valuation": 0,
    "bookStatus": "AVAILABLE",
    "mediaIds": [
        "4bcc1a56-d6a2-4493-a678-64980898cd16",
        "7e674d1b-8827-43fa-9cf4-4e9d2814a562",
        "1faac308-ac7e-4fef-a110-16ac75f16390",
        "6a531acc-4af3-4d14-b7f1-6b2138280829",
        "cafe307e-d669-491b-85c2-f2decaf47dae",
        "a28ce490-5b1c-420c-973e-0e828323d571"
    ],
    "ownerUserId": "b10b357d-ca4d-4c6f-ab27-7b73b8ab9308",
    "createdAt": "2025-10-08T00:01:46.789808",
    "updatedAt": "2025-10-08T00:01:46.789853"
}

headers = [
    ("aggregateId", event["bookId"].encode("utf-8")),
    ("aggregateType", b"BOOK"),
    ("eventType", b"BOOK_MEDIA_FINALIZED"),
    ("outboxEventId", b"9060516c-0fc2-4d3b-97ad-8cb1ed9fc1ed"),
]

producer.send(
    topic=TOPIC,
    key=event["bookId"],
    value=event,
    headers=headers
)

producer.flush()
print(f"✅ Sent BOOK_MEDIA_FINALIZED event for bookId={event['bookId']} to topic {TOPIC}")
time.sleep(1)
producer.close()

# # for BOOK_UNLISTED event
# import json
# import time
# import uuid
# from kafka import KafkaProducer
#
# KAFKA_BROKER = "localhost:9092"
# TOPIC = "catalog.events"
#
# producer = KafkaProducer(
#     bootstrap_servers=[KAFKA_BROKER],
#     value_serializer=lambda v: json.dumps(v).encode("utf-8"),
#     key_serializer=lambda k: k.encode("utf-8") if k else None,
# )
#
# # Use your provided bookId and ownerUserId
# event = {
#     "bookId": "242b2fd9-0f0b-454a-8aa0-9e4186cc400c",
#     "ownerUserId": "d3576497-16b9-4322-b97c-6849880da383"
# }
#
# headers = [
#     ("aggregateType", b"BOOK"),
#     ("eventType", b"BOOK_UNLISTED"),
#     ("outboxEventId", str(uuid.uuid4()).encode("utf-8")),
# ]
#
# producer.send(
#     topic=TOPIC,
#     key=event["bookId"],
#     value=event,
#     headers=headers
# )
#
# producer.flush()
# print(f"✅ Sent BOOK_UNLISTED event for bookId={event['bookId']} to topic {TOPIC}")
# time.sleep(1)
# producer.close()
