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
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpApprovalStepsComponent } from '../../../../shared/components/ep-approval-steps/ep-approval-steps.component';
import { EpPrLifecycleComponent } from '../../../../shared/components/ep-pr-lifecycle/ep-pr-lifecycle.component';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { ToastService } from '../../../../core/services/toast.service';

import { PurchaseRequestService } from '../../services/purchase-request.service';
import { BudgetCheckResult, Money, PrLineItemResponse, PrStatus, PurchaseRequestDetail } from '../../models/purchase-request.model';

const STATUS_TONE: Record<string, EpBadgeTone> = {
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

const PRIORITY_TONE: Record<string, EpBadgeTone> = {
  NORMAL: 'neutral',
  URGENT: 'warning',
  EMERGENCY: 'danger'
};

const APPROVAL_STEP_TONE: Record<string, EpBadgeTone> = {
  PENDING: 'neutral',
  APPROVED: 'success',
  REJECTED: 'danger',
  ESCALATED: 'warning',
  SKIPPED: 'neutral',
  FORWARDED: 'info'
};

const BUDGET_TONE: Record<string, EpBadgeTone> = {
  PASS: 'success',
  WARNING: 'warning',
  FAIL: 'danger'
};

@Component({
  selector: 'ep-pr-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpBadgeComponent,
    EpAmountComponent,
    EpCardComponent,
    EpButtonComponent,
    EpSkeletonComponent,
    EpBreadcrumbComponent,
    EpModalComponent,
    EpIconComponent,
    EpFormFieldComponent,
    EpApprovalStepsComponent,
    EpPrLifecycleComponent,
    HasPermissionDirective
  ],
  templateUrl: './pr-detail.component.html',
  styleUrl: './pr-detail.component.scss'
})
export class PrDetailComponent implements OnInit {
  private readonly prService = inject(PurchaseRequestService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly pr = signal<PurchaseRequestDetail | null>(null);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly isCancelling = signal(false);
  readonly showCancelModal = signal(false);

  readonly cancelReason = new FormControl('', [Validators.required, Validators.minLength(10)]);

  readonly statusTone = computed(() => STATUS_TONE[this.pr()?.status ?? ''] ?? 'neutral');
  readonly priorityTone = computed(() => PRIORITY_TONE[this.pr()?.priority ?? ''] ?? 'neutral');
  readonly totalLineItems = computed(() => this.pr()?.lineItems?.length ?? 0);
  readonly budgetUsagePercent = computed(() => this.calculateBudgetUsage(this.pr()?.budgetCheck ?? null));
  readonly approvalSteps = computed(() => this.pr()?.approvalProcess?.steps ?? []);
  readonly currentApprovalStep = computed(() => this.pr()?.approvalProcess?.currentStep ?? null);

  readonly canEdit = computed(() => {
    const status = this.pr()?.status as PrStatus;
    return status === 'DRAFT' || status === 'CHANGES_REQUESTED';
  });

  readonly canSubmit = computed(() => {
    const status = this.pr()?.status as PrStatus;
    return status === 'DRAFT' || status === 'CHANGES_REQUESTED';
  });

  readonly canCancel = computed(() => {
    const status = this.pr()?.status as PrStatus;
    return status === 'DRAFT' || status === 'SUBMITTED' || status === 'CHANGES_REQUESTED';
  });

  readonly canCreatePo = computed(() => this.pr()?.status === 'APPROVED');

  readonly canCreateRfq = computed(() => this.pr()?.status === 'APPROVED');

  readonly approvalStepTone = APPROVAL_STEP_TONE;
  readonly budgetTone = BUDGET_TONE;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPr(id);
    }
  }

  navigateEdit(): void {
    const id = this.pr()?.id;
    if (!id) {
      return;
    }
    this.router.navigate(['/procurement', 'create'], {
      queryParams: { edit: id }
    });
  }

  navigateCreatePo(): void {
    const data = this.pr();
    if (!data) {
      return;
    }
    this.router.navigate(['/finance', 'purchase-orders', 'create'], {
      queryParams: {
        prId: data.id,
        vendorId: this.preferredVendorId(data)
      }
    });
  }

  navigateCreateRfq(): void {
    const data = this.pr();
    if (!data) {
      return;
    }
    this.router.navigate(['/vendors', 'rfq', 'create'], {
      queryParams: { prId: data.id }
    });
  }

  navigateApprovalInbox(): void {
    this.router.navigate(['/approvals']);
  }

  onSubmit(): void {
    const id = this.pr()?.id;
    if (!id) {
      return;
    }
    this.isSubmitting.set(true);
    this.prService.submit(id)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.toastService.successKey('pr.detail.toast.submitSuccess');
          this.loadPr(id);
        }
      });
  }

  openCancelModal(): void {
    this.cancelReason.reset('');
    this.showCancelModal.set(true);
  }

  closeCancelModal(): void {
    this.showCancelModal.set(false);
  }

  onConfirmCancel(): void {
    if (this.cancelReason.invalid) {
      this.cancelReason.markAsTouched();
      return;
    }
    const id = this.pr()?.id;
    if (!id) {
      return;
    }
    this.isCancelling.set(true);
    this.prService.cancel(id, { reason: this.cancelReason.value! })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isCancelling.set(false)))
      .subscribe({
        next: () => {
          this.toastService.successKey('pr.detail.toast.cancelSuccess');
          this.showCancelModal.set(false);
          this.loadPr(id);
        }
      });
  }

  navigateBack(): void {
    this.router.navigate(['/procurement']);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '--';
    }
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(iso));
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

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  requesterLabel(data: PurchaseRequestDetail): string {
    return data.requester?.fullName || this.shortId(data.requesterId);
  }

  requesterDepartmentLabel(data: PurchaseRequestDetail): string {
    return data.requester?.department || this.shortId(data.departmentId);
  }

  shortId(value: string | null | undefined): string {
    if (!value) {
      return '--';
    }
    if (value.length <= 12) {
      return value;
    }
    return `${value.slice(0, 8)}...${value.slice(-4)}`;
  }

  quantityLabel(item: PrLineItemResponse): string {
    if (!item.quantity) {
      return '--';
    }
    return `${this.trimDecimal(item.quantity.amount)} ${item.quantity.unit}`;
  }

  trimDecimal(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '--';
    }
    const trimmed = String(value).replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, '');
    return trimmed === '' ? '0' : trimmed;
  }

  budgetStatusTone(status: string | null | undefined): EpBadgeTone {
    return BUDGET_TONE[status ?? ''] ?? 'neutral';
  }

  approvalStatusTone(status: string | null | undefined): EpBadgeTone {
    return APPROVAL_STEP_TONE[status ?? ''] ?? 'neutral';
  }

  private loadPr(id: string): void {
    this.isLoading.set(true);
    this.prService.getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (res) => this.pr.set(res.data),
        error: () => this.router.navigate(['/procurement'])
      });
  }

  private calculateBudgetUsage(budget: BudgetCheckResult | null): string {
    if (!budget) {
      return '0%';
    }
    const allocated = this.amountNumber(budget.allocated);
    if (allocated <= 0) {
      return '0%';
    }
    const used = this.amountNumber(budget.committed) + this.amountNumber(budget.spent);
    const percent = Math.max(0, Math.min(100, (used / allocated) * 100));
    return `${percent.toFixed(0)}%`;
  }

  private amountNumber(value: Money | string | number | null | undefined): number {
    const raw = typeof value === 'object' && value !== null ? value.amount : value;
    const parsed = Number(raw ?? 0);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private preferredVendorId(data: PurchaseRequestDetail): string | null {
    return data.lineItems.find((item) => Boolean(item.preferredVendorId))?.preferredVendorId ?? null;
  }
}
