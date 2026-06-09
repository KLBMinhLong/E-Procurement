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
import {
  formatQuoteTotal,
  formatRfqLineQuantity,
  resolveAwardedVendorName,
  RfqDetail,
  RfqInvitation,
  VendorQuote
} from '../../models/vendor.model';
import { RfqService } from '../../services/rfq.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  OPEN: 'info',
  EVALUATING: 'warning',
  AWARDED: 'success',
  CLOSED: 'neutral',
  CANCELLED: 'danger'
};

interface SubmitLineDraft {
  rfqLineItemId: string;
  itemName: string;
  unitPrice: string;
  deliveryDays: string;
}

@Component({
  selector: 'ep-rfq-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpModalComponent,
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

  readonly rfq = signal<RfqDetail | null>(null);
  readonly isLoading = signal(false);
  readonly isActioning = signal(false);
  readonly statusTone = STATUS_TONE;

  readonly selectedQuoteId = signal<string | null>(null);
  readonly showAwardModal = signal(false);
  readonly showCloseModal = signal(false);
  readonly showSubmitQuoteModal = signal(false);
  readonly awardTargetQuoteId = signal<string | null>(null);

  evalScore = 0;
  evalNote = '';

  readonly awardReason = new FormControl('', [Validators.required, Validators.minLength(20)]);

  submitVendorId = '';
  submitCurrency = 'VND';
  submitValidUntil = '';
  submitPaymentTerms = '';
  submitNotes = '';
  submitLineDrafts: SubmitLineDraft[] = [];

  readonly quotes = computed(() => this.rfq()?.quotes ?? []);
  readonly awardedVendorName = computed(() => {
    const data = this.rfq();
    return data ? resolveAwardedVendorName(data) : null;
  });

  readonly pendingInvitations = computed(() => {
    const data = this.rfq();
    if (!data) return [] as RfqInvitation[];
    return data.invitations.filter((inv) => !inv.hasSubmitted);
  });

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
    this.selectedQuoteId.set(quote.id);
    this.evalScore = quote.evaluationScore ?? 0;
    this.evalNote = quote.evaluationNote ?? '';
  }

  cancelEvaluation(): void {
    this.selectedQuoteId.set(null);
  }

  saveEvaluation(quoteId: string): void {
    const rfqId = this.rfq()?.id;
    if (!rfqId) return;

    this.isActioning.set(true);
    this.rfqService.evaluateQuote(rfqId, quoteId, {
      evaluationScore: this.evalScore,
      evaluationNote: this.evalNote.trim() || null
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isActioning.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.success('rfq.detail.toast.evaluated');
          this.selectedQuoteId.set(null);
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
    const data = this.rfq();
    if (!data) return;

    const firstPending = this.pendingInvitations()[0];
    this.submitVendorId = firstPending?.vendor.id ?? '';
    this.submitCurrency = 'VND';
    this.submitValidUntil = '';
    this.submitPaymentTerms = '';
    this.submitNotes = '';
    this.submitLineDrafts = data.lineItems.map((item) => ({
      rfqLineItemId: item.id,
      itemName: item.itemName,
      unitPrice: '',
      deliveryDays: ''
    }));
    this.showSubmitQuoteModal.set(true);
  }

  closeSubmitQuoteModal(): void {
    this.showSubmitQuoteModal.set(false);
  }

  confirmSubmitQuote(): void {
    const rfqId = this.rfq()?.id;
    if (!rfqId || !this.submitVendorId || !this.submitValidUntil) {
      this.toastService.error('rfq.detail.toast.submitValidation');
      return;
    }

    const lineItems = this.submitLineDrafts
      .filter((line) => line.unitPrice.trim())
      .map((line) => ({
        rfqLineItemId: line.rfqLineItemId,
        unitPrice: line.unitPrice.trim(),
        deliveryDays: line.deliveryDays ? Number(line.deliveryDays) : null
      }));

    if (!lineItems.length) {
      this.toastService.error('rfq.detail.toast.submitValidation');
      return;
    }

    this.isActioning.set(true);
    this.rfqService.submitQuote(rfqId, {
      vendorId: this.submitVendorId,
      currency: this.submitCurrency,
      validUntil: this.submitValidUntil,
      lineItems,
      paymentTerms: this.submitPaymentTerms.trim() || null,
      notes: this.submitNotes.trim() || null
    })
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

  formatQuoteTotal = formatQuoteTotal;
  formatLineQuantity = formatRfqLineQuantity;

  invitationTone(inv: RfqInvitation): EpBadgeTone {
    return inv.hasSubmitted ? 'success' : 'warning';
  }

  invitationLabelKey(inv: RfqInvitation): string {
    return inv.hasSubmitted ? 'rfq.invitation.submitted' : 'rfq.invitation.pending';
  }
}
