import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, finalize, forkJoin, of } from 'rxjs';

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
  Invoice,
  InvoiceLineItem,
  InvoiceStatus,
  MatchStatus,
  invoiceLineTotalMoney,
  invoiceLineUnitMoney,
  invoiceSubtotalMoney,
  invoiceTaxMoney,
  invoiceTotalMoney
} from '../../models/invoice.model';
import { PoLineItem, PurchaseOrder } from '../../models/purchase-order.model';
import { InvoiceService } from '../../services/invoice.service';
import { PurchaseOrderService } from '../../services/purchase-order.service';
import { Money } from '../../../procurement/models/purchase-request.model';
import { GoodsReceiptDetail } from '../../../inventory/models/goods-receipt.model';
import { GoodsReceiptService } from '../../../inventory/services/goods-receipt.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  PENDING_MATCH: 'warning',
  MATCHED: 'success',
  MISMATCHED: 'danger',
  APPROVED: 'info',
  DISPUTED: 'warning',
  PAID: 'success',
  CANCELLED: 'neutral'
};

const MATCH_TONE: Record<string, EpBadgeTone> = {
  MATCHED: 'success',
  MISMATCHED: 'danger',
  PARTIAL: 'warning'
};

const MATCHABLE_STATUSES = new Set<InvoiceStatus>(['PENDING_MATCH', 'MISMATCHED', 'DISPUTED']);

interface GrQuantitySummary {
  received: number;
  rejected: number;
}

interface MatchRow {
  poLineItemId: string;
  lineNumber: number;
  itemName: string;
  categoryCode: string;
  unit: string;
  poQty: number | null;
  poUnitPrice: string | null;
  grReceivedQty: number | null;
  grRejectedQty: number;
  invoiceQty: number;
  invoiceUnitPrice: string;
  invoiceTotalPrice: string;
  priceVariancePct: number;
  quantityVariance: number | null;
  hasVariance: boolean;
}

