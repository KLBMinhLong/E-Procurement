# Kafka Topics Registry

| Topic | Publisher | Consumers | Event |
|---|---|---|---|
| procurement.pr.submitted | pr-service | approval-service, notification-service | PR submitted |
| procurement.pr.approved | approval-service | pr-service, finance-service, notification-service | PR approved |
| procurement.pr.rejected | approval-service | pr-service, notification-service | PR rejected |
| procurement.pr.changes-requested | approval-service | pr-service, notification-service | PR changes requested |
| procurement.pr.cancelled | pr-service | approval-service, finance-service, notification-service | PR cancelled |
| approval.step.assigned | approval-service | notification-service | Step assigned |
| approval.step.completed | approval-service | approval-service (next step), pr-service | Step completed |
| approval.sla.warning | approval-service | notification-service | SLA warning (50%, 75%) |
| approval.sla.breached | approval-service | notification-service, admin-service | SLA breached |
| approval.escalated | approval-service | notification-service, admin-service | Approval escalated |
| finance.budget.warning | finance-service | notification-service | Budget low |
| finance.budget.exceeded | finance-service | pr-service, notification-service | Budget exceeded |
| procurement.po.issued | finance-service | inventory-service, notification-service | PO issued |
| inventory.gr.created | inventory-service | finance-service, notification-service | GR created |
| inventory.stock.low | inventory-service | notification-service | Stock low |
| notification.email.send | any service | notification-service | Send email |
| finance.invoice.matched | finance-service | notification-service | 3-way match OK |
| procurement.emergency.abuse | pr-service | notification-service, admin-service | Emergency PR abuse warning |
