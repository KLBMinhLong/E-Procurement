## ADR-006 — Camunda BPMN 7 cho Approval Workflow

**Status:** Accepted  
**Date:** 2025-01  

### Bối cảnh
Approval logic rất phức tạp: ma trận đa chiều (giá trị × danh mục × phòng ban), sequential/parallel, SLA, auto-escalation, delegation. Nếu code thuần sẽ khó maintain và sửa đổi.

### Quyết định
Sử dụng **Camunda BPM 7 (embedded)** làm workflow engine:

- BPMN định nghĩa visual flow của approval process
- Camunda xử lý: task assignment, timer (SLA), gateway (sequential/parallel), escalation
- Approval Engine Service embed Camunda, không cần separate Camunda server
- Dữ liệu BPMN lưu trong schema `camunda` riêng biệt

**BPMN processes chính:**
- `pr-approval-process.bpmn` — luồng duyệt PR chính
- `rfq-process.bpmn` — luồng thu thập báo giá
- `po-process.bpmn` — luồng tạo và giao PO
- `emergency-approval.bpmn` — luồng khẩn cấp

### Hậu quả
- (+) Visual BPMN dễ giải thích cho business stakeholder
- (+) SLA, timer, escalation được Camunda xử lý built-in
- (+) Rules thay đổi không cần deploy lại code
- (-) Học curve Camunda API
- (-) RAM overhead (~256MB) → tính vào resource limits