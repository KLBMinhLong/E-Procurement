import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { ToastService } from '../../../../core/services/toast.service';
import {
  lineTotalMoney,
  lineUnitMoney,
  PoLineItem,
  PurchaseOrder,
  purchaseOrderMoney
} from '../../models/purchase-order.model';
import { PurchaseOrderService } from '../../services/purchase-order.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  DRAFT: 'neutral',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'success',
  SENT_TO_VENDOR: 'info',
  PARTIALLY_RECEIVED: 'warning',
  FULLY_RECEIVED: 'success',
  INVOICED: 'info',
  PAID: 'success',
  CLOSED: 'neutral',
  CANCELLED: 'danger'
};

const CALLBACK_TONE: Record<string, EpBadgeTone> = {
  PENDING: 'warning',
  DELIVERED: 'success',
  FAILED_RETRYABLE: 'warning',
  FAILED_EXHAUSTED: 'danger'
};

const CANCELLABLE_STATUSES = new Set(['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SENT_TO_VENDOR']);
const SENDABLE_STATUSES = new Set(['DRAFT', 'APPROVED']);
const INVOICEABLE_STATUSES = new Set(['SENT_TO_VENDOR', 'PARTIALLY_RECEIVED', 'FULLY_RECEIVED', 'INVOICED']);

@Component({
  selector: 'ep-po-detail',
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
  templateUrl: './po-detail.component.html',
  styleUrl: './po-detail.component.scss'
})
export class PoDetailComponent implements OnInit {
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly po = signal<PurchaseOrder | null>(null);
  readonly isLoading = signal(true);
  readonly isSavingDraft = signal(false);
  readonly isSending = signal(false);
  readonly isCancelling = signal(false);
  readonly showEditModal = signal(false);
  readonly showSendModal = signal(false);
  readonly showCancelModal = signal(false);
  readonly statusTone = computed(() => STATUS_TONE[this.po()?.status ?? ''] ?? 'neutral');
  readonly callbackTone = computed(() => CALLBACK_TONE[this.po()?.prConversionStatus ?? ''] ?? 'neutral');
  readonly totalLines = computed(() => this.po()?.lineItems.length ?? 0);
  readonly canEditDraft = computed(() => this.po()?.status === 'DRAFT');
  readonly canCancel = computed(() => CANCELLABLE_STATUSES.has(this.po()?.status ?? ''));
  readonly canCreateInvoice = computed(() => INVOICEABLE_STATUSES.has(this.po()?.status ?? ''));
  readonly canSend = computed(() => !this.sendBlockKey());
  readonly sendBlockKey = computed(() => {
    const data = this.po();
    if (!data) {
      return 'finance.po.detail.actionHint.noData';
    }
    if (!SENDABLE_STATUSES.has(data.status)) {
      return 'finance.po.detail.actionHint.notSendableStatus';
    }
    if (data.prConversionStatus !== 'DELIVERED') {
      return 'finance.po.detail.actionHint.waitingPrCallback';
    }
    if (!data.vendor.email) {
      return 'finance.po.detail.actionHint.missingVendorEmail';
    }
    return null;
  });

