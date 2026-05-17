# RBAC Permission Codes

## Rules
- Use permission codes in @PreAuthorize, never roles.
- Naming: RESOURCE_ACTION[_SCOPE]
- Source of truth: db_iam.iam.permissions seed data.

## Purchase Request (PR)
- PR_CREATE - Tao PR moi
- PR_SUBMIT - Submit PR
- PR_VIEW_OWN - Xem PR cua minh
- PR_VIEW_DEPARTMENT - Xem PR cua phong ban
- PR_VIEW_ALL - Xem tat ca PR
- PR_EDIT_OWN_DRAFT - Sua PR draft cua minh
- PR_CANCEL_OWN - Huy PR cua minh
- PR_CANCEL_ANY - Huy PR bat ky (admin)
- PR_APPROVE_L1 - Duyet cap 1
- PR_APPROVE_L2 - Duyet cap 2
- PR_APPROVE_L3 - Duyet cap 3
- PR_APPROVE_FINANCE - Duyet buoc tai chinh
- PR_APPROVE_EMERGENCY - Duyet PR khan cap
- PR_APPROVE_BUDGET_OVERRIDE - Duyet vuot ngan sach
- PR_REQUEST_CHANGES - Yeu cau bo sung thong tin
- PR_FORWARD - Chuyen task
- PR_VIEW_REPORTS - Xem bao cao PR

## Purchase Order (PO)
- PO_CREATE - Tao PO tu PR
- PO_VIEW_OWN - Xem PO cua minh
- PO_VIEW_ALL - Xem tat ca PO
- PO_EDIT - Sua PO
- PO_SEND_TO_VENDOR - Gui PO cho vendor
- PO_CANCEL - Huy PO
- PO_VIEW_REPORTS - Xem bao cao PO

## RFQ
- RFQ_CREATE - Tao RFQ
- RFQ_VIEW - Xem RFQ
- RFQ_SEND_INVITATION - Gui moi bao gia
- RFQ_EVALUATE - Danh gia bao gia
- RFQ_AWARD - Chot vendor thang thau

## Goods Receipt (GR)
- GR_CREATE - Tao phieu nhan hang
- GR_VIEW - Xem GR
- GR_APPROVE - Duyet GR
- GR_ISSUE_OUT - Cap phat kho

## Invoice & Payment
- INVOICE_CREATE - Nhap hoa don
- INVOICE_VIEW - Xem hoa don
- INVOICE_MATCH - 3-way match
- INVOICE_APPROVE - Duyet hoa don
- PAYMENT_CONFIRM - Xac nhan thanh toan

## Budget
- BUDGET_VIEW_OWN_DEPT - Xem ngan sach phong minh
- BUDGET_VIEW_ALL - Xem ngan sach toan cong ty
- BUDGET_EDIT - Lap/sua ngan sach
- BUDGET_APPROVE - Duyet ngan sach
- BUDGET_TRANSFER_REQUEST - De xuat chuyen ngan sach
- BUDGET_TRANSFER_APPROVE - Duyet chuyen ngan sach
- BUDGET_OVERRIDE - Duyet vuot ngan sach

## Vendor
- VENDOR_VIEW - Xem danh sach vendor
- VENDOR_CREATE - Them vendor moi
- VENDOR_EDIT - Sua vendor
- VENDOR_APPROVE - Duyet vendor vao AVL
- VENDOR_SCORE - Nhap diem scorecard

## Inventory
- INVENTORY_VIEW - Xem ton kho
- INVENTORY_ADJUST - Dieu chinh ton kho

## Reports & Analytics
- REPORT_VIEW - Xem bao cao
- REPORT_EXPORT - Xuat bao cao
- DASHBOARD_C_LEVEL - Xem dashboard cap cao

## Admin/System
- ADMIN_USER_VIEW - Xem danh sach user
- ADMIN_USER_MANAGE - Quan ly user
- ADMIN_ROLE_MANAGE - Quan ly role/permission
- ADMIN_APPROVAL_RULE - Cau hinh approval rules
- ADMIN_CATALOG_MANAGE - Quan ly danh muc hang hoa
- ADMIN_DELEGATION_MANAGE - Quan ly uy quyen
- ADMIN_DEPARTMENT_MANAGE - Quan ly phong ban va org chart
- SYSTEM_CONFIG - Truy cap trang cau hinh he thong
- SYSTEM_AUDIT_VIEW - Xem audit log
