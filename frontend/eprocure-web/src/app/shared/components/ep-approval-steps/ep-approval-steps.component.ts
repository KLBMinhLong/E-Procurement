import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { EpBadgeComponent, EpBadgeTone } from '../ep-badge/ep-badge.component';
import { EpEmptyStateComponent } from '../ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../ep-icon/ep-icon.component';
import { EpSlaBarComponent } from '../ep-sla-bar/ep-sla-bar.component';

export type ApprovalStepStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'ESCALATED' | 'SKIPPED' | 'FORWARDED';

export interface ApprovalStepView {
  stepIndex: number;
  requiredPermission: string;
  approver?: {
    id?: string | null;
    fullName?: string | null;
  } | null;
  status: ApprovalStepStatus;
  action?: 'APPROVE' | 'REJECT' | 'REQUEST_CHANGES' | 'FORWARD' | null;
  comment?: string | null;
  actedAt?: string | null;
  assignedAt?: string | null;
  slaDeadline?: string | null;
  slaRemainingHours?: number | null;
  delegateId?: string | null;
  isEscalated?: boolean;
}

const STATUS_TONE: Record<ApprovalStepStatus, EpBadgeTone> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  ESCALATED: 'warning',
  SKIPPED: 'neutral',
  FORWARDED: 'info'
};

const STATUS_ICON: Record<ApprovalStepStatus, string> = {
  PENDING: 'clock',
  APPROVED: 'check',
  REJECTED: 'x',
  ESCALATED: 'shield-alert',
  SKIPPED: 'circle-off',
  FORWARDED: 'send'
};

const TERMINAL_STATUSES = new Set<ApprovalStepStatus>(['APPROVED', 'REJECTED', 'ESCALATED', 'SKIPPED', 'FORWARDED']);

@Component({
  selector: 'ep-approval-steps',
  standalone: true,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpEmptyStateComponent,
    EpIconComponent,
    EpSlaBarComponent
  ],
  templateUrl: './ep-approval-steps.component.html',
  styleUrl: './ep-approval-steps.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpApprovalStepsComponent {
  readonly steps = input.required<ReadonlyArray<ApprovalStepView>>();
  readonly currentStepIndex = input<number | null>(null);
  readonly viewMode = input<'horizontal' | 'vertical'>('horizontal');
  readonly showSlaBar = input(true);
  readonly compact = input(false);

  readonly expandedStepIndex = signal<number | null>(null);

  readonly sortedSteps = computed(() => [...this.steps()].sort((left, right) => left.stepIndex - right.stepIndex));

  readonly resolvedCurrentStepIndex = computed(() => {
    const explicit = this.currentStepIndex();
    if (explicit !== null && explicit !== undefined) {
      return explicit;
    }
    return this.sortedSteps().find((step) => step.status === 'PENDING')?.stepIndex ?? null;
  });

  readonly activeStep = computed(() => {
    const current = this.resolvedCurrentStepIndex();
    return this.sortedSteps().find((step) => step.stepIndex === current) ?? null;
  });

  toggleStep(step: ApprovalStepView): void {
    this.expandedStepIndex.update((current) => (current === step.stepIndex ? null : step.stepIndex));
  }

  isExpanded(step: ApprovalStepView): boolean {
    return this.expandedStepIndex() === step.stepIndex;
  }

  isCurrent(step: ApprovalStepView): boolean {
    return this.resolvedCurrentStepIndex() === step.stepIndex;
  }

  isComplete(step: ApprovalStepView): boolean {
    return step.status === 'APPROVED';
  }

  statusTone(status: ApprovalStepStatus): EpBadgeTone {
    return STATUS_TONE[status] ?? 'neutral';
  }

  statusIcon(status: ApprovalStepStatus): string {
    return STATUS_ICON[status] ?? 'circle';
  }

  connectorTone(step: ApprovalStepView): EpBadgeTone {
    if (step.status === 'REJECTED') {
      return 'danger';
    }
    if (step.status === 'APPROVED') {
      return 'success';
    }
    if (step.status === 'PENDING') {
      return 'warning';
    }
    return 'neutral';
  }

  canShowSla(step: ApprovalStepView): boolean {
    return Boolean(this.showSlaBar() && step.status === 'PENDING' && step.assignedAt && step.slaDeadline);
  }

  approverLabel(step: ApprovalStepView): string {
    return step.approver?.fullName || step.approver?.id || '';
  }

  hasApprover(step: ApprovalStepView): boolean {
    return Boolean(this.approverLabel(step));
  }

  shouldShowCompactComment(step: ApprovalStepView): boolean {
    return Boolean(step.comment && TERMINAL_STATUSES.has(step.status));
  }

  detailNoteKey(step: ApprovalStepView): string | null {
    if (step.status === 'FORWARDED') {
      return 'approval.steps.forwarded';
    }
    if (step.status === 'SKIPPED') {
      return 'approval.steps.skipped';
    }
    if (step.isEscalated || step.status === 'ESCALATED') {
      return 'approval.steps.escalated';
    }
    return null;
  }

  formatDateTime(iso: string | null | undefined): string {
    if (!iso) {
      return '--';
    }
    return new Intl.DateTimeFormat('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(new Date(iso));
  }
}
