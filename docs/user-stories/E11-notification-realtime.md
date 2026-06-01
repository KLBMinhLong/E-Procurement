# E11 Notification & Realtime
## User Stories và Use Cases

---

## 1. Epic Goal

Triển khai `notification-service` để chuyển các event nghiệp vụ hiện đã publish từ PR/Approval/Finance thành thông báo in-app, email và realtime WebSocket. Epic này nên làm sau E10 budget foundation vì E10 sẽ phát thêm `finance.budget.warning`/`finance.budget.exceeded`, nhưng có thể chuẩn bị ngay để thay các adapter email stub như reset password delivery ở IAM.

---

## 2. Actors

| Actor | Vai trò |
|---|---|
| Requester | Nhận thông báo PR approved/rejected/changes requested |
| Approver | Nhận thông báo task assigned, SLA warning/breach |
| Finance Manager | Nhận cảnh báo budget warning/exceeded |
| Admin | Quản lý template notification |
| IAM/PR/Approval/Finance Service | Publish event hoặc yêu cầu gửi email |
| Notification Service | Render template, lưu notification, gửi email/WebSocket |

---

## 3. User Stories

| ID | Story | Priority | API/Event Reference |
|---|---|---|---|
| E11-US-001 | Là Approver, tôi muốn nhận in-app/WebSocket khi có approval task mới. | P1 | `approval.step.assigned` |
| E11-US-002 | Là Requester, tôi muốn nhận thông báo khi PR được approve/reject/request changes. | P1 | `procurement.pr.approved`, `procurement.pr.rejected`, `procurement.pr.changes-requested` |
| E11-US-003 | Là Approver/Admin, tôi muốn nhận cảnh báo SLA warning/breach để xử lý task quá hạn. | P1 | `approval.sla.warning`, `approval.sla.breached` |
| E11-US-004 | Là Finance Manager, tôi muốn nhận cảnh báo budget thấp hoặc vượt ngân sách. | P1 | `finance.budget.warning`, `finance.budget.exceeded` |
| E11-US-005 | Là User, tôi muốn xem danh sách thông báo của mình với unread count. | P1 | `GET /notifications`, `GET /notifications/count` |
| E11-US-006 | Là User, tôi muốn đánh dấu đã đọc một hoặc tất cả thông báo. | P1 | `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` |
| E11-US-007 | Là Admin, tôi muốn quản lý template notification theo event/channel/language. | P2 | `/notification-templates` |
| E11-US-008 | Là Admin, tôi muốn preview template với sample data trước khi bật. | P2 | `POST /notification-templates/{code}/preview` |
| E11-US-009 | Là hệ thống, tôi muốn gửi email reset password/transactional email qua notification-service thay vì stub trong IAM. | P1 | `notification.email.send` hoặc internal send contract |
| E11-US-010 | Là hệ thống, tôi muốn retry email thất bại tối đa 3 lần và ghi trạng thái FAILED khi hết retry. | P1 | scheduled retry/DLQ |

---

## 4. Use Cases

### E11-UC-001: ConsumeBusinessEventUseCase

**Trigger topics:**

```
approval.step.assigned
approval.sla.warning
approval.sla.breached
procurement.pr.approved
procurement.pr.rejected
procurement.pr.changes-requested
finance.budget.warning
finance.budget.exceeded
notification.email.send
```

**Main flow:**

```
1. Kafka listener validates envelope eventId, eventType, source, timestamp, payload.
2. Check idempotency by eventId.
3. Map eventType to notification policy.
4. Resolve recipients.
5. Create in-app notification records.
6. Queue email dispatch if policy includes EMAIL.
7. Push WebSocket message after DB commit.
8. Mark event processed.
```

**Acceptance criteria:**

```
[ ] Duplicate event does not duplicate notifications.
[ ] Unknown eventType is logged and skipped safely.
[ ] Payload/log không chứa password, token, secret, encrypted payload.
[x] Notification creation happens transactionally before WebSocket push.
```

### E11-UC-002: ResolveNotificationRecipientsUseCase

