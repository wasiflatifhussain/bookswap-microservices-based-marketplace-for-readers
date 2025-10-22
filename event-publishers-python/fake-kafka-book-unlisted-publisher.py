# publish_book_unlisted.py
import json
import sys
from kafka import KafkaProducer

BOOTSTRAP = "localhost:9092"
TOPIC = "catalog.events"


def main():
    if len(sys.argv) < 2:
        print("Usage: python publish_book_unlisted.py <bookId> [valuation] [ownerUserId]")
        sys.exit(1)

    book_id = sys.argv[1]
    valuation = float(sys.argv[2]) if len(sys.argv) >= 3 else 42.0
    owner_user_id = sys.argv[3] if len(sys.argv) >= 4 else "user-123"

    payload = {
        "bookId": book_id,
        "ownerUserId": owner_user_id,
        "valuation": valuation,
    }

    producer = KafkaProducer(
        bootstrap_servers=[BOOTSTRAP],
        key_serializer=lambda k: k.encode("utf-8"),
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )

    headers = [
        ("eventType", b"BOOK_UNLISTED"),
        ("source", b"manual-test"),
    ]

    fut = producer.send(TOPIC, key=book_id, value=payload, headers=headers)
    fut.get(timeout=10)
    producer.flush()
    producer.close()

    print(f"Sent BOOK_UNLISTED for bookId={book_id}")


if __name__ == "__main__":
    main()
