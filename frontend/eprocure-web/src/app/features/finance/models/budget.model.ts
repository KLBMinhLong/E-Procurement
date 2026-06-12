import { Money } from '../../procurement/models/purchase-request.model';

export type BudgetStatus = 'PLANNING' | 'SUBMITTED' | 'APPROVED' | 'ACTIVE' | 'CLOSED';

export type BudgetHealthTone = 'HEALTHY' | 'WARNING' | 'EXCEEDED';

export interface BudgetDashboard {
  id: string;
  departmentId: string;
  fiscalYear: number;
  quarter: number | null;
  glAccountCode: string;
  allocated: string;
  committed: string;
  spent: string;
  available: string;
  availablePercent: number;
  burnRatePerMonth: string | null;
  forecastExhaustedAt: string | null;
  status: BudgetStatus;
}

export interface BudgetListFilter {
  page: number;
  size: number;
  sort?: string;
  department_id?: string;
  fiscal_year?: number;
  quarter?: number;
  status?: BudgetStatus;
  gl_account_code?: string;
}

export interface BudgetOverrideRequest {
  prId: string;
  overrideAmount: string;
  currency?: string | null;
  overrideReason: string;
}

export interface BudgetTransferRequest {
  targetBudgetId: string;
  amount: string;
  currency?: string | null;
  reason: string;
}

export interface BudgetOverrideResult {
  id: string;
  budgetId: string;
  prId: string;
  overrideAmount: string;
  currency: string;
  overrideReason: string;
  approvedBy: string;
  approvedAt: string;
  status: 'APPROVED';
}

export interface BudgetTransferResult {
  id: string;
  sourceBudgetId: string;
  targetBudgetId: string;
  amount: string;
  currency: string;
  reason: string;
  approvedBy: string;
  approvedAt: string;
  sourceDashboard: BudgetDashboard;
  targetDashboard: BudgetDashboard;
}

export function budgetHealthTone(budget: BudgetDashboard): BudgetHealthTone {
  if (Number(budget.available) < 0) {
    return 'EXCEEDED';
  }
  if (Number(budget.availablePercent) <= 20) {
    return 'WARNING';
  }
  return 'HEALTHY';
}

export function budgetMoney(budget: BudgetDashboard, field: 'allocated' | 'committed' | 'spent' | 'available'): Money {
  return { amount: budget[field], currency: 'VND' };
}
