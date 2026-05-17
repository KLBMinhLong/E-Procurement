#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
PARTITIONS="${KAFKA_TOPIC_PARTITIONS:-3}"
REPLICATION_FACTOR="${KAFKA_TOPIC_REPLICATION_FACTOR:-1}"

topics=(
  "procurement.pr.submitted"
  "procurement.pr.approved"
  "procurement.pr.rejected"
  "procurement.pr.changes-requested"
  "procurement.pr.cancelled"
  "approval.step.assigned"
  "approval.step.completed"
  "approval.sla.warning"
  "approval.sla.breached"
  "approval.escalated"
  "finance.budget.warning"
  "finance.budget.exceeded"
  "procurement.po.issued"
  "inventory.gr.created"
  "inventory.stock.low"
  "finance.invoice.matched"
  "notification.email.send"
  "procurement.emergency.abuse"
)

for topic in "${topics[@]}"; do
  kafka-topics \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions "${PARTITIONS}" \
    --replication-factor "${REPLICATION_FACTOR}"
done

kafka-topics --bootstrap-server "${BOOTSTRAP_SERVER}" --list
