import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { EpBadgeComponent, EpBadgeTone } from '../ep-badge/ep-badge.component';
import { EpEmptyStateComponent } from '../ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../ep-icon/ep-icon.component';

export type PrLifecycleStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'PENDING_APPROVAL'
  | 'CHANGES_REQUESTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'CONVERTED_TO_PO'
  | 'CANCELLED'
  | 'CLOSED';

type LifecycleStageStatus = 'complete' | 'current' | 'upcoming' | 'blocked';

interface LifecycleStage {
  status: PrLifecycleStatus;
  icon: string;
}

interface LifecycleStageView extends LifecycleStage {
  state: LifecycleStageStatus;
  tone: EpBadgeTone;
  labelKey: string;
  descriptionKey: string;
}

const LIFECYCLE_STAGES: readonly LifecycleStage[] = [
  { status: 'DRAFT', icon: 'file-pen-line' },
  { status: 'SUBMITTED', icon: 'send' },
  { status: 'PENDING_APPROVAL', icon: 'workflow' },
  { status: 'APPROVED', icon: 'shield-check' },
  { status: 'CONVERTED_TO_PO', icon: 'shopping-cart' },
  { status: 'CLOSED', icon: 'check-circle' }
];

const TERMINAL_STATUSES = new Set<PrLifecycleStatus>(['REJECTED', 'CANCELLED', 'CHANGES_REQUESTED']);

const TERMINAL_ICON: Record<PrLifecycleStatus, string> = {
  DRAFT: 'file-pen-line',
  SUBMITTED: 'send',
  PENDING_APPROVAL: 'workflow',
  CHANGES_REQUESTED: 'refresh-cw',
  APPROVED: 'shield-check',
  REJECTED: 'x-circle',
  CONVERTED_TO_PO: 'shopping-cart',
  CANCELLED: 'ban',
  CLOSED: 'check-circle'
};

const TERMINAL_TONE: Record<PrLifecycleStatus, EpBadgeTone> = {
  DRAFT: 'neutral',
  SUBMITTED: 'info',
  PENDING_APPROVAL: 'warning',
  CHANGES_REQUESTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CONVERTED_TO_PO: 'success',
  CANCELLED: 'neutral',
  CLOSED: 'neutral'
};

@Component({
  selector: 'ep-pr-lifecycle',
  standalone: true,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpEmptyStateComponent,
    EpIconComponent
  ],
  templateUrl: './ep-pr-lifecycle.component.html',
  styleUrl: './ep-pr-lifecycle.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpPrLifecycleComponent {
  readonly status = input<PrLifecycleStatus | null>(null);
  readonly approvalStatus = input<string | null>(null);

  readonly terminalStatus = computed(() => {
    const current = this.status();
    return current && TERMINAL_STATUSES.has(current) ? current : null;
  });

  readonly currentStageIndex = computed(() => {
    const current = this.status();
    if (!current || this.terminalStatus()) {
      return -1;
    }
    const stageIndex = LIFECYCLE_STAGES.findIndex((stage) => stage.status === current);
    return stageIndex >= 0 ? stageIndex : -1;
  });

  readonly terminalAnchorIndex = computed(() => {
    const terminal = this.terminalStatus();
    if (terminal === 'REJECTED' || terminal === 'CHANGES_REQUESTED') {
      return LIFECYCLE_STAGES.findIndex((stage) => stage.status === 'PENDING_APPROVAL');
    }
    if (terminal === 'CANCELLED') {
      return LIFECYCLE_STAGES.findIndex((stage) => stage.status === 'DRAFT');
    }
    return -1;
  });

  readonly stages = computed<readonly LifecycleStageView[]>(() => {
    const currentIndex = this.currentStageIndex();
    const terminalAnchorIndex = this.terminalAnchorIndex();

    return LIFECYCLE_STAGES.map((stage, index) => {
      const state = this.resolveStageState(index, currentIndex, terminalAnchorIndex);
      return {
        ...stage,
        state,
        tone: this.toneForState(state),
        labelKey: `pr.lifecycle.${stage.status}.label`,
        descriptionKey: `pr.lifecycle.${stage.status}.description`
      };
    });
  });

  readonly terminalLabelKey = computed(() => {
    const terminal = this.terminalStatus();
    return terminal ? `pr.lifecycle.${terminal}.label` : null;
  });

  readonly terminalDescriptionKey = computed(() => {
    const terminal = this.terminalStatus();
    return terminal ? `pr.lifecycle.${terminal}.description` : null;
  });

  readonly terminalIcon = computed(() => {
    const terminal = this.terminalStatus();
    return terminal ? TERMINAL_ICON[terminal] : 'circle-alert';
  });

  readonly terminalTone = computed(() => {
    const terminal = this.terminalStatus();
    return terminal ? TERMINAL_TONE[terminal] : 'neutral';
  });

  readonly hasApprovalStatus = computed(() => Boolean(this.approvalStatus()));

  isComplete(stage: LifecycleStageView): boolean {
    return stage.state === 'complete';
  }

  isCurrent(stage: LifecycleStageView): boolean {
    return stage.state === 'current';
  }

  isBlocked(stage: LifecycleStageView): boolean {
    return stage.state === 'blocked';
  }

  private resolveStageState(index: number, currentIndex: number, terminalAnchorIndex: number): LifecycleStageStatus {
    if (terminalAnchorIndex >= 0) {
      return index < terminalAnchorIndex ? 'complete' : 'blocked';
    }
    if (currentIndex < 0) {
      return 'upcoming';
    }
    if (index < currentIndex) {
      return 'complete';
    }
    if (index === currentIndex) {
      return 'current';
    }
    return 'upcoming';
  }

  private toneForState(state: LifecycleStageStatus): EpBadgeTone {
    if (state === 'complete') {
      return 'success';
    }
    if (state === 'current') {
      return 'warning';
    }
    return 'neutral';
  }
}