**Main flow:**

```
1. Read explicit recipientId if event payload has one.
2. For approval task assigned, notify approverId and delegateId if present.
3. For PR result, notify requesterId.
4. For SLA breach/escalation, notify task owner plus admin/manager recipients if event contains routing metadata.
5. For budget warning/exceeded, notify finance managers or configured department owners.
```

**Acceptance criteria:**

```
[ ] Recipient resolution is deterministic and testable.
[ ] Missing recipient routes to DLQ/error log, not silent success.
[ ] Does not query another service unless a clear port exists.
```

### E11-UC-003: RenderNotificationTemplateUseCase

**Main flow:**

```
1. Load active template by eventType + channel + language.
2. Validate required variables exist.
3. Render subject/body with safe escaping.
4. Return rendered notification content.
```

**Alternate/error flows:**

```
- Missing language template -> fallback to vi.
- Missing template -> use safe system default and log warning.
- Template variable missing -> mark notification FAILED for EMAIL, still allow minimal IN_APP if configured.
```

**Acceptance criteria:**

```
[ ] Template rendering does not execute arbitrary code.
[ ] HTML email body is sanitized/escaped according to template engine capability.
[ ] Template update is audit logged.
```

### E11-UC-004: CreateInAppNotificationUseCase

**Main flow:**

```
1. Build Notification aggregate with recipientId, eventType, channel IN_APP.
2. Set referenceType/referenceId/referenceNumber/actionUrl.
3. Save status SENT for in-app notification.
4. Publish WebSocket payload to user topic.
```

**Acceptance criteria:**

```
[ ] notifications table supports unread feed by recipient_id/is_read/created_at.
[ ] actionUrl is a relative FE route, not hardcoded domain.
[x] WebSocket failure does not roll back persisted notification.
```

### E11-UC-005: DispatchEmailUseCase

**Trigger:** notification policy includes EMAIL, or event `notification.email.send`.

**Main flow:**

```
1. Render EMAIL template.
2. Send through BrevoEmailPort.
3. Mark notification SENT and sentAt on success.
4. On failure, store lastError sanitized, increment retryCount.
5. Retry up to 3 attempts; after that mark FAILED.
```

**Acceptance criteria:**

```
[x] Email dispatch is async and does not block PR/Approval/Finance transactions.
[x] No raw token/password is logged.
[x] Reset password email sends only short-lived reset URL/token material per IAM contract and masks logs.
```

### E11-UC-006: ListMyNotificationsUseCase

**Permission:** authenticated user with valid `ep_session`.

**Main flow:**

```
1. Resolve current user id from security context.
2. Accept is_read, event_type, cursor/page/size filters.
3. Query notifications by recipient_id with unread/date filters.
4. Return NotificationItem list with PageMeta or cursor meta.
```

**Acceptance criteria:**

```
[ ] User cannot list another user's notifications.
[ ] Query uses index recipient_id, is_read, created_at DESC.
[ ] Cursor and page modes are not mixed incorrectly.
```

### E11-UC-007: CountUnreadNotificationsUseCase

**Main flow:**

```
1. Count unread notifications for current user.
2. Return { unread }.
3. Optionally cache short TTL if volume requires.
```

**Acceptance criteria:**

```
[ ] Count updates immediately after mark read/read-all.
[ ] Count endpoint does not require body or Idempotency-Key.
```

### E11-UC-008: MarkNotificationReadUseCase

**Endpoint:** `PATCH /notifications/{id}/read`.

**Main flow:**

```
1. Check Idempotency-Key.
2. Load notification by id and recipientId = current user.
3. If already read, return success idempotently.
4. Set isRead=true, readAt=now.
```

**Acceptance criteria:**

```
[ ] Cannot mark another user's notification.
[ ] Repeating same key replays or remains safe.
[ ] No HTTP DELETE for notification cleanup.
```

### E11-UC-009: MarkAllNotificationsReadUseCase

**Endpoint:** `PATCH /notifications/read-all`.

**Main flow:**

```
1. Check Idempotency-Key.
2. Update unread notifications for current user.
3. Return markedCount.
```

