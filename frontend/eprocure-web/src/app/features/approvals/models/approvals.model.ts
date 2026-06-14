import { Money } from '../../procurement/models/purchase-request.model';

export interface SlaStatus {
  deadlineAt: string;
  remainingHours: number | null;
  isOverdue: boolean;
  isWarning: boolean;
}

export interface RequesterInfo {
  id: string;
  fullName: string;
  department: string;
}

export interface DelegatedFrom {
  id: string;
  fullName: string;
}

export interface ApprovalTaskSummary {
  taskId: string;
  processId: string;
  entityType: string;
  entityId: string;
  entityNumber: string;
  entityTitle: string;
  requester: RequesterInfo;
  totalAmount: Money;
  currency: string;
  priority: 'NORMAL' | 'URGENT' | 'EMERGENCY';
  stepIndex: number;
  stepType: 'SEQUENTIAL' | 'PARALLEL';
  sla: SlaStatus;
  isDelegated: boolean;
  delegatedFrom: DelegatedFrom | null;
  assignedAt: string;
}

export interface ApprovalInboxCount {
  total: number;
  overdue: number;
  emergency: number;
}

export interface ApprovalStepDetail {
  stepIndex: number;
  requiredPermission: string;
  approver: {
    id: string;
    fullName: string;
  };
  delegateId: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ESCALATED' | 'SKIPPED' | 'FORWARDED';
  action: 'APPROVE' | 'REJECT' | 'REQUEST_CHANGES' | 'FORWARD' | null;
  comment: string | null;
  slaDeadline: string;
  assignedAt: string;
  actedAt: string | null;
  isEscalated: boolean;
}

export interface ApprovalProcessDetail {
  id: string;
  entityType: string;
  entityId: string;
  entityNumber: string;
  status: 'RUNNING' | 'COMPLETED' | 'CANCELLED';
  currentStepIndex: number;
  steps: ApprovalStepDetail[];
  startedAt: string;
  completedAt: string | null;
}

export interface ApprovalTaskDetail {
  task: ApprovalTaskSummary;
  process: ApprovalProcessDetail;
  entitySnapshot: any; // Dynamic entity context
}

export interface ApproveTaskRequest {
  comment?: string;
}

export interface RejectTaskRequest {
  comment: string;
}

export interface RequestChangesRequest {
  comment: string;
  requestedFields?: string[];
}

export interface ForwardTaskRequest {
  forwardToUserId: string;
  reason: string;
}

export interface ApprovalInboxFilter {
  page: number;
  size: number;
  sort: string;
  priority?: 'NORMAL' | 'URGENT' | 'EMERGENCY';
  entity_type?: string;
  min_amount?: string;
  is_overdue?: boolean;
}

export type ApprovalRuleType = 'VALUE' | 'CATEGORY' | 'DEPARTMENT' | 'DEFAULT';
export type ApprovalStepType = 'SEQUENTIAL' | 'PARALLEL';
export type ApprovalPriority = 'NORMAL' | 'URGENT' | 'EMERGENCY';

export interface ApprovalRuleCondition {
  minValue: string | null;
  maxValue: string | null;
  categories: string[] | null;
  departmentIds: string[] | null;
  priorities: ApprovalPriority[] | null;
}

export interface ApprovalRuleStepTemplate {
  stepIndex: number;
  requiredPermission: string;
  stepType: ApprovalStepType;
  slaHours: number;
  required: boolean;
}

export interface ApprovalRuleDetail {
  id: string;
  ruleName: string;
  priority: number;
  active: boolean;
  isActive?: boolean;
  ruleType: ApprovalRuleType;
  conditions: ApprovalRuleCondition;
  steps: ApprovalRuleStepTemplate[];
  description: string | null;
}

export interface ApprovalRuleUpsertRequest {
  ruleName: string;
  priority: number;
  active?: boolean;
  ruleType: ApprovalRuleType;
  conditions: ApprovalRuleCondition;
  steps: ApprovalRuleStepTemplate[];
  description: string | null;
}