@Component({
  selector: 'ep-invoice-detail',
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
    EpSkeletonComponent,
    HasPermissionDirective
  ],
  templateUrl: './invoice-detail.component.html',
  styleUrl: './invoice-detail.component.scss'
})
export class InvoiceDetailComponent implements OnInit {
  private readonly invoiceService = inject(InvoiceService);
  private readonly poService = inject(PurchaseOrderService);
  private readonly grService = inject(GoodsReceiptService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly invoice = signal<Invoice | null>(null);
  readonly isLoading = signal(true);
  readonly isMatching = signal(false);
  readonly isApproving = signal(false);
  readonly isDisputing = signal(false);
  readonly isPaying = signal(false);
  readonly isLoadingMatchData = signal(false);
  readonly showApproveModal = signal(false);
  readonly showDisputeModal = signal(false);
  readonly showPaymentModal = signal(false);
  readonly lastMatchRequiresReview = signal<boolean | null>(null);
  readonly poDetail = signal<PurchaseOrder | null>(null);
  readonly completedGrs = signal<GoodsReceiptDetail[]>([]);
  readonly statusTone = computed(() => STATUS_TONE[this.invoice()?.status ?? ''] ?? 'neutral');
  readonly canMatch = computed(() => MATCHABLE_STATUSES.has(this.invoice()?.status ?? 'PENDING_MATCH'));
  readonly canApprove = computed(() => this.invoice()?.status === 'MATCHED');
  readonly canDispute = computed(() => this.invoice()?.status === 'MISMATCHED');
  readonly canPay = computed(() => this.invoice()?.status === 'APPROVED');
  readonly totalLines = computed(() => this.invoice()?.lineItems.length ?? 0);
  readonly poMatchTone = computed(() => this.matchTone(this.invoice()?.matchResult?.poMatchStatus ?? null));
  readonly grMatchTone = computed(() => this.matchTone(this.invoice()?.matchResult?.grMatchStatus ?? null));
  readonly poMatchClass = computed(() => this.matchClass(this.invoice()?.matchResult?.poMatchStatus ?? null));
  readonly grMatchClass = computed(() => this.matchClass(this.invoice()?.matchResult?.grMatchStatus ?? null));
  readonly poMatchIcon = computed(() => this.matchIcon(this.invoice()?.matchResult?.poMatchStatus ?? null));
  readonly grMatchIcon = computed(() => this.matchIcon(this.invoice()?.matchResult?.grMatchStatus ?? null));
  readonly qtyVarianceNum = computed(() => this.parseDecimal(this.invoice()?.matchResult?.qtyVariance));
  readonly priceVarianceNum = computed(() => this.parseDecimal(this.invoice()?.matchResult?.priceVariance));
  readonly absPriceVariance = computed<Money>(() => ({
    amount: Math.abs(this.priceVarianceNum()).toFixed(4),
    currency: this.invoice()?.currency ?? 'VND'
  }));
  readonly priceVarianceTone = computed<EpBadgeTone>(() => (this.priceVarianceNum() === 0 ? 'success' : 'warning'));
  readonly priceVarianceLabelKey = computed(() => {
    const variance = this.priceVarianceNum();
    if (variance > 0) {
      return 'finance.invoice.match.overCharged';
    }
    if (variance < 0) {
      return 'finance.invoice.match.underCharged';
    }
    return 'finance.invoice.match.exact';
  });
  readonly grSummary = computed<Map<string, GrQuantitySummary>>(() => {
    const summary = new Map<string, GrQuantitySummary>();
    for (const gr of this.completedGrs()) {
      for (const line of gr.lineItems ?? []) {
        const current = summary.get(line.poLineItemId) ?? { received: 0, rejected: 0 };
        current.received += this.parseDecimal(line.receivedQuantity);
        current.rejected += this.parseDecimal(line.rejectedQuantity);
        summary.set(line.poLineItemId, current);
      }
    }
    return summary;
  });
  readonly matchRows = computed<MatchRow[]>(() => {
    const invoice = this.invoice();
    if (!invoice) {
      return [];
    }
    const poLines = this.poDetail()?.lineItems ?? [];
    const poLineMap = new Map(poLines.map((line) => [line.id, line]));
    const grSummary = this.grSummary();

    return invoice.lineItems.map((invoiceLine) => {
      const poLine = poLineMap.get(invoiceLine.poLineItemId);
      return this.buildMatchRow(invoice, invoiceLine, poLine, grSummary.get(invoiceLine.poLineItemId) ?? null);
    });
  });
  readonly matchDataUnavailable = computed(
    () => !this.isLoadingMatchData() && this.invoice()?.status !== 'PENDING_MATCH' && !this.poDetail()
  );

  readonly approveComment = new FormControl('', [Validators.maxLength(1000)]);
  readonly disputeReason = new FormControl('', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]);
  readonly paymentForm = this.fb.group({
    paymentDate: [this.today(), [Validators.required]],
    paymentReference: ['', [Validators.required, Validators.maxLength(120)]],
    paidAmount: ['', [Validators.required, Validators.min(0.0001)]],
    notes: ['', [Validators.maxLength(1000)]]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadInvoice(id);
    }
  }

  navigateBack(): void {
    this.router.navigate(['/finance', 'invoices']);
  }

  navigatePo(): void {
    const poId = this.invoice()?.po.id;
    if (poId) {
      this.router.navigate(['/finance', 'purchase-orders', poId]);
    }
  }

