# publish_swap_created.py
import json
import uuid
from kafka import KafkaProducer

BOOTSTRAP = "localhost:9092"
SWAP_TOPIC = "swap.events"


def b(s: str) -> bytes:
    return s.encode("utf-8")


def main():
    with open("swap_fixture.json") as fp:
        fx = json.load(fp)

    swap_id = f"swap-{uuid.uuid4()}"

    payload = {
        "swapId": swap_id,
        "requesterUserId": fx["requesterUserId"],
        "responderUserId": fx["responderUserId"],
        "requesterBookId": fx["requesterBookId"],
        "responderBookId": fx["responderBookId"],
        # your DTO also has responderBookName, but your listener/service doesn't use it,
        # so we can omit or add a dummy if you prefer:
        # "responderBookName": "Clean Architecture",
    }

    producer = KafkaProducer(
        bootstrap_servers=[BOOTSTRAP],
        key_serializer=lambda k: k.encode("utf-8"),
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )

    headers = [("eventType", b("SWAP_CREATED")), ("source", b("manual-test"))]
    fut = producer.send(SWAP_TOPIC, key=swap_id, value=payload, headers=headers)
    fut.get(timeout=10)
    producer.flush()
    producer.close()

    fx["swapId"] = swap_id
    with open("swap_fixture.json", "w") as fp:
        json.dump(fx, fp, indent=2)

    print(f"Sent SWAP_CREATED for swapId={swap_id}")
    print("Updated swap_fixture.json with the swapId.")
    print("\nNow run:  python publish_swap_completed.py")


if __name__ == "__main__":
    main()
