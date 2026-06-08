import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  computed
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { ToastService } from '../../../../core/services/toast.service';

import { Rfq, VendorQuote } from '../../models/vendor.model';
import { RfqService } from '../../services/rfq.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  OPEN: 'info',
  EVALUATING: 'warning',
  AWARDED: 'success',
  CLOSED: 'neutral',
  CANCELLED: 'danger'
};

@Component({
  selector: 'ep-rfq-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpIconComponent,
    EpSkeletonComponent,
    HasPermissionDirective
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

  readonly rfq = signal<Rfq | null>(null);
  readonly quotes = signal<VendorQuote[]>([]);
  readonly isLoading = signal(false);
  readonly isLoadingQuotes = signal(false);
  readonly isActioning = signal(false);

  readonly statusTone = STATUS_TONE;

  // Evaluation state
  selectedQuoteId = signal<string | null>(null);
  evalScore = 0;
  evalNote = '';
  isEvaluating = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadRfq(id);
      this.loadQuotes(id);
    }
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

  loadQuotes(id: string): void {
    this.isLoadingQuotes.set(true);
    this.rfqService.getQuotes(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoadingQuotes.set(false))
      )
      .subscribe({
        next: (res) => this.quotes.set(res.data)
      });
  }

  startEvaluation(quote: VendorQuote): void {
    this.selectedQuoteId.set(quote.id);
    this.evalScore = quote.score || 0;
    this.evalNote = quote.evaluationNote || '';
  }

  cancelEvaluation(): void {
    this.selectedQuoteId.set(null);
  }

  saveEvaluation(quoteId: string): void {
    const rfqId = this.rfq()?.id;
    if (!rfqId) return;

    this.isEvaluating.set(true);
    this.rfqService.evaluateQuote(rfqId, quoteId, { score: this.evalScore, note: this.evalNote })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isEvaluating.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.success('rfq.detail.toast.evaluated');
          this.selectedQuoteId.set(null);
          this.loadQuotes(rfqId);
        }
      });
  }

  awardQuote(quoteId: string): void {
    const rfqId = this.rfq()?.id;
    if (!rfqId) return;

    if (!confirm('Are you sure you want to award this RFQ to this quote?')) return;

    this.isActioning.set(true);
    this.rfqService.award(rfqId, { awardedQuoteId: quoteId })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isActioning.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.success('rfq.detail.toast.awarded');
          this.loadRfq(rfqId);
          this.loadQuotes(rfqId);
        }
      });
  }

  navigateBack(): void {
    this.router.navigate(['/vendors/rfq']);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  formatMoney(amount: { amount: string; currency: string } | null | undefined): string {
    if (!amount) return '--';
    return `${new Intl.NumberFormat('vi-VN').format(Number(amount.amount))} ${amount.currency}`;
  }
}