  reload(): void {
    const id = this.invoice()?.id ?? this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadInvoice(id);
    }
  }

  runMatch(): void {
    const id = this.invoice()?.id;
    if (!id || !this.canMatch() || this.isMatching()) {
      return;
    }
    this.isMatching.set(true);
    this.invoiceService
      .match(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isMatching.set(false))
      )
      .subscribe({
        next: (res) => {
          this.lastMatchRequiresReview.set(res.data.requiresManualReview);
          this.toastService.successKey('finance.invoice.detail.toast.matched');
          this.loadInvoice(id);
        }
      });
  }

  openApproveModal(): void {
    if (!this.canApprove()) {
      return;
    }
    this.approveComment.reset('');
    this.showApproveModal.set(true);
  }

  closeApproveModal(): void {
    if (!this.isApproving()) {
      this.showApproveModal.set(false);
    }
  }

  confirmApprove(): void {
    const id = this.invoice()?.id;
    this.approveComment.markAsTouched();
    if (!id || this.approveComment.invalid || this.isApproving()) {
      return;
    }
    this.isApproving.set(true);
    this.invoiceService
      .approve(id, { comment: this.approveComment.value?.trim() || null })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isApproving.set(false))
      )
      .subscribe({
        next: () => {
          this.showApproveModal.set(false);
          this.toastService.successKey('finance.invoice.detail.toast.approved');
          this.loadInvoice(id);
        }
      });
  }

  openDisputeModal(): void {
    if (!this.canDispute()) {
      return;
    }
    this.disputeReason.reset('');
    this.showDisputeModal.set(true);
  }

  closeDisputeModal(): void {
    if (!this.isDisputing()) {
      this.showDisputeModal.set(false);
    }
  }

  confirmDispute(): void {
    const id = this.invoice()?.id;
    this.disputeReason.markAsTouched();
    if (!id || this.disputeReason.invalid || this.isDisputing()) {
      return;
    }
    this.isDisputing.set(true);
    this.invoiceService
      .dispute(id, { reason: this.disputeReason.value!.trim() })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isDisputing.set(false))
      )
      .subscribe({
        next: () => {
          this.showDisputeModal.set(false);
          this.toastService.successKey('finance.invoice.detail.toast.disputed');
          this.loadInvoice(id);
        }
      });
  }

  openPaymentModal(): void {
    const data = this.invoice();
    if (!data || !this.canPay()) {
      return;
    }
    this.paymentForm.reset({
      paymentDate: this.today(),
      paymentReference: '',
      paidAmount: data.totalAmount,
      notes: ''
    });
    this.showPaymentModal.set(true);
  }

  closePaymentModal(): void {
    if (!this.isPaying()) {
      this.showPaymentModal.set(false);
    }
  }

  confirmPayment(): void {
    const id = this.invoice()?.id;
    this.paymentForm.markAllAsTouched();
    if (!id || this.paymentForm.invalid || this.isPaying()) {
      return;
    }
    const raw = this.paymentForm.getRawValue();
    this.isPaying.set(true);
    this.invoiceService
      .confirmPayment(id, {
        paymentDate: raw.paymentDate!,
        paymentReference: raw.paymentReference!.trim(),
        paidAmount: raw.paidAmount ? String(raw.paidAmount).trim() : null,
        notes: raw.notes?.trim() || null
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isPaying.set(false))
      )
      .subscribe({
        next: () => {
          this.showPaymentModal.set(false);
          this.toastService.successKey('finance.invoice.detail.toast.paid');
          this.loadInvoice(id);
        }
      });
  }

  invoiceTotal(invoice: Invoice) {
    return invoiceTotalMoney(invoice);
  }

  invoiceSubtotal(invoice: Invoice) {
    return invoiceSubtotalMoney(invoice);
  }

  invoiceTax(invoice: Invoice) {
    return invoiceTaxMoney(invoice);
  }

  lineUnitAmount(invoice: Invoice, line: InvoiceLineItem) {
    return invoiceLineUnitMoney(invoice, line);
  }

  lineTotalAmount(invoice: Invoice, line: InvoiceLineItem) {
    return invoiceLineTotalMoney(invoice, line);
  }

  amountValue(amount: string | null, currency: string): Money {
    return { amount: amount ?? '0', currency };
  }

  matchTone(status: MatchStatus | null): EpBadgeTone {
    if (!status) {
      return 'neutral';
    }
    return MATCH_TONE[status] ?? 'neutral';
  }

  rowMatchTone(row: MatchRow): EpBadgeTone {
    return row.hasVariance ? 'warning' : 'success';
  }

  rowMatchLabelKey(row: MatchRow): string {
    return row.hasVariance ? 'finance.invoice.match.status.PARTIAL' : 'finance.invoice.match.status.MATCHED';
  }

  formatQtyVariance(): string {
    const variance = this.qtyVarianceNum();
    const sign = variance > 0 ? '+' : '';
    return `${sign}${variance.toFixed(2)}`;
  }

  formatQuantity(value: number | null, unit: string): string {
    if (value === null) {
      return '--';
    }
    const formatted = Number.isInteger(value) ? value.toFixed(0) : value.toFixed(2);
    return unit ? `${formatted} ${unit}` : formatted;
  }

  formatVariance(current: number, baseline: number | null): string {
    if (baseline === null) {
      return '--';
    }
    const variance = current - baseline;
    const sign = variance > 0 ? '+' : '';
    return `${sign}${variance.toFixed(2)}`;
  }

  formatPct(value: number): string {
    return `${(value * 100).toFixed(1)}%`;
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

  approveCommentError(): string | null {
    return this.approveComment.touched && this.approveComment.hasError('maxlength')
      ? 'finance.invoice.detail.validation.maxLength'
      : null;
  }

  disputeReasonError(): string | null {
    if (!this.disputeReason.touched) {
      return null;
    }
    if (this.disputeReason.hasError('required')) {
      return 'finance.invoice.detail.validation.required';
    }
    if (this.disputeReason.hasError('minlength')) {
      return 'finance.invoice.detail.validation.disputeMinLength';
    }
    if (this.disputeReason.hasError('maxlength')) {
      return 'finance.invoice.detail.validation.maxLength';
    }
    return null;
  }

  paymentFieldError(controlName: 'paymentDate' | 'paymentReference' | 'paidAmount' | 'notes'): string | null {
    const control = this.paymentForm.controls[controlName];
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'finance.invoice.detail.validation.required';
    }
    if (control.hasError('min')) {
      return 'finance.invoice.detail.validation.positive';
    }
    if (control.hasError('maxlength')) {
      return 'finance.invoice.detail.validation.maxLength';
    }
    return null;
  }

  private loadInvoice(id: string): void {
    this.isLoading.set(true);
    this.poDetail.set(null);
    this.completedGrs.set([]);
    this.invoiceService
      .getById(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          this.invoice.set(res.data);
          this.loadMatchData(res.data);
        },
        error: () => this.router.navigate(['/finance', 'invoices'])
      });
  }

  private loadMatchData(invoice: Invoice): void {
    this.poDetail.set(null);
    this.completedGrs.set([]);
    if (!invoice.po.id || invoice.status === 'PENDING_MATCH') {
      return;
    }

    this.isLoadingMatchData.set(true);
    forkJoin({
      po: this.poService.getByIdForMatch(invoice.po.id).pipe(catchError(() => of(null))),
      grs: this.grService.listCompletedByPoId(invoice.po.id).pipe(catchError(() => of(null)))
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoadingMatchData.set(false))
      )
      .subscribe(({ po, grs }) => {
        this.poDetail.set(po?.data ?? null);
        this.completedGrs.set(grs?.data ?? []);
      });
  }

  private matchClass(status: MatchStatus | null): string {
    return status ? status.toLowerCase() : 'empty';
  }

  private matchIcon(status: MatchStatus | null): string {
    if (status === 'MATCHED') {
      return 'badge-check';
    }
    if (status === 'MISMATCHED') {
      return 'triangle-alert';
    }
    if (status === 'PARTIAL') {
      return 'circle-alert';
    }
    return 'circle-help';
  }

  private buildMatchRow(
    invoice: Invoice,
    invoiceLine: InvoiceLineItem,
    poLine: PoLineItem | undefined,
    gr: GrQuantitySummary | null
  ): MatchRow {
    const invoiceQty = this.parseDecimal(invoiceLine.quantity);
    const invoicePrice = this.parseDecimal(invoiceLine.unitPrice);
    const poQty = poLine ? this.parseDecimal(poLine.quantity.amount) : null;
    const poPrice = poLine ? this.parseDecimal(poLine.unitPrice) : null;
    const quantityVariance = poQty === null ? null : invoiceQty - poQty;
    const priceVariancePct = poPrice && poPrice > 0 ? Math.abs(invoicePrice - poPrice) / poPrice : 0;
    const grReceivedQty = gr?.received ?? null;
    const grRejectedQty = gr?.rejected ?? 0;
    const hasQuantityVariance = quantityVariance !== null && Math.abs(quantityVariance) > 0.001;
    const hasPriceVariance = priceVariancePct > 0.01;
    const hasGrVariance = grRejectedQty > 0 || (grReceivedQty !== null && grReceivedQty + 0.001 < invoiceQty);

    return {
      poLineItemId: invoiceLine.poLineItemId,
      lineNumber: invoiceLine.lineNumber,
      itemName: poLine?.itemName ?? invoiceLine.description,
      categoryCode: poLine?.categoryCode ?? '',
      unit: poLine?.quantity.unit ?? '',
      poQty,
      poUnitPrice: poLine?.unitPrice ?? null,
      grReceivedQty,
      grRejectedQty,
      invoiceQty,
      invoiceUnitPrice: invoiceLine.unitPrice,
      invoiceTotalPrice: invoiceLine.totalPrice,
      priceVariancePct,
      quantityVariance,
      hasVariance: hasQuantityVariance || hasPriceVariance || hasGrVariance
    };
  }

  private parseDecimal(value: string | number | null | undefined): number {
    const parsed = Number.parseFloat(String(value ?? '0'));
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
