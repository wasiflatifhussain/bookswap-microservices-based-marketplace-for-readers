# # for BOOK_MEDIA_FINALIZED event
# import json
# import time
# import uuid
# from kafka import KafkaProducer
#
# # Kafka setup
# KAFKA_BROKER = "localhost:9092"
# TOPIC = "catalog.events"
#
# # Create Kafka producer
# producer = KafkaProducer(
#     bootstrap_servers=[KAFKA_BROKER],
#     value_serializer=lambda v: json.dumps(v).encode("utf-8"),
#     key_serializer=lambda k: k.encode("utf-8") if k else None,
# )
#
# # Example event payload (structure same as your Kafdrop screenshot)
# event = {
#     "bookId": str(uuid.uuid4()),
#     "title": "The Great Gatsby",
#     "description": "Classic American novel by F. Scott Fitzgerald",
#     "genre": "CLASSIC",
#     "author": "F. Scott Fitzgerald",
#     "bookCondition": "GOOD",
#     "valuation": 0,
#     "bookStatus": "AVAILABLE",
#     "mediaIds": [
#         str(uuid.uuid4()),
#         str(uuid.uuid4()),
#         str(uuid.uuid4())
#     ],
#     "ownerUserId": str(uuid.uuid4()),
#     "createdAt": "2025-10-06T12:00:00.000",
#     "updatedAt": "2025-10-06T12:00:00.000"
# }
#
# # Kafka headers — match your system
# headers = [
#     ("aggregateType", b"BOOK"),
#     ("eventType", b"BOOK_MEDIA_FINALIZED"),
#     ("outboxEventId", str(uuid.uuid4()).encode("utf-8")),
# ]
#
# # Publish event
# producer.send(
#     topic=TOPIC,
#     key=event["bookId"],
#     value=event,
#     headers=headers
# )
#
# producer.flush()
# print(f"✅ Sent BOOK_MEDIA_FINALIZED event for bookId={event['bookId']} to topic {TOPIC}")
# time.sleep(1)
# producer.close()

# for BOOK_UNLISTED event
import json
import time
import uuid
from kafka import KafkaProducer

KAFKA_BROKER = "localhost:9092"
TOPIC = "catalog.events"

producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    key_serializer=lambda k: k.encode("utf-8") if k else None,
)

# Use your provided bookId and ownerUserId
event = {
    "bookId": "242b2fd9-0f0b-454a-8aa0-9e4186cc400c",
    "ownerUserId": "d3576497-16b9-4322-b97c-6849880da383"
}

headers = [
    ("aggregateType", b"BOOK"),
    ("eventType", b"BOOK_UNLISTED"),
    ("outboxEventId", str(uuid.uuid4()).encode("utf-8")),
]

producer.send(
    topic=TOPIC,
    key=event["bookId"],
    value=event,
    headers=headers
)

producer.flush()
print(f"✅ Sent BOOK_UNLISTED event for bookId={event['bookId']} to topic {TOPIC}")
time.sleep(1)
producer.close()
