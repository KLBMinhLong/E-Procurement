import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
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

@Component({
  selector: 'ep-po-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpAmountComponent,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpIconComponent,
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

  readonly po = signal<PurchaseOrder | null>(null);
  readonly isLoading = signal(true);
  readonly statusTone = computed(() => STATUS_TONE[this.po()?.status ?? ''] ?? 'neutral');
  readonly callbackTone = computed(() => CALLBACK_TONE[this.po()?.prConversionStatus ?? ''] ?? 'neutral');
  readonly totalLines = computed(() => this.po()?.lineItems.length ?? 0);

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
