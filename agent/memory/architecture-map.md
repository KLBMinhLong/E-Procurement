# architecture-map.md
## eProcure Enterprise — Sơ Đồ Quan Hệ Giữa Services

---

## 1. SERVICE DEPENDENCY GRAPH

```
                    ┌─────────────────────┐
                    │   Angular Frontend   │
                    │  (NGINX :80/:443)    │
                    └──────────┬──────────┘
                               │ HTTPS
                    ┌──────────▼──────────┐
                    │    API GATEWAY       │
                    │   (NGINX / Spring)   │
                    │  Verify token        │
                    │  Inject X-Api-Key    │
                    │  Rate limit          │
                    └─┬──────┬──────┬─────┘
           REST        │      │      │        REST (x-api-key)
     ┌─────────────────┘      │      └──────────────────────┐
     │                        │                             │
┌────▼────────┐    ┌──────────▼──────────┐    ┌────────────▼───────┐
│ IAM Service │    │    PR Service        │    │  Approval Engine   │
│  :8081      │◄───│    :8082            │───▶│  :8083 (Camunda)   │
│             │    │  - Budget check     │    │                    │
│ Token verify│    │    → Finance        │    │  SLA, Escalation   │
│ Permission  │    │  - Inventory check  │    │  BPMN workflow     │
│ Org chart   │    │    → Inventory      │    └──────────┬─────────┘
└─────────────┘    └──────────┬──────────┘               │
                              │                           │
                     Kafka Events                   Kafka Events
                              │                           │
          ┌───────────────────┼────────────────────────── ┤
          │                   │                           │
┌─────────▼──────┐  ┌─────────▼──────┐  ┌───────────────▼──────┐
│ Finance Service│  │Inventory Service│  │ Notification Service  │
│ :8084          │  │ :8085           │  │ :8088                 │
│                │  │                 │  │                       │
│ Budget, PO     │  │ Catalog, Stock  │  │ Email (Brevo)         │
│ Invoice        │  │ GR, Movements   │  │ In-app                │
│ 3-Way Match    │  │                 │  │ WebSocket/STOMP       │
└────────────────┘  └─────────────────┘  └───────────────────────┘
          │                   │
┌─────────▼──────┐  ┌─────────▼──────┐
│ Vendor Service │  │Analytics Service│
│ :8086          │  │ :8087           │
│                │  │                 │
│ Vendor, AVL    │  │ Dashboard, KPI  │
│ RFQ, Quotes    │  │ JasperReport    │
└────────────────┘  └─────────────────┘

┌──────────────────────────────────────┐
│          Admin Service :8089          │
│  System Config, Audit Log, Health     │
│  (Tách biệt với hệ thống nghiệp vụ) │
└──────────────────────────────────────┘
```

---

## 2. DATA FLOW — PR LIFECYCLE

```
1. Tạo PR (DRAFT)
   FE → Gateway → PR Service
   PR Service → Finance Service [GET budget]
   PR Service → Inventory Service [GET stock]

2. Submit PR
   PR Service → Approval Service [Start BPMN process]
   PR Service → Kafka [procurement.pr.submitted]
   Kafka → Notification Service [Gửi email cho approver]

3. Phê duyệt từng bước
   Approver → Gateway → Approval Service
   Approval Service → IAM Service [Verify approver permission]
   Approval Service → Kafka [approval.step.assigned] (nếu có bước tiếp)
   Kafka → Notification Service [Email cho approver tiếp theo]

4. PR Approved hoàn toàn
   Approval Service → Kafka [procurement.pr.approved]
   Kafka → PR Service [Update status = APPROVED]
   Kafka → Finance Service [Commit budget]
   Kafka → Notification Service [Email cho requester]

5. Tạo PO
   Purchasing → Gateway → Finance Service [POST /purchase-orders]
   Finance Service → PR Service [GET /internal/purchase-requests/{id}/po-source]
   Finance Service → Vendor Service [GET /internal/vendors/{id}/po-source]
   Finance Service → PR Service [PATCH /internal/purchase-requests/{id}/converted-to-po]

6. Gửi PO cho Vendor
   Finance Service → Notification Service [Email PO đến vendor]

7. Nhận hàng (GR)
   Warehouse → Gateway → Inventory Service [POST /goods-receipts]
   Inventory Service → Kafka [inventory.gr.created]
   Kafka → Finance Service [Trigger 3-way match]

8. Thanh toán
   Accountant → Gateway → Finance Service [confirm-payment]
   Finance Service → Finance Service [Committed → Spent]
   Finance Service → Kafka [finance.invoice.matched]
```

---

## 3. INFRASTRUCTURE TOPOLOGY

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                      │
│  [All Java services] ──connect──▶ [PostgreSQL]           │
│  [All Java services] ──connect──▶ [Redis]                │
│  [Event-driven services] ──connect──▶ [Kafka]            │
│  [IAM Service] ──connect──▶ [Keycloak]                   │
│  [Notification] ──connect──▶ [Brevo SMTP]                │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                   Observability Stack                     │
│  [All services] ──OTel Agent──▶ [Tempo] ←── [Grafana]   │
│  [All services] ──Micrometer──▶ [Prometheus] ←─ ─ ─ ─ ┘ │
│  [All services] ──Log4j2 JSON──▶ [Loki] ←── [Grafana]   │
└─────────────────────────────────────────────────────────┘
```

---

## 4. NETWORK PORTS MAP (Dev)

```
80/443   → NGINX (Frontend)
8080     → API Gateway
8081     → IAM Service
8082     → PR Service
8083     → Approval Service (+ Camunda REST: /camunda)
8084     → Finance Service
8085     → Inventory Service
8086     → Vendor Service
8087     → Analytics Service
8088     → Notification Service (WS: /ws/notifications)
8089     → Admin Service
8180     → Keycloak Admin Console
5432     → PostgreSQL
6379     → Redis
9092     → Kafka
9090     → Prometheus
3000     → Grafana
3100     → Loki
4317     → Tempo (OTLP gRPC)
```

---

## 5. CROSS-SERVICE CALL RULES

```
✅ Sync (REST): Chỉ khi cần data NGAY để trả response
   VD: PR Service gọi Finance để check budget trước khi submit

✅ Async (Kafka): Khi không cần kết quả ngay, side effects
   VD: Sau khi PR approved → Kafka event → Finance commit budget

❌ KHÔNG gọi qua cùng DB (cross-schema join trực tiếp)
   Mỗi service chỉ đọc/ghi vào DB của mình

❌ KHÔNG gọi trực tiếp không qua Gateway từ FE
   Mọi request FE đều qua Gateway

✅ Inter-service call phải có X-Api-Key header
   Gateway inject tự động → Service validate

✅ Truyền X-Request-ID (trace ID) xuyên suốt
   OTel agent tự động propagate
```
