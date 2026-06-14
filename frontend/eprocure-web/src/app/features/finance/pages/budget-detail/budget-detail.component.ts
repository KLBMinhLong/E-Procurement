import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { ToastService } from '../../../../core/services/toast.service';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import {
  BudgetDashboard,
  BudgetHealthTone,
  BudgetStatus,
  budgetHealthTone,
  budgetMoney
} from '../../models/budget.model';
import { BudgetService } from '../../services/budget.service';

const STATUS_TONE: Record<BudgetStatus, EpBadgeTone> = {
  PLANNING: 'neutral',
  SUBMITTED: 'warning',
  APPROVED: 'info',
  ACTIVE: 'success',
  CLOSED: 'neutral'
};

const HEALTH_TONE: Record<BudgetHealthTone, EpBadgeTone> = {
  HEALTHY: 'success',
  WARNING: 'warning',
  EXCEEDED: 'danger'
};

const UUID_PATTERN = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$/;
const MONEY_PATTERN = /^\d+(\.\d{1,4})?$/;
const CURRENCY_PATTERN = /^[A-Z]{3}$/;

@Component({
  selector: 'ep-budget-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpAmountComponent,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpModalComponent,
    HasPermissionDirective,
    EpSkeletonComponent
  ],
  templateUrl: './budget-detail.component.html',
  styleUrl: './budget-detail.component.scss'
})
export class BudgetDetailComponent implements OnInit {
  private readonly budgetService = inject(BudgetService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly budget = signal<BudgetDashboard | null>(null);
  readonly isLoading = signal(true);
  readonly isSubmittingOverride = signal(false);
  readonly isSubmittingTransfer = signal(false);
  readonly isLoadingTransferTargets = signal(false);
  readonly showOverrideModal = signal(false);
  readonly showTransferModal = signal(false);
  readonly transferTargets = signal<BudgetDashboard[]>([]);
  readonly statusTone = computed(() => STATUS_TONE[this.budget()?.status ?? 'PLANNING']);
  readonly health = computed(() => {
    const data = this.budget();
    return data ? budgetHealthTone(data) : 'HEALTHY';
  });
  readonly healthTone = computed(() => HEALTH_TONE[this.health()]);
  readonly utilizationPercent = computed(() => {
    const data = this.budget();
    return data ? this.percentOfAllocated(data, Number(data.committed ?? 0) + Number(data.spent ?? 0)) : 0;
  });
  readonly forecastIsSoon = computed(() => {
    const value = this.budget()?.forecastExhaustedAt;
    if (!value) {
      return false;
    }
    const forecast = new Date(value).getTime();
    const now = Date.now();
    const thirtyDays = 30 * 24 * 60 * 60 * 1000;
    return forecast >= now && forecast - now <= thirtyDays;
  });
  readonly canUseBudgetActions = computed(() => this.budget()?.status === 'ACTIVE');

  readonly overrideForm = this.fb.group({
    prId: ['', [Validators.required, Validators.pattern(UUID_PATTERN)]],
    overrideAmount: ['', [Validators.required, Validators.min(0.0001), Validators.pattern(MONEY_PATTERN)]],
    currency: ['VND', [Validators.required, Validators.pattern(CURRENCY_PATTERN)]],
    overrideReason: ['', [Validators.required, Validators.minLength(50), Validators.maxLength(1000)]]
  });

  readonly transferForm = this.fb.group({
    targetBudgetId: ['', [Validators.required, Validators.pattern(UUID_PATTERN)]],
    amount: ['', [Validators.required, Validators.min(0.0001), Validators.pattern(MONEY_PATTERN)]],
    currency: ['VND', [Validators.required, Validators.pattern(CURRENCY_PATTERN)]],
    reason: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(1000)]]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadBudget(id);
    } else {
      this.isLoading.set(false);
    }
  }

  navigateBack(): void {
    this.router.navigate(['/finance', 'budgets']);
  }

  reload(): void {
    const id = this.budget()?.id ?? this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadBudget(id);
    }
  }

  openOverrideModal(): void {
    if (!this.canUseBudgetActions()) {
      return;
    }
    this.overrideForm.reset({
      prId: '',
      overrideAmount: '',
      currency: 'VND',
      overrideReason: ''
    });
    this.showOverrideModal.set(true);
  }

  closeOverrideModal(): void {
    if (!this.isSubmittingOverride()) {
      this.showOverrideModal.set(false);
    }
  }

  submitOverride(): void {
    const id = this.budget()?.id;
    this.overrideForm.markAllAsTouched();
    if (!id || this.overrideForm.invalid || this.isSubmittingOverride()) {
      return;
    }

    const raw = this.overrideForm.getRawValue();
    this.isSubmittingOverride.set(true);
    this.budgetService
      .override(id, {
        prId: raw.prId!.trim(),
        overrideAmount: raw.overrideAmount!.trim(),
        currency: raw.currency!.trim(),
        overrideReason: raw.overrideReason!.trim()
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmittingOverride.set(false))
      )
      .subscribe({
        next: () => {
          this.showOverrideModal.set(false);
          this.toastService.successKey('finance.budget.detail.toast.overrideApproved');
          this.loadBudget(id);
        }
      });
  }

  openTransferModal(): void {
    if (!this.canUseBudgetActions()) {
      return;
    }
    this.transferForm.reset({
      targetBudgetId: '',
      amount: '',
      currency: 'VND',
      reason: ''
    });
    this.showTransferModal.set(true);
    this.loadTransferTargets();
  }

  closeTransferModal(): void {
    if (!this.isSubmittingTransfer()) {
      this.showTransferModal.set(false);
    }
  }

  submitTransfer(): void {
    const id = this.budget()?.id;
    this.transferForm.markAllAsTouched();
    if (!id || this.transferForm.invalid || this.transferAmountExceedsAvailable() || this.transferTargetIsCurrent() || this.isSubmittingTransfer()) {
      return;
    }

    const raw = this.transferForm.getRawValue();
    this.isSubmittingTransfer.set(true);
    this.budgetService
      .transfer(id, {
        targetBudgetId: raw.targetBudgetId!.trim(),
        amount: raw.amount!.trim(),
        currency: raw.currency!.trim(),
        reason: raw.reason!.trim()
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmittingTransfer.set(false))
      )
      .subscribe({
        next: (response) => {
          this.showTransferModal.set(false);
          if (response.data?.sourceDashboard) {
            this.budget.set(response.data.sourceDashboard);
          }
          this.toastService.successKey('finance.budget.detail.toast.transferCompleted');
          this.loadBudget(id);
        }
      });
  }

  amount(budget: BudgetDashboard, field: 'allocated' | 'committed' | 'spent' | 'available') {
    return budgetMoney(budget, field);
  }

  detailTitle(budget: BudgetDashboard): string {
    return `${budget.glAccountCode} - ${this.shortId(budget.departmentId)}`;
  }

  periodLabelKey(budget: BudgetDashboard): string {
    return budget.quarter ? 'finance.budget.detail.period.quarter' : 'finance.budget.detail.period.allYear';
  }

  utilizationWidth(): string {
    return `${Math.min(this.utilizationPercent(), 100)}%`;
  }

  segmentWidth(budget: BudgetDashboard, field: 'committed' | 'spent' | 'available'): string {
    return `${this.percentOfAllocated(budget, Math.max(0, Number(budget[field] ?? 0)))}%`;
  }

  burnRateText(budget: BudgetDashboard): string {
    return budget.burnRatePerMonth ?? '0';
  }

  transferAmountExceedsAvailable(): boolean {
    const amount = Number(this.transferForm.controls.amount.value ?? 0);
    const available = Number(this.budget()?.available ?? 0);
    return amount > available;
  }

  transferTargetIsCurrent(): boolean {
    const targetBudgetId = this.transferForm.controls.targetBudgetId.value?.trim();
    return Boolean(targetBudgetId && targetBudgetId === this.budget()?.id);
  }

  targetLabel(target: BudgetDashboard): string {
    return `${target.glAccountCode} - ${this.shortId(target.departmentId)}`;
  }

  overrideError(controlName: 'prId' | 'overrideAmount' | 'currency' | 'overrideReason'): string | null {
    const control = this.overrideForm.controls[controlName];
    if (!control.touched && !control.dirty) {
      return null;
    }
    if (control.hasError('required')) {
      return 'finance.budget.detail.validation.required';
    }
    if (control.hasError('pattern')) {
      return controlName === 'prId'
        ? 'finance.budget.detail.validation.uuid'
        : 'finance.budget.detail.validation.format';
    }
    if (control.hasError('min')) {
      return 'finance.budget.detail.validation.amountPositive';
    }
    if (control.hasError('minlength')) {
      return 'finance.budget.detail.validation.overrideReasonMinLength';
    }
    if (control.hasError('maxlength')) {
      return 'finance.budget.detail.validation.maxLength';
    }
    return null;
  }

  transferError(controlName: 'targetBudgetId' | 'amount' | 'currency' | 'reason'): string | null {
    const control = this.transferForm.controls[controlName];
    if (!control.touched && !control.dirty) {
      return null;
    }
    if (control.hasError('required')) {
      return 'finance.budget.detail.validation.required';
    }
    if (control.hasError('pattern')) {
      return controlName === 'targetBudgetId'
        ? 'finance.budget.detail.validation.uuid'
        : 'finance.budget.detail.validation.format';
    }
    if (control.hasError('min')) {
      return 'finance.budget.detail.validation.amountPositive';
    }
    if (controlName === 'amount' && this.transferAmountExceedsAvailable()) {
      return 'finance.budget.detail.validation.amountExceedsAvailable';
    }
    if (controlName === 'targetBudgetId' && this.transferTargetIsCurrent()) {
      return 'finance.budget.detail.validation.targetDifferent';
    }
    if (control.hasError('minlength')) {
      return 'finance.budget.detail.validation.transferReasonMinLength';
    }
    if (control.hasError('maxlength')) {
      return 'finance.budget.detail.validation.maxLength';
    }
    return null;
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '--';
    }
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(iso));
  }

  shortId(value: string | null | undefined): string {
    if (!value) {
      return '--';
    }
    return value.length <= 12 ? value : `${value.slice(0, 8)}...${value.slice(-4)}`;
  }

  private loadBudget(id: string): void {
    this.isLoading.set(true);
    this.budgetService
      .getDashboard(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => this.budget.set(res.data),
        error: () => this.budget.set(null)
      });
  }

  private loadTransferTargets(): void {
    const data = this.budget();
    if (!data) {
      this.transferTargets.set([]);
      return;
    }

    this.isLoadingTransferTargets.set(true);
    this.budgetService
      .list({
        page: 1,
        size: 50,
        sort: 'glAccountCode,asc',
        fiscal_year: data.fiscalYear,
        quarter: data.quarter ?? undefined,
        status: 'ACTIVE'
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoadingTransferTargets.set(false))
      )
      .subscribe({
        next: (res) => this.transferTargets.set((res.data ?? []).filter((target) => target.id !== data.id)),
        error: () => this.transferTargets.set([])
      });
  }

  private percentOfAllocated(budget: BudgetDashboard, value: number): number {
    const allocated = Number(budget.allocated ?? 0);
    if (allocated <= 0) {
      return 0;
    }
    return Math.max(0, Math.round((value / allocated) * 100));
  }
}
