#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP="kafka:9092"
TOPICS=("catalog.events" "media.events" "swap.events" "valuation.events")
PARTITIONS=3
REPLICATION=1

echo "Waiting for Kafka at ${BOOTSTRAP}..."
# Wait until broker responds
for i in {1..60}; do
  if /opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list >/dev/null 2>&1; then
    echo "Kafka is up."
    break
  fi
  sleep 2
done

echo "Ensuring topics exist..."
for t in "${TOPICS[@]}"; do
  if /opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --describe --topic "$t" >/dev/null 2>&1; then
    echo "✓ Topic exists: $t"
  else
    echo "+ Creating topic: $t (partitions=${PARTITIONS}, replication=${REPLICATION})"
    /opt/kafka/bin/kafka-topics.sh \
      --create \
      --topic "$t" \
      --bootstrap-server "${BOOTSTRAP}" \
      --partitions "${PARTITIONS}" \
      --replication-factor "${REPLICATION}"
  fi
done

echo "Done."
