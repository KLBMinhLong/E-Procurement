import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { PageMeta } from '../../../../core/models/api-response.model';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpPageChangeEvent } from '../../../../shared/shared.index';
import {
  Invoice,
  InvoiceListFilter,
  InvoiceStatus,
  MatchStatus,
  invoiceTotalMoney
} from '../../models/invoice.model';
import { InvoiceService } from '../../services/invoice.service';

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

@Component({
  selector: 'ep-invoice-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpAmountComponent,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFilterBarComponent,
    EpIconComponent,
    EpSkeletonComponent,
    HasPermissionDirective
  ],
  templateUrl: './invoice-list.component.html',
  styleUrl: './invoice-list.component.scss'
})
export class InvoiceListComponent implements OnInit {
  private readonly invoiceService = inject(InvoiceService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly items = signal<Invoice[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly vendorId = signal('');
  readonly poId = signal('');
  readonly activeStatus = signal<InvoiceStatus | ''>('');
  readonly overdueOnly = signal(false);
  readonly page = signal(1);
  readonly size = signal(20);

  readonly statusTone = STATUS_TONE;
  readonly matchTone = MATCH_TONE;
  readonly statuses: InvoiceStatus[] = [
    'PENDING_MATCH',
    'MATCHED',
    'MISMATCHED',
    'APPROVED',
    'DISPUTED',
    'PAID',
    'CANCELLED'
  ];

  readonly filter = computed<InvoiceListFilter>(() => ({
    page: this.page(),
    size: this.size(),
    status: (this.activeStatus() as InvoiceStatus) || undefined,
    vendor_id: this.vendorId().trim() || undefined,
    po_id: this.poId().trim() || undefined,
    overdue_only: this.overdueOnly() || undefined
  }));

  readonly hasActiveFilters = computed(() =>
    Boolean(this.activeStatus() || this.vendorId().trim() || this.poId().trim() || this.overdueOnly())
  );
  readonly pendingCount = computed(() => this.items().filter((item) => item.status === 'PENDING_MATCH').length);
  readonly mismatchCount = computed(() => this.items().filter((item) => item.status === 'MISMATCHED').length);
  readonly approvedCount = computed(() => this.items().filter((item) => item.status === 'APPROVED').length);
  readonly visibleTotalAmount = computed(() =>
    this.items().reduce((sum, item) => sum + Number(item.totalAmount ?? 0), 0).toFixed(4)
  );

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.invoiceService
      .list(this.filter())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          this.items.set(res.data ?? []);
          this.meta.set(res.meta ?? null);
        },
        error: () => {
          this.items.set([]);
          this.meta.set(null);
        }
      });
  }

  onStatusChange(status: string): void {
    this.activeStatus.set(status as InvoiceStatus | '');
    this.resetPageAndLoad();
  }

  onVendorSearch(value: string): void {
    this.vendorId.set(value);
    this.resetPageAndLoad();
  }

  onPoSearch(value: string): void {
    this.poId.set(value);
    this.resetPageAndLoad();
  }

  onOverdueToggle(checked: boolean): void {
    this.overdueOnly.set(checked);
    this.resetPageAndLoad();
  }

  clearFilters(): void {
    this.activeStatus.set('');
    this.vendorId.set('');
    this.poId.set('');
    this.overdueOnly.set(false);
    this.resetPageAndLoad();
  }

  onPageChange(event: EpPageChangeEvent): void {
    this.page.set(event.page);
    this.size.set(event.size);
    this.loadData();
  }

  navigateCreate(): void {
    this.router.navigate(['/finance', 'invoices', 'create']);
  }

  navigateDetail(invoiceId: string): void {
    this.router.navigate(['/finance', 'invoices', invoiceId]);
  }

  amount(invoice: Invoice) {
    return invoiceTotalMoney(invoice);
  }

  matchStatus(invoice: Invoice, side: 'po' | 'gr'): MatchStatus | null {
    if (!invoice.matchResult) {
      return null;
    }
    return side === 'po' ? invoice.matchResult.poMatchStatus : invoice.matchResult.grMatchStatus;
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

  private resetPageAndLoad(): void {
    this.page.set(1);
    this.loadData();
  }
}
