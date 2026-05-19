# progress-tracker.md
## eProcure Enterprise — Tiến Độ Epics & Tasks

> Cập nhật file này mỗi khi hoàn thành một epic hoặc phát hiện blockers.  
> Status: ⬜ Chưa làm | 🔄 Đang làm | ✅ Xong | 🔒 Blocked

---

## DOCS LAYER
| Task | Status | Ghi chú |
|---|---|---|
| PROJECT_BRIEF.md | ✅ | |
| DOMAIN_MODEL.md | ✅ | |
| DATABASE_SCHEMA.md | ✅ | |
| CODING_GUIDE.md | ✅ | |
| ENV_CONFIG.md | ✅ | |
| README.md | ✅ | |
| adr/ (13 ADRs + README.md) | ✅ | |
| api/common/ (6 files) | ✅ | |
| iam-service.openapi.yaml | ✅ | |
| purchase-request-service.openapi.yaml | ✅ | |
| approval-service.openapi.yaml | ✅ | |
| finance-service.openapi.yaml | ✅ | |
| inventory-service.openapi.yaml | ✅ | |
| vendor-service.openapi.yaml | ✅ | |
| analytics-service.openapi.yaml | ✅ | |
| notification-service.openapi.yaml | ✅ | |
| admin-service.openapi.yaml | ✅ | |
| api/README.md | ✅ | |
| user-stories/README.md | ✅ | Quy ước story/use case và MVP boundary |
| user-stories/01-story-map-E01-E15.md | ✅ | Story map toàn bộ epic E01-E15 |
| user-stories/E01-infrastructure-setup.md | ✅ | User story/use case chi tiết MVP |
| user-stories/E02-iam-service.md | ✅ | User story/use case chi tiết MVP |
| user-stories/E03-ui-shell-design-system.md | ✅ | User story/use case chi tiết MVP |
| user-stories/E04-purchase-request-service.md | ✅ | User story/use case chi tiết MVP |
| user-stories/E05-approval-engine.md | ✅ | User story/use case chi tiết MVP |

## RULES LAYER (.cursor/rules/)
| File | Status | Ghi chú |
|---|---|---|
| angular-frontend.mdc | ✅ | Component, template, SCSS, i18n |
| coding.mdc | ✅ | Clean arch, naming, MyBatis, soft delete |
| database.mdc | ✅ | Schema, soft delete, index, Flyway |
| docker-resource.mdc | ✅ | Dockerfile, compose, resource limits |
| error-codes.mdc | ✅ | Error enum, exception hierarchy, handler |
| git-workflow.mdc | ✅ | Branch, commit, PR template, Jenkins |
| logging.mdc | ✅ | Log4j2, masking, layer format |
| testing.mdc | ✅ | JUnit, Postman, JMeter |

## TASK LAYER (agent/tasks/)
| File | Status | Ghi chú |
|---|---|---|
| task-new-feature.md | ✅ | Playbook thêm feature theo layer order |
| task-bug-fix.md | ✅ | Reproduce, root cause, fix, error-history |
| task-add-endpoint.md | ✅ | REST contract, DTO, Controller, OpenAPI |
| task-add-migration.md | ✅ | Flyway versioning, schema rules, rollback note |
| task-refactor.md | ✅ | Refactor boundary, invariant cleanup, verification |

## MEMORY LAYER (agent/memory/)
| File | Status | Ghi chú |
|---|---|---|
| architecture-map.md | ✅ | Service relationships, data flow |
| coding-patterns.md | ✅ | Code templates tái sử dụng |
| decision-log.md | ✅ | Quyết định kỹ thuật đã chốt |
| error-history.md | ✅ | Lịch sử lỗi và cách xử lý |
| progress-tracker.md | ✅ | File này |
| project-context.md | ✅ | Master context |
| tech-stack.md | ✅ | Versions, config patterns |

## EPICS — CODE

### E01: Infrastructure Setup
| Task | Status |
|---|---|
| Docker Compose dev environment | ✅ |
| PostgreSQL init scripts (multiple DBs) | ✅ |
| Kafka topic creation scripts | ✅ |
| Keycloak realm + custom provider setup | ✅ |
| Redis config | ✅ |
| Prometheus + Grafana + Loki + Tempo | ✅ |
| NGINX config (dev + prod) | ✅ |

