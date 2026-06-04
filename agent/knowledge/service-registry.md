# Service Registry

| Service Key | Module Name | Port | DB | Schema(s) | Package Root | Status |
|---|---|---|---|---|---|---|
| iam | iam-service | 8081 | db_iam | iam | com.eprocure.iam | Active dev |
| pr | pr-service | 8082 | db_procurement | pr | com.eprocure.pr | Active dev |
| approval | approval-service | 8083 | db_procurement | approval, camunda | com.eprocure.approval | Planned |
| finance | finance-service | 8084 | db_finance | finance | com.eprocure.finance | Planned |
| inventory | inventory-service | 8085 | db_inventory | inventory | com.eprocure.inventory | Foundation |
| vendor | vendor-service | 8086 | db_vendor | vendor | com.eprocure.vendor | Planned |
| analytics | analytics-service | 8087 | db_analytics | analytics | com.eprocure.analytics | Foundation |
| notification | notification-service | 8088 | db_notification | notification | com.eprocure.notification | Planned |
| admin | admin-service | 8089 | db_audit | audit | com.eprocure.admin | Planned |
| gateway | api-gateway | 8080 | - | - | com.eprocure.gateway | Active dev |
| frontend | eprocure-web | 4200 dev / 80 prod | - | - | Angular app | Active dev |