  readonly editForm = this.fb.group({
    deliveryAddress: ['', [Validators.required, Validators.maxLength(1000)]],
    deliveryDeadline: [''],
    paymentTerms: ['', [Validators.maxLength(100)]]
  });
  readonly sendNote = new FormControl('', [Validators.maxLength(1000)]);
  readonly cancelReason = new FormControl('', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPo(id);
    }
  }

  navigateBack(): void {
    this.router.navigate(['/finance', 'purchase-orders']);
  }

  reload(): void {
    const id = this.po()?.id ?? this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPo(id);
    }
  }

  navigateCreateInvoice(): void {
    const id = this.po()?.id;
    if (id) {
      this.router.navigate(['/finance', 'invoices', 'create'], { queryParams: { poId: id } });
    }
  }

  openEditModal(): void {
    const data = this.po();
    if (!data || !this.canEditDraft()) {
      return;
    }
    this.editForm.reset({
      deliveryAddress: data.deliveryAddress ?? '',
      deliveryDeadline: data.deliveryDeadline ?? '',
      paymentTerms: data.paymentTerms ?? ''
    });
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    if (!this.isSavingDraft()) {
      this.showEditModal.set(false);
    }
  }

  saveDraft(): void {
    const id = this.po()?.id;
    if (!id) {
      return;
    }
    this.editForm.markAllAsTouched();
    if (this.editForm.invalid || this.isSavingDraft()) {
      return;
    }
    const raw = this.editForm.getRawValue();
    this.isSavingDraft.set(true);
    this.purchaseOrderService.updateDraft(id, {
      deliveryAddress: raw.deliveryAddress!.trim(),
      deliveryDeadline: raw.deliveryDeadline || null,
      paymentTerms: raw.paymentTerms?.trim() || null
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSavingDraft.set(false))
      )
      .subscribe({
        next: (response) => {
          this.po.set(response.data);
          this.showEditModal.set(false);
          this.toastService.successKey('finance.po.detail.toast.updated');
        }
      });
  }

  openSendModal(): void {
    if (!this.canSend()) {
      return;
    }
    this.sendNote.reset('');
    this.showSendModal.set(true);
  }

  closeSendModal(): void {
    if (!this.isSending()) {
      this.showSendModal.set(false);
    }
  }

  confirmSend(): void {
    const id = this.po()?.id;
    if (!id || this.sendNote.invalid || this.isSending()) {
      this.sendNote.markAsTouched();
      return;
    }
    this.isSending.set(true);
    this.purchaseOrderService.send(id, {
      additionalNote: this.sendNote.value?.trim() || null
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSending.set(false))
      )
      .subscribe({
        next: (response) => {
          this.po.set(response.data);
          this.showSendModal.set(false);
          this.toastService.successKey('finance.po.detail.toast.sent');
        }
      });
  }

  openCancelModal(): void {
    if (!this.canCancel()) {
      return;
    }
    this.cancelReason.reset('');
    this.showCancelModal.set(true);
  }

  closeCancelModal(): void {
    if (!this.isCancelling()) {
      this.showCancelModal.set(false);
    }
  }

  confirmCancel(): void {
    const id = this.po()?.id;
    this.cancelReason.markAsTouched();
    if (!id || this.cancelReason.invalid || this.isCancelling()) {
      return;
    }
    this.isCancelling.set(true);
    this.purchaseOrderService.cancel(id, {
      reason: this.cancelReason.value!.trim()
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isCancelling.set(false))
      )
      .subscribe({
        next: (response) => {
          this.po.set(response.data);
          this.showCancelModal.set(false);
          this.toastService.successKey('finance.po.detail.toast.cancelled');
        }
      });
  }

  amount(po: PurchaseOrder) {
    return purchaseOrderMoney(po);
  }

  lineUnitAmount(line: PoLineItem) {
    return lineUnitMoney(line);
  }

  lineTotalAmount(line: PoLineItem) {
    return lineTotalMoney(line);
  }

  quantityLabel(line: PoLineItem): string {
    return `${this.trimDecimal(line.quantity.amount)} ${line.quantity.unit}`;
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

  shortId(value: string | null | undefined): string {
    if (!value) {
      return '--';
    }
    return value.length <= 12 ? value : `${value.slice(0, 8)}...${value.slice(-4)}`;
  }

  fieldError(controlName: 'deliveryAddress' | 'deliveryDeadline' | 'paymentTerms'): string | null {
    const control = this.editForm.get(controlName);
    if (!control || (!control.touched && !control.dirty)) {
      return null;
    }
    if (control.hasError('required')) {
      return 'finance.po.detail.validation.required';
    }
    if (control.hasError('maxlength')) {
      return 'finance.po.detail.validation.maxLength';
    }
    return null;
  }

  noteError(): string | null {
    return this.sendNote.touched && this.sendNote.hasError('maxlength')
      ? 'finance.po.detail.validation.maxLength'
      : null;
  }

  cancelReasonError(): string | null {
    if (!this.cancelReason.touched) {
      return null;
    }
    if (this.cancelReason.hasError('required')) {
      return 'finance.po.detail.validation.required';
    }
    if (this.cancelReason.hasError('minlength')) {
      return 'finance.po.detail.validation.cancelMinLength';
    }
    if (this.cancelReason.hasError('maxlength')) {
      return 'finance.po.detail.validation.maxLength';
    }
    return null;
  }

  private loadPo(id: string): void {
    this.isLoading.set(true);
    this.purchaseOrderService
      .getById(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => this.po.set(res.data),
        error: () => this.router.navigate(['/finance', 'purchase-orders'])
      });
  }

  private trimDecimal(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '--';
    }
    const trimmed = String(value).replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, '');
    return trimmed === '' ? '0' : trimmed;
  }
}