### E02: IAM Service
| Task | Status |
|---|---|
| Spring Boot project setup | ✅ |
| Flyway migrations (users, roles, permissions, sessions) | ✅ |
| MyBatis mappers | ✅ |
| Login + Keycloak integration | ✅ |
| Opaque token generation + Redis session | ✅ |
| RSA+AES encryption interceptor | 🔄 |
| HttpOnly Cookie management | ✅ |
| Single session enforcement | ✅ |
| Forgot/reset password token + IAM credential reset | ✅ |
| Password reset email delivery adapter | 🔄 |
| RBAC permission loading | ✅ |
| Admin user CRUD API | ✅ |
| Admin role/permission API | ✅ |
| 2FA (TOTP) | ✅ |
| Google OAuth | ✅ |
| Forgot password flow | ✅ |
| Delegation API | ✅ |
| Org chart API | ✅ |
| Unit tests | 🔄 |

### E03: UI Shell & Design System (Angular)
| Task | Status |
|---|---|
| Angular project setup | ✅ |
| Design tokens (SCSS variables) | ✅ |
| ep-button, ep-badge, ep-card | ✅ |
| ep-table (với pagination) | ✅ |
| ep-modal, ep-form-field | ✅ |
| ep-amount, ep-sla-bar, ep-stat-card | ✅ |
| ep-avatar, ep-icon (custom SVG) | ✅ |
| ep-approval-action, ep-filter-bar | ✅ |
| ep-breadcrumb, ep-lang-switcher | ✅ |
| ep-empty-state, ep-skeleton | ✅ |
| /ui-showcase page | ✅ |
| i18n setup (vi/en) | ✅ |
| Auth interceptors | ✅ |
| Error interceptor | ✅ |
| WebSocket service | ✅ |
| Permission guard + directive | ✅ |

### E04: Purchase Request Service
| Task | Status |
|---|---|
| Spring Boot project setup | ✅ |
| Flyway migrations | ✅ |
| Domain model (PurchaseRequest, PrLineItem, Money) | ✅ |
| CreatePR UseCase | ✅ |
| SubmitPR UseCase (budget check + inventory check) | ✅ |
| UpdatePR UseCase | ⬜ |
| CancelPR UseCase | ⬜ |
| File attachment upload | ⬜ |
| Catalog API | ⬜ |
| Kafka event publishing | ⬜ |
| Unit tests (domain + CreatePR use case) | 🔄 | Domain + CreatePR + SubmitPR covered |
| Frontend: PR list page | ⬜ |
| Frontend: PR create form | ⬜ |
| Frontend: PR detail page | ⬜ |

### E05: Approval Engine
| Task | Status |
|---|---|
| Spring Boot + Camunda setup | ⬜ |
| BPMN files (pr-approval-process.bpmn, emergency.bpmn) | ⬜ |
| ApprovalRule engine (select rule by conditions) | ⬜ |
| Approval chain resolution (từ org chart) | ⬜ |
| SLA calculation (business hours) | ⬜ |
| Approve/Reject/RequestChanges/Forward actions | ⬜ |
| SLA timer + escalation | ⬜ |
| Delegation awareness | ⬜ |
| Conflict of interest check | ⬜ |
| Admin: CRUD approval rules | ⬜ |
| Frontend: Approval inbox | ⬜ |
| Frontend: Task detail + actions | ⬜ |

### E06–E15: (Xem PROJECT_BRIEF.md)
| Epic | Status |
|---|---|
| E06: RFQ & Vendor | ⬜ |
| E07: Purchase Order | ⬜ |
| E08: Goods Receipt & Inventory | ⬜ |
| E09: Invoice & Payment | ⬜ |
| E10: Budget Management | ⬜ |
| E11: Notification & Realtime | ⬜ |
| E12: Analytics & Reports | ⬜ |
| E13: Admin & Config Portal | ⬜ |
| E14: Security Hardening | ⬜ |
| E15: Testing & CI/CD | ⬜ |
