// ── Money & Quantity ────────────────────────────────────────────────
export interface Money {
  amount: string;
  currency: string;
}

export interface Quantity {
  amount: string;
  unit: string;
}

// ── Enums ────────────────────────────────────────────────────────────
export type PrStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'PENDING_APPROVAL'
  | 'CHANGES_REQUESTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'CONVERTED_TO_PO'
  | 'CANCELLED'
  | 'CLOSED';

export type PrPriority = 'NORMAL' | 'URGENT' | 'EMERGENCY';

export type BudgetCheckStatus = 'PASS' | 'WARNING' | 'FAIL';

// ── Line Items ────────────────────────────────────────────────────────
export interface PrLineItemRequest {
  itemCode?: string | null;
  itemName: string;
  description?: string | null;
  categoryCode: string;
  quantity: Quantity;
  unitPrice: Money;
  preferredVendorId?: string | null;
  specifications?: string | null;
  glAccountCode: string;
  isFromCatalog?: boolean;
}

export interface PrLineItemResponse extends PrLineItemRequest {
  id: string;
  lineNumber: number;
  totalPrice: Money;
}

// ── Attachment ────────────────────────────────────────────────────────
export interface AttachmentInfo {
  id: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  uploadedAt: string;
}

// ── Approval ─────────────────────────────────────────────────────────
export interface ApprovalStepSummary {
  stepIndex: number;
  approverRole: string;
  approver: { id: string; fullName: string };
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ESCALATED' | 'SKIPPED' | 'FORWARDED';
  comment: string | null;
  assignedAt: string;
  slaDeadline: string;
  slaRemainingHours: number | null;
  actedAt: string | null;
}

export interface ApprovalProcess {
  status: string;
  currentStep: number;
  steps: ApprovalStepSummary[];
}

// ── Budget & Inventory ────────────────────────────────────────────────
export interface BudgetCheckResult {
  allocated: string;
  committed: string;
  spent: string;
  available: string;
  status: BudgetCheckStatus;
  warningMessage: string | null;
}

export interface InventoryCheckItem {
  itemCode: string;
  itemName: string;
  quantityOnHand: string;
  unit: string;
}

export interface InventoryCheckResult {
  itemsWithStock: InventoryCheckItem[];
  suggestion: string | null;
}

// ── Requester ─────────────────────────────────────────────────────────
export interface RequesterInfo {
  id: string;
  fullName: string;
  department: string;
}

export interface CurrentApprover {
  id: string;
  fullName: string;
  role: string;
  slaDeadline: string;
}

// ── Summary (List row) ────────────────────────────────────────────────
export interface PurchaseRequestSummary {
  id: string;
  prNumber: string;
  title: string;
  priority: PrPriority;
  status: PrStatus;
  totalAmount: Money;
  requesterId?: string;
  departmentId?: string;
  requester?: RequesterInfo;
  needByDate: string | null;
  currentApprover: CurrentApprover | null;
  createdAt: string;
  updatedAt: string;
}

// ── Detail ────────────────────────────────────────────────────────────
export interface PurchaseRequestDetail extends PurchaseRequestSummary {
  justification: string;
  urgencyReason: string | null;
  fiscalYear: number;
  departmentId: string;
  lineItems: PrLineItemResponse[];
  attachments: AttachmentInfo[];
  budgetCheck: BudgetCheckResult | null;
  inventoryCheck: InventoryCheckResult | null;
  approvalProcess: ApprovalProcess | null;
}

// ── Request payloads ──────────────────────────────────────────────────
export interface CreatePrRequest {
  title: string;
  justification: string;
  priority?: PrPriority;
  urgencyReason?: string | null;
  needByDate?: string | null;
  lineItems: PrLineItemRequest[];
  attachmentIds?: string[];
}

export interface UpdatePrRequest {
  title?: string;
  justification?: string;
  priority?: PrPriority;
  urgencyReason?: string | null;
  needByDate?: string | null;
  lineItems?: PrLineItemRequest[];
  attachmentIds?: string[];
}

export interface CancelPrRequest {
  reason: string;
}

// ── Filter ────────────────────────────────────────────────────────────
export interface PrListFilter {
  page: number;
  size: number;
  sort: string;
  status?: PrStatus;
  priority?: PrPriority;
  q?: string;
  from_date?: string;
  to_date?: string;
  min_amount?: string;
  max_amount?: string;
  department_id?: string;
  requester_id?: string;
}

// ── Catalog ───────────────────────────────────────────────────────────
export interface CatalogCategory {
  code: string;
  name: string;
  parentCode: string | null;
  requiresRfqAbove: string | null;
  isCapex: boolean;
  children: CatalogCategory[];
}

export interface CatalogItem {
  id: string;
  itemCode: string;
  name: string;
  description: string | null;
  categoryCode: string;
  unit: string;
  unitPrice: Money;
  quantityOnHand: string | null;
  isActive: boolean;
}

export interface CatalogItemFilter {
  q?: string;
  category_code?: string;
  page?: number;
  size?: number;
}
