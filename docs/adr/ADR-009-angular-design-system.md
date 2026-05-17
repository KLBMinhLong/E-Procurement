## ADR-009 — Angular + Enterprise Dark Design System

**Status:** Accepted  
**Date:** 2025-01  

### Bối cảnh
Cần consistent UI cho nhiều developer. Hệ thống enterprise cần cảm giác chuyên nghiệp, tin cậy, mật độ thông tin cao.

### Quyết định
- **Design System:** "Enterprise Dark Command Center" — Deep navy + Amber accent
- **Fonts:** IBM Plex Mono (data) + IBM Plex Sans (UI text)
- **Component library:** Tự build, không dùng icon library ngoài
- **Icons:** 100% custom SVG component `ep-icon`
- **CSS:** SCSS với CSS Custom Properties (design tokens) — không hardcode màu
- **i18n:** ngx-translate, 100% text qua translate pipe
- **Storybook-like page:** Trang `/ui-showcase` để xem toàn bộ component

**Shared components (prefix `ep-`):**
`ep-button`, `ep-badge`, `ep-card`, `ep-table`, `ep-modal`, `ep-form-field`, `ep-avatar`, `ep-sla-bar`, `ep-stat-card`, `ep-amount`, `ep-approval-action`, `ep-filter-bar`, `ep-breadcrumb`, `ep-lang-switcher`, `ep-empty-state`, `ep-skeleton`, `ep-icon`

### Hậu quả
- (+) Consistent UI, dễ maintain, dễ theme
- (+) Developer không phải ra quyết định design → tập trung vào logic
- (-) Initial effort lớn để build component library
- (-) Không có icon library → cần vẽ custom SVG