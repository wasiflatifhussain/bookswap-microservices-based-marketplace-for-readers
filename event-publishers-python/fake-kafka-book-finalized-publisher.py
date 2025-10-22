# setup_fixture.py
import json
import uuid
from kafka import KafkaProducer

BOOTSTRAP = "localhost:9092"
CATALOG_TOPIC = "catalog.events"


def b(s: str) -> bytes:
    return s.encode("utf-8")


def main():
    # make deterministic-ish but unique ids
    requester_user_id = f"user-req-{uuid.uuid4().hex[:8]}"
    responder_user_id = f"user-resp-{uuid.uuid4().hex[:8]}"
    requester_book_id = f"book-req-{uuid.uuid4()}"
    responder_book_id = f"book-resp-{uuid.uuid4()}"

    producer = KafkaProducer(
        bootstrap_servers=[BOOTSTRAP],
        key_serializer=lambda k: k.encode("utf-8"),
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )

    # 1) BOOK_VALUATION_FINALIZED for requester book
    finalized_req = {
        "bookId": requester_book_id,
        "title": "Algorithms Illuminated",
        "description": "Requester’s test book",
        "author": "Jane Doe",
        "bookCondition": "LIKE_NEW",
        "valuation": 25.0,
        "bookStatus": "FINALIZED",
        "mediaIds": [],
        "ownerUserId": requester_user_id,
    }
    headers = [("eventType", b("BOOK_VALUATION_FINALIZED")), ("source", b("manual-test"))]
    f1 = producer.send(CATALOG_TOPIC, key=requester_book_id, value=finalized_req, headers=headers)
    f1.get(timeout=10)

    # 2) BOOK_VALUATION_FINALIZED for responder book
    finalized_resp = {
        "bookId": responder_book_id,
        "title": "Clean Architecture",
        "description": "Responder’s test book",
        "author": "John Smith",
        "bookCondition": "GOOD",
        "valuation": 30.0,
        "bookStatus": "FINALIZED",
        "mediaIds": [],
        "ownerUserId": responder_user_id,
    }
    f2 = producer.send(CATALOG_TOPIC, key=responder_book_id, value=finalized_resp, headers=headers)
    f2.get(timeout=10)

    producer.flush()
    producer.close()

    fixture = {
        "requesterUserId": requester_user_id,
        "responderUserId": responder_user_id,
        "requesterBookId": requester_book_id,
        "responderBookId": responder_book_id,
        # swapId will be added by the next script
    }
    with open("../swap_fixture.json", "w") as fp:
        json.dump(fixture, fp, indent=2)

    print("Created fixture and sent BOOK_VALUATION_FINALIZED events:")
    print(json.dumps(fixture, indent=2))


if __name__ == "__main__":
    main()
