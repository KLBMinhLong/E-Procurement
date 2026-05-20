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
import { EpSlaBarComponent } from '../../../../shared/components/ep-sla-bar/ep-sla-bar.component';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';

import { PurchaseRequestService } from '../../services/purchase-request.service';
import { PrStatus, PurchaseRequestDetail } from '../../models/purchase-request.model';

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
    EpSlaBarComponent,
    HasPermissionDirective
  ],
  templateUrl: './pr-detail.component.html',
  styleUrl: './pr-detail.component.scss'
})
export class PrDetailComponent implements OnInit {
  private readonly prService = inject(PurchaseRequestService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // ── State ──────────────────────────────────────────────────────────
  readonly pr = signal<PurchaseRequestDetail | null>(null);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly isCancelling = signal(false);
  readonly showCancelModal = signal(false);

  readonly cancelReason = new FormControl('', [Validators.required, Validators.minLength(10)]);

  // ── Computed helpers ───────────────────────────────────────────────
  readonly statusTone = computed(() => STATUS_TONE[this.pr()?.status ?? ''] ?? 'neutral');
  readonly priorityTone = computed(() => PRIORITY_TONE[this.pr()?.priority ?? ''] ?? 'neutral');

  readonly canEdit = computed(() => {
    const s = this.pr()?.status as PrStatus;
    return s === 'DRAFT' || s === 'CHANGES_REQUESTED';
  });

  readonly canSubmit = computed(() => {
    const s = this.pr()?.status as PrStatus;
    return s === 'DRAFT' || s === 'CHANGES_REQUESTED';
  });

  readonly canCancel = computed(() => {
    const s = this.pr()?.status as PrStatus;
    return s === 'DRAFT' || s === 'SUBMITTED' || s === 'CHANGES_REQUESTED';
  });

  readonly approvalStepTone = APPROVAL_STEP_TONE;
  readonly budgetTone = BUDGET_TONE;

  // ── Lifecycle ──────────────────────────────────────────────────────
  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadPr(id);
  }

  // ── Actions ────────────────────────────────────────────────────────
  navigateEdit(): void {
    this.router.navigate(['/procurement', 'create'], {
      queryParams: { edit: this.pr()?.id }
    });
  }

  onSubmit(): void {
    const id = this.pr()?.id;
    if (!id) return;
    this.isSubmitting.set(true);
    this.prService.submit(id)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isSubmitting.set(false)))
      .subscribe({ next: () => this.loadPr(id) });
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
    if (!id) return;
    this.isCancelling.set(true);
    this.prService.cancel(id, { reason: this.cancelReason.value! })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isCancelling.set(false)))
      .subscribe({
        next: () => {
          this.showCancelModal.set(false);
          this.loadPr(id);
        }
      });
  }

  navigateBack(): void {
    this.router.navigate(['/procurement']);
  }

  // ── Helpers ────────────────────────────────────────────────────────
  formatDate(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Date(iso).toLocaleDateString('vi-VN');
  }

  formatDateTime(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Date(iso).toLocaleString('vi-VN');
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  // ── Private ────────────────────────────────────────────────────────
  private loadPr(id: string): void {
    this.isLoading.set(true);
    this.prService.getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (res) => this.pr.set(res.data),
        error: () => this.router.navigate(['/procurement'])
      });
  }
}
