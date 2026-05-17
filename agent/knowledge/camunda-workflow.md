# Camunda Workflow

## Decision
- Approval Engine embeds Camunda BPMN 7.
- Workflow definitions are BPMN files.
- Camunda schema is stored in db_camunda.camunda.

## Core processes
- pr-approval-process.bpmn
- rfq-process.bpmn
- po-process.bpmn
- emergency-approval.bpmn

## Responsibilities
- Task assignment and routing
- Sequential/parallel steps
- SLA timers and escalation