**Acceptance criteria:**

```
[ ] Scope is current user only.
[ ] markedCount is stable on idempotency replay.
```

### E11-UC-010: ManageNotificationTemplateUseCases

**Use cases:**

```
ListNotificationTemplatesUseCase
UpdateNotificationTemplateUseCase
PreviewNotificationTemplateUseCase
```

**Permission:** `SYSTEM_CONFIG`.

**Main flow:**

```
1. Admin lists templates by channel/eventType/language.
2. Admin updates subject/body/isActive with Idempotency-Key.
3. Admin previews rendered content with sample data.
4. Audit log records template code, actor, action.
```

**Acceptance criteria:**

```
[x] Template updates do not break existing unread notification bodies.
[x] Preview does not persist notification rows.
[x] Visible admin UI text uses i18n keys.
```

---

## 5. Frontend Use Cases

```
NotificationBell: unread badge, dropdown latest notifications, WebSocket live update.
NotificationCenterPage: filter read/unread/event type, infinite scroll or pagination.
NotificationTemplateAdminPage: list/edit/preview templates.
Toast bridge: show short realtime toast for task assigned/result/SLA events.
```

Acceptance:

```
[x] WebSocket service uses cookie auth and reconnect strategy.
[ ] All HTTP requests use withCredentials.
[ ] PATCH read/read-all uses Idempotency-Key.
[ ] No hardcoded visible text; translate keys only.
```

---

## 6. Technical Deliverables

```
services/notification-service Maven module
Dockerfile + docker-compose service on port 8088
Flyway migrations: notification_templates, notifications
Domain: Notification, NotificationTemplate
UseCases: ConsumeBusinessEvent, ResolveRecipients, RenderTemplate, CreateInAppNotification, DispatchEmail, ListMyNotifications, CountUnread, MarkRead, MarkAllRead, ManageTemplates
Ports: BrevoEmailPort, WebSocketPushPort, IdempotencyService
Adapters: Kafka consumers, Brevo SMTP/API adapter, STOMP/WebSocket config, MyBatis repositories
Frontend: Notification bell/feed and template admin page
Tests: event idempotency, template render, read/unread scope, retry handling
```

---

## 7. First Coding Slice Recommendation

```
1. [x] Scaffold notification-service with schema, template seed, Log4j2, Kafka, Redis and Docker resource limit.
2. [x] Implement in-app notification persistence plus GET /notifications and /notifications/count.
3. [x] Consume approval/PR/finance notification topics and persist in-app notifications. Budget alerts are wired first because E10 now publishes them.
4. [x] Add real WebSocket/STOMP adapter and frontend notification bell consuming count/feed and realtime messages.
5. [x] Add email dispatch for notification.email.send after in-app path is stable.
6. [x] Add template admin list/update/preview API and Angular admin page.
```

This keeps E11 useful immediately for Approval tasks while avoiding early coupling to every future event type.

---

## 8. Readiness Checklist

```
[x] Template seed list defined for approval.step.assigned, PR result events, SLA events, and budget alert events.
[x] Event payload fields needed for subject/body/actionUrl documented in `notification-service` template seed and `ConsumeBusinessEventUseCase` mapping.
[x] Brevo credentials remain ENV-only and never logged.
[x] Retry/DLQ behavior decided for failed email.
[x] IAM password reset delivery migration path decided: Kafka notification.email.send preferred; internal API only if product requires sync confirmation.

---

## 9. Implementation Status

```
[x] services/notification-service Maven module
[x] Dockerfile + docker-compose service on port 8088
[x] Flyway V1 notification schema + template seed + event_processing_log
[x] In-app feed/count/read APIs guarded by NOTIFICATION_VIEW_OWN
[x] Business event consumer for approval/PR/finance notification topics
[x] Unit tests for budget alert creation and duplicate event idempotency
[x] Real STOMP/WebSocket delivery adapter
[x] Angular notification bell/feed in shell
[x] Email dispatch + retry worker
[x] Template admin API/UI
```
```
