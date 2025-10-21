# publish_swap_completed.py
import json
from kafka import KafkaProducer

BOOTSTRAP = "localhost:9092"
SWAP_TOPIC = "swap.events"


def b(s: str) -> bytes:
    return s.encode("utf-8")


def main():
    with open("swap_fixture.json") as fp:
        fx = json.load(fp)

    required = ["swapId", "requesterUserId", "responderUserId", "requesterBookId", "responderBookId"]
    missing = [k for k in required if k not in fx]
    if missing:
        raise SystemExit(
            f"swap_fixture.json missing keys: {missing}. Run setup_fixture.py and publish_swap_created.py first.")

    payload = {
        "swapId": fx["swapId"],
        "requesterUserId": fx["requesterUserId"],
        "responderUserId": fx["responderUserId"],
        "requesterBookId": fx["requesterBookId"],
        "responderBookId": fx["responderBookId"],
    }

    producer = KafkaProducer(
        bootstrap_servers=[BOOTSTRAP],
        key_serializer=lambda k: k.encode("utf-8"),
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )

    headers = [("eventType", b("SWAP_COMPLETED")), ("source", b("manual-test"))]
    fut = producer.send(SWAP_TOPIC, key=fx["swapId"], value=payload, headers=headers)
    fut.get(timeout=10)
    producer.flush()
    producer.close()

    print(f"Sent SWAP_COMPLETED for swapId={fx['swapId']}")


if __name__ == "__main__":
    main()
