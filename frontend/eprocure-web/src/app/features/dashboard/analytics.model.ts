import { Money } from '../procurement/models/purchase-request.model';

export type KpiStatus = 'GOOD' | 'WARNING' | 'CRITICAL';
export type TrendDirection = 'UP' | 'DOWN' | 'FLAT';
export type ReportFormat = 'PDF' | 'EXCEL';
export type ReportJobStatus = 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type ReportType =
  | 'SPENDING_BY_DEPARTMENT'
  | 'BUDGET_VS_PLAN'
  | 'VENDOR_SCORECARD'
  | 'CYCLE_TIME_ANALYSIS'
  | 'SLA_COMPLIANCE'
  | 'THREE_WAY_MATCH'
  | 'INVENTORY_PENDING'
  | 'RFQ_SAVINGS'
  | 'MAVERICK_SPENDING'
  | 'AUDIT_TRAIL'
  | 'PR_SUMMARY'
  | 'PO_SUMMARY';

export interface Trend {
  direction: TrendDirection;
  percent: string | number;
  vsLabel: string;
}

export interface KpiCard {
  label: string;
  value: string;
  unit: string | null;
  trend: Trend | null;
  status: KpiStatus | null;
}

export interface ChartDataPoint {
  label: string;
  value: string | number;
  value2: string | number | null;
}

export interface DepartmentSpend {
  departmentCode: string;
  departmentName: string;
  spent: string;
  budget: string;
  utilization: string | number;
  status: KpiStatus;
}

export interface MonthlyTrend {
  month: string;
  spent: string | number;
  budget: string | number;
  prCount: number;
}

export interface ApprovalSla {
  onTimePercent: string | number;
  avgCycleHours: string | number;
  overdueCount: number;
}

export interface TopVendor {
  vendorName: string;
  totalSpent: string;
  orderCount: number;
  avgScore: string | number;
}

export interface ExecutiveDashboard {
  kpis: KpiCard[];
  spendByDepartment: DepartmentSpend[];
  spendByCategory: ChartDataPoint[];
  monthlyTrend: MonthlyTrend[];
  approvalSla: ApprovalSla;
  topVendors: TopVendor[];
  cachedAt: string | null;
}

export interface BudgetStatus {
  allocated: string;
  committed: string;
  spent: string;
  available: string;
  availablePct: string | number;
  forecastRunOutDate: string | null;
}

export interface PendingApprovals {
  count: number;
  overdueCount: number;
  urgentCount: number;
}

export interface ManagerRecentPr {
  prNumber: string;
  title: string;
  status: string;
  totalAmount: string;
  requester: string;
  createdAt: string;
}

export interface SlaWarning {
  taskId: string;
  prNumber: string;
  slaDeadline: string;
  isOverdue: boolean;
}

export interface ManagerDashboard {
  kpis: KpiCard[];
  budgetStatus: BudgetStatus;
  pendingApprovals: PendingApprovals;
  recentPrs: ManagerRecentPr[];
  slaWarnings: SlaWarning[];
}

export interface PoPipeline {
  draft: number;
  pendingApproval: number;
  sentToVendor: number;
  partiallyReceived: number;
}

export interface VendorPerformance {
  vendorName: string;
  onTimeDelivery: string | number;
  qualityScore: number;
  pendingOrders: number;
}

export interface PurchasingDashboard {
  kpis: KpiCard[];
  poPipeline: PoPipeline;
  openRfqs: number;
  grPending: number;
  invoicesPendingMatch: number;
  vendorPerformance: VendorPerformance[];
}

export interface MyPurchaseRequestStats {
  draft: number;
  pendingApproval: number;
  changesRequested: number;
  approved: number;
  rejected: number;
}

export interface DepartmentBudgetSummary {
  available: string;
  availablePct: string | number;
}

export interface RequesterRecentPr {
  prNumber: string;
  title: string;
  status: string;
  currentApprover: string | null;
  totalAmount: string;
}

export interface RequesterDashboard {
  myPrStats: MyPurchaseRequestStats;
  departmentBudget: DepartmentBudgetSummary;
  recentPrs: RequesterRecentPr[];
}

export interface PriorityCycleTime {
  priority: string;
  avgHours: string | number;
}

export interface WeeklyCycleTime {
  week: string;
  avgHours: string | number;
}

export interface CycleTimeKpi {
  avgCycleHours: string | number;
  medianCycleHours: string | number;
  p95CycleHours: string | number;
  target: string | number;
  byPriority: PriorityCycleTime[];
  trend: WeeklyCycleTime[];
}

export interface ApproverRoleSla {
  role: string;
  compliancePct: string | number;
  avgActionHours: string | number;
  overdueCount: number;
}

export interface WorstApproverSla {
  approverName: string;
  overdueCount: number;
  compliancePct: string | number;
}

export interface SlaComplianceKpi {
  overallCompliancePct: string | number;
  byApproverRole: ApproverRoleSla[];
  worstApprovers: WorstApproverSla[];
}

export interface ExportReportFilters {
  fromDate?: string | null;
  toDate?: string | null;
  fiscalYear?: number | null;
  quarter?: number | null;
  vendorId?: string | null;
  categoryCode?: string | null;
}

export interface ExportReportRequest {
  reportType: ReportType;
  format: ReportFormat;
  filters: ExportReportFilters;
}

export interface ReportJob {
  jobId: string;
  reportType: ReportType;
  status: ReportJobStatus;
  format: ReportFormat;
  downloadUrl: string | null;
  createdAt: string;
  completedAt: string | null;
  expiresAt: string | null;
}

export interface ExecutiveDashboardFilter {
  fiscal_year?: number;
  quarter?: number;
}

export interface KpiFilter {
  from_date: string;
  to_date: string;
  department_id?: string;
}

export function analyticsMoney(amount: string | number | null | undefined, currency = 'VND'): Money {
  return { amount: String(amount ?? '0'), currency };
}
