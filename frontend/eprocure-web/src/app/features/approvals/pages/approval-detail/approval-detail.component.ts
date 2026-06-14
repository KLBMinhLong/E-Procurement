import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';
import { FormsModule } from '@angular/forms';

import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpAvatarComponent } from '../../../../shared/components/ep-avatar/ep-avatar.component';
import { EpSlaBarComponent } from '../../../../shared/components/ep-sla-bar/ep-sla-bar.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpApprovalStepsComponent } from '../../../../shared/components/ep-approval-steps/ep-approval-steps.component';
import { ApprovalsService } from '../../services/approvals.service';
import { AdminUserService } from '../../../admin/services/admin-user.service';
import { AdminUserSummary } from '../../../admin/models/admin.model';
import {
  ApprovalTaskDetail,
  ApprovalProcessDetail
} from '../../models/approvals.model';

const PRIORITY_TONE: Record<string, EpBadgeTone> = {
  NORMAL: 'neutral',
  URGENT: 'warning',
  EMERGENCY: 'danger'
};

const STATUS_TONE: Record<string, EpBadgeTone> = {
  PENDING: 'warning',
  RUNNING: 'warning',
  IN_PROGRESS: 'warning',
  APPROVED: 'success',
  COMPLETED: 'success',
  REJECTED: 'danger',
  CHANGES_REQUESTED: 'warning',
  FORWARDED: 'info',
  CANCELLED: 'neutral',
  BYPASS: 'neutral'
};

@Component({
  selector: 'ep-approval-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    FormsModule,
    EpBadgeComponent,
    EpAmountComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpButtonComponent,
    EpBreadcrumbComponent,
    EpAvatarComponent,
    EpSlaBarComponent,
    EpModalComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpApprovalStepsComponent
  ],
  templateUrl: './approval-detail.component.html',
  styleUrl: './approval-detail.component.scss'
})
export class ApprovalDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly approvalsService = inject(ApprovalsService);
  private readonly userService = inject(AdminUserService);
  private readonly destroyRef = inject(DestroyRef);

  // ── State ──────────────────────────────────────────────────────────
  readonly taskId = signal<string | null>(null);
  readonly taskDetail = signal<ApprovalTaskDetail | null>(null);
  readonly task = computed(() => this.taskDetail()?.task);
  readonly entityPayload = computed(() => this.taskDetail()?.entitySnapshot);
  readonly process = signal<ApprovalProcessDetail | null>(null);
  readonly approvalSteps = computed(() => this.process()?.steps ?? []);
  readonly currentStepIndex = computed(() => this.process()?.currentStepIndex ?? null);
  readonly users = signal<AdminUserSummary[]>([]);
  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);

  // ── Modals State ──────────────────────────────────────────────────
  readonly activeModal = signal<'approve' | 'reject' | 'changes' | 'forward' | null>(null);
  readonly modalComment = signal('');
  readonly selectedForwardUser = signal('');

  readonly priorityTone = PRIORITY_TONE;
  readonly statusTone = STATUS_TONE;

  // ── Lifecycle ─────────────────────────────────────────────────────
  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.taskId.set(id);
      this.loadData();
      this.loadUsers();
    } else {
      this.router.navigate(['/approvals']);
    }
  }

  // ── Event Handlers ────────────────────────────────────────────────
  goBack(): void {
    this.router.navigate(['/approvals']);
  }

  canOpenPurchaseRequest(): boolean {
    return this.task()?.entityType === 'PURCHASE_REQUEST' && Boolean(this.task()?.entityId);
  }

  navigateToPurchaseRequest(): void {
    const entityId = this.task()?.entityId;
    if (entityId) {
      this.router.navigate(['/procurement', entityId]);
    }
  }

  openModal(type: 'approve' | 'reject' | 'changes' | 'forward'): void {
    this.modalComment.set('');
    this.selectedForwardUser.set('');
    this.activeModal.set(type);
  }

  closeModal(): void {
    this.activeModal.set(null);
  }

  submitAction(): void {
    const type = this.activeModal();
    const id = this.taskId();
    if (!id || !type) return;

    this.isSubmitting.set(true);

    if (type === 'approve') {
      this.approvalsService
        .approveTask(id, { comment: this.modalComment() || undefined })
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(() => {
            this.isSubmitting.set(false);
            this.closeModal();
          })
        )
        .subscribe({
          next: () => {
            this.router.navigate(['/approvals']);
          }
        });
    } else if (type === 'reject') {
      if (!this.modalComment().trim()) {
        this.isSubmitting.set(false);
        return;
      }
      this.approvalsService
        .rejectTask(id, { comment: this.modalComment() })
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(() => {
            this.isSubmitting.set(false);
            this.closeModal();
          })
        )
        .subscribe({
          next: () => {
            this.router.navigate(['/approvals']);
          }
        });
    } else if (type === 'changes') {
      if (!this.modalComment().trim()) {
        this.isSubmitting.set(false);
        return;
      }
      this.approvalsService
        .requestChanges(id, { comment: this.modalComment() })
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(() => {
            this.isSubmitting.set(false);
            this.closeModal();
          })
        )
        .subscribe({
          next: () => {
            this.router.navigate(['/approvals']);
          }
        });
    } else if (type === 'forward') {
      if (!this.selectedForwardUser()) {
        this.isSubmitting.set(false);
        return;
      }
      this.approvalsService
        .forwardTask(id, {
          forwardToUserId: this.selectedForwardUser(),
          reason: this.modalComment() || 'Chuyển tiếp tác vụ'
        })
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(() => {
            this.isSubmitting.set(false);
            this.closeModal();
          })
        )
        .subscribe({
          next: () => {
            this.router.navigate(['/approvals']);
          }
        });
    }
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Date(iso).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // ── Private ───────────────────────────────────────────────────────
  private loadData(): void {
    const id = this.taskId();
    if (!id) return;

    this.isLoading.set(true);

    this.approvalsService
      .getTaskDetail(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          if (res.data) {
            this.taskDetail.set(res.data);
            this.loadProcessDetail(res.data.task.entityType, res.data.task.entityId);
          }
        },
        error: () => {
          this.router.navigate(['/approvals']);
        }
      });
  }

  private loadProcessDetail(entityType: string, entityId: string): void {
    this.approvalsService
      .getProcessDetail(entityType, entityId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          if (res.data) {
            this.process.set(res.data);
          }
        }
      });
  }

  private loadUsers(): void {
    this.userService
      .list({ page: 1, size: 100 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          if (res.data) {
            this.users.set(res.data.filter(u => u.status === 'ACTIVE'));
          }
        }
      });
  }
}
