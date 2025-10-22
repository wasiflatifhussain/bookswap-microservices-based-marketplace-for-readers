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
    "bookId": "86ceb6fa-7db1-48a6-8fee-cdf3704e252e",
    "title": "1984",
    "description": "A dystopian novel by George Orwell set in a totalitarian society under constant surveillance, exploring themes of freedom, truth, and oppression. This is an original copy of the book in like-new condition; it still has the fresh, new book smell and features a soft back cover. There are no writings, marks, or stains on any pages.",
    "genre": "HISTORICAL_FICTION",
    "author": "George Orwell",
    "bookCondition": "LIKE_NEW",
    "valuation": 0,
    "bookStatus": "AVAILABLE",
    "mediaIds": [
        "7cf331ec-2800-4a9a-ae93-1586ef1500ea",
        "24ed4656-0f93-442a-a91a-e3fbf0579896",
        "75842ac1-ef72-4dfa-b4a5-d000b10eacc3"
    ],
    "ownerUserId": "b10b357d-ca4d-4c6f-ab27-7b73b8ab9308",
    "createdAt": "2025-10-10T12:08:58.027817",
    "updatedAt": "2025-10-10T12:08:58.027857"
}

headers = [
    ("aggregateId", event["bookId"].encode("utf-8")),
    ("aggregateType", b"BOOK"),
    ("eventType", b"BOOK_MEDIA_FINALIZED"),
    ("outboxEventId", b"4ad0d88b-7c45-4171-9976-9f19eb4bd353"),
]

producer.send(
    topic=TOPIC,
    key=event["bookId"],
    value=event,
    headers=headers
)

producer.flush()
print(f"✅ Sent BOOK_VALUATION_FINALIZED event for bookId={event['bookId']} to topic {TOPIC}")
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
#     "bookId": "de68f384-9d89-4ffd-9f2c-040b201499df",
#     "ownerUserId": "b10b357d-ca4d-4c6f-ab27-7b73b8ab9308",
#     "valuation": 5.452
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
