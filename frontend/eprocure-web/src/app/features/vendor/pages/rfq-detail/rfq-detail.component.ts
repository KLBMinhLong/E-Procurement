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
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { ToastService } from '../../../../core/services/toast.service';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import {
  formatRfqLineQuantity,
  resolveAwardedVendorName,
  RfqDetail,
  RfqInvitation,
  SubmitQuoteRequest,
  VendorQuote,
  VendorQuoteLineItem
} from '../../models/vendor.model';
import { Money } from '../../../procurement/models/purchase-request.model';
import { RfqService } from '../../services/rfq.service';
import { RfqSubmitQuoteModalComponent } from './rfq-submit-quote-modal.component';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  DRAFT: 'neutral',
  PUBLISHED: 'info',
  AWARDED: 'success',
  CLOSED: 'warning',
  CANCELLED: 'danger'
};

type QuoteViewMode = 'cards' | 'compare';

@Component({
  selector: 'ep-rfq-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    TranslatePipe,
    EpAmountComponent,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpModalComponent,
    EpSkeletonComponent,
    HasPermissionDirective,
    RfqSubmitQuoteModalComponent
  ],
  templateUrl: './rfq-detail.component.html',
  styleUrl: './rfq-detail.component.scss'
})
export class RfqDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly rfqService = inject(RfqService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rfq = signal<RfqDetail | null>(null);
  readonly isLoading = signal(false);
  readonly isActioning = signal(false);
  readonly statusTone = STATUS_TONE;
  readonly quoteViewMode = signal<QuoteViewMode>('compare');

  readonly evaluatingQuote = signal<VendorQuote | null>(null);
  readonly showAwardModal = signal(false);
  readonly showCloseModal = signal(false);
  readonly showSubmitQuoteModal = signal(false);
  readonly awardTargetQuoteId = signal<string | null>(null);
  readonly showEvaluateModal = computed(() => this.evaluatingQuote() !== null);

  evalScore = 0;
  evalNote = '';

  readonly awardReason = new FormControl('', [Validators.required, Validators.minLength(20)]);

  readonly quotes = computed(() => this.rfq()?.quotes ?? []);
  readonly canSubmitQuote = computed(() => this.rfq()?.status === 'PUBLISHED' && this.pendingInvitations().length > 0);
  readonly canCloseRfq = computed(() => this.rfq()?.status === 'PUBLISHED');
  readonly canEvaluateQuotes = computed(() => this.rfq()?.status === 'CLOSED');
  readonly evaluatedCount = computed(() => this.quotes().filter((quote) => quote.evaluationScore !== null).length);
  readonly evalProgressPct = computed(() => {
    const total = this.quotes().length;
    return total ? `${Math.round((this.evaluatedCount() / total) * 100)}%` : '0%';
  });
  readonly lowestTotalQuoteId = computed(() => {
    const quotes = this.quotes();
    if (!quotes.length) {
      return null;
    }
    return quotes.reduce((min, quote) =>
      this.parseDecimal(quote.totalAmount) < this.parseDecimal(min.totalAmount) ? quote : min
    ).id;
  });
  readonly bestScoreQuoteId = computed(() => {
    const quotes = this.quotes().filter((quote) => quote.evaluationScore !== null);
    if (!quotes.length) {
      return null;
    }
    return quotes.reduce((max, quote) =>
      (quote.evaluationScore ?? 0) > (max.evaluationScore ?? 0) ? quote : max
    ).id;
  });
  readonly fastestDeliveryQuoteId = computed(() => {
    const quotes = this.quotes().filter((quote) => this.avgDeliveryDays(quote) !== null);
    if (!quotes.length) {
      return null;
    }
    return quotes.reduce((min, quote) =>
      (this.avgDeliveryDays(quote) ?? Number.POSITIVE_INFINITY) < (this.avgDeliveryDays(min) ?? Number.POSITIVE_INFINITY)
        ? quote
        : min
    ).id;
  });
  readonly lineLowestQuoteIds = computed(() => {
    const data = this.rfq();
    const result = new Map<string, Set<string>>();
    if (!data) {
      return result;
    }

    for (const item of data.lineItems) {
      const prices = this.quotes()
        .map((quote) => ({
          quoteId: quote.id,
          price: this.parseDecimal(this.getLinePrice(quote, item.id)?.unitPrice)
        }))
        .filter((entry) => Number.isFinite(entry.price) && entry.price > 0);
      if (!prices.length) {
        continue;
      }
      const lowest = Math.min(...prices.map((entry) => entry.price));
      result.set(
        item.id,
        new Set(prices.filter((entry) => entry.price === lowest).map((entry) => entry.quoteId))
      );
    }
    return result;
  });
  readonly awardedVendorName = computed(() => {
    const data = this.rfq();
    return data ? resolveAwardedVendorName(data) : null;
  });

  readonly pendingInvitations = computed(() => {
    const data = this.rfq();
    if (!data) return [] as RfqInvitation[];
    return data.invitations.filter((inv) => !inv.hasSubmitted);
  });

  setQuoteViewMode(mode: QuoteViewMode): void {
    this.quoteViewMode.set(mode);
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadRfq(id);
  }

  loadRfq(id: string): void {
    this.isLoading.set(true);
    this.rfqService.getById(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => this.rfq.set(res.data),
        error: () => this.router.navigate(['/vendors/rfq'])
      });
  }

  startEvaluation(quote: VendorQuote): void {
    this.evaluatingQuote.set(quote);
    this.evalScore = quote.evaluationScore ?? 0;
    this.evalNote = quote.evaluationNote ?? '';
  }

  closeEvaluateModal(): void {
    if (!this.isActioning()) {
      this.evaluatingQuote.set(null);
    }
  }

  saveEvaluationFromModal(): void {
    const rfqId = this.rfq()?.id;
    const quoteId = this.evaluatingQuote()?.id;
    if (!rfqId || !quoteId) return;

    this.isActioning.set(true);
    this.rfqService.evaluateQuote(rfqId, quoteId, {
      evaluationScore: this.normalizeEvaluationScore(this.evalScore),
      evaluationNote: this.evalNote.trim() || null
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isActioning.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.success('rfq.detail.toast.evaluated');
          this.evaluatingQuote.set(null);
          this.loadRfq(rfqId);
        }
      });
  }

  openAwardModal(quoteId: string): void {
    this.awardTargetQuoteId.set(quoteId);
    this.awardReason.reset('');
    this.showAwardModal.set(true);
  }

  closeAwardModal(): void {
    this.showAwardModal.set(false);
    this.awardTargetQuoteId.set(null);
  }

  confirmAward(): void {
    const rfqId = this.rfq()?.id;
    const quoteId = this.awardTargetQuoteId();
    if (!rfqId || !quoteId) return;

    this.awardReason.markAsTouched();
    if (this.awardReason.invalid) return;

    this.isActioning.set(true);
    this.rfqService.award(rfqId, {
      awardedQuoteId: quoteId,
      awardReason: this.awardReason.value!.trim()
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isActioning.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.success('rfq.detail.toast.awarded');
          this.closeAwardModal();
          this.loadRfq(rfqId);
        }
      });
  }

  openCloseModal(): void {
    this.showCloseModal.set(true);
  }

  closeCloseModal(): void {
    this.showCloseModal.set(false);
  }

  confirmClose(): void {
    const rfqId = this.rfq()?.id;
    if (!rfqId) return;

    this.isActioning.set(true);
    this.rfqService.close(rfqId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isActioning.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.success('rfq.detail.toast.closed');
          this.closeCloseModal();
          this.loadRfq(rfqId);
        }
      });
  }

  openSubmitQuoteModal(): void {
    if (!this.rfq() || !this.pendingInvitations().length) {
      return;
    }
    this.showSubmitQuoteModal.set(true);
  }

  closeSubmitQuoteModal(): void {
    if (!this.isActioning()) {
      this.showSubmitQuoteModal.set(false);
    }
  }

  submitQuote(request: SubmitQuoteRequest): void {
    const rfqId = this.rfq()?.id;
    if (!rfqId) return;

    this.isActioning.set(true);
    this.rfqService.submitQuote(rfqId, request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isActioning.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.success('rfq.detail.toast.quoteSubmitted');
          this.closeSubmitQuoteModal();
          this.loadRfq(rfqId);
        }
      });
  }

  navigateBack(): void {
    this.router.navigate(['/vendors/rfq']);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(iso));
  }

  formatLineQuantity = formatRfqLineQuantity;

  quoteAmount(quote: VendorQuote): Money {
    return { amount: quote.totalAmount, currency: quote.currency };
  }

  quoteLineUnitAmount(line: VendorQuoteLineItem): Money {
    return { amount: line.unitPrice, currency: line.currency };
  }

  quoteLineTotalAmount(line: VendorQuoteLineItem): Money {
    return { amount: line.totalPrice, currency: line.currency };
  }

  quoteCoverage(quote: VendorQuote): number {
    const totalLines = this.rfq()?.lineItems.length ?? 0;
    if (!totalLines) {
      return 100;
    }
    const quoted = new Set(quote.lineItems.map((line) => line.rfqLineItemId)).size;
    return Math.round((quoted / totalLines) * 100);
  }

  quoteCoverageLabel(quote: VendorQuote): string {
    return `${this.quoteCoverage(quote)}%`;
  }

  avgDeliveryDays(quote: VendorQuote): number | null {
    const days = quote.lineItems
      .map((line) => line.deliveryDays)
      .filter((value): value is number => value !== null && Number.isFinite(value));
    if (!days.length) {
      return null;
    }
    return Math.round(days.reduce((sum, value) => sum + value, 0) / days.length);
  }

  formatAverageDeliveryDays(quote: VendorQuote): string {
    const value = this.avgDeliveryDays(quote);
    return value === null ? '--' : String(value);
  }

  getLinePrice(quote: VendorQuote, rfqLineItemId: string): VendorQuoteLineItem | null {
    return quote.lineItems.find((line) => line.rfqLineItemId === rfqLineItemId) ?? null;
  }

  isLowestForLine(quoteId: string, rfqLineItemId: string): boolean {
    return this.lineLowestQuoteIds().get(rfqLineItemId)?.has(quoteId) ?? false;
  }

  private parseDecimal(value: string | number | null | undefined): number {
    const parsed = Number.parseFloat(String(value ?? '0'));
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private normalizeEvaluationScore(value: number): number {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
      return 0;
    }
    return Math.min(100, Math.max(0, Math.round(parsed)));
  }

  invitationTone(inv: RfqInvitation): EpBadgeTone {
    return inv.hasSubmitted ? 'success' : 'warning';
  }

  invitationLabelKey(inv: RfqInvitation): string {
    return inv.hasSubmitted ? 'rfq.invitation.submitted' : 'rfq.invitation.pending';
  }
}
