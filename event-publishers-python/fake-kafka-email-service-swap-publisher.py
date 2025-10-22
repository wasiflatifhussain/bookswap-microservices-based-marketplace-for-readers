import json
from kafka import KafkaProducer

# Kafka broker & topic
BROKER = "localhost:9092"  # change if not local
TOPIC = "swap.events"

# Swap and book/user info
payload = {
    "swapId": "swap-67fe032b-06bb-428c-9ae3-73000b4d88cf",
    "requesterUserId": "37c578c6-d3b4-43d4-af68-90919682a2d4",  # md.wasiflatif9859@gmail.com
    "responderUserId": "44a128c9-0ac2-4a61-9b02-5f7a71ab2298",  # wasiflh@connect.hku.hk
    "requesterBookId": "book-req-93e6b124-fdb8-4040-821a-a18009d427a5",
    "responderBookId": "book-resp-7e7a8b66-c407-4c4e-b12b-e1acfe942292",
}

headers = [
    ("eventType", b"SWAP_COMPLETED"),
    ("source", b"manual-test"),
]

# Kafka producer setup
producer = KafkaProducer(
    bootstrap_servers=[BROKER],
    value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    key_serializer=lambda v: v.encode("utf-8"),
)

# Send event
producer.send(
    TOPIC,
    key=payload["swapId"],
    value=payload,
    headers=headers,
)

producer.flush()
producer.close()

print(f"🎉 Sent SWAP_COMPLETED event for {payload['swapId']}")
