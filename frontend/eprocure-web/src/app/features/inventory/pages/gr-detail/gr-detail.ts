import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { GoodsReceiptService } from '../../services/goods-receipt.service';
import { GoodsReceiptDetail } from '../../models/goods-receipt.model';
import { CompleteGrResponse } from '../../models/stock.model';
import { ToastService } from '../../../../core/services/toast.service';
import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';

@Component({
  selector: 'app-gr-detail',
  standalone: true,
  imports: [
    TranslatePipe,
    EpCardComponent,
    EpButtonComponent,
    EpBadgeComponent,
    EpIconComponent,
    EpBreadcrumbComponent,
    EpSkeletonComponent,
    EpModalComponent,
    HasPermissionDirective
  ],
  templateUrl: './gr-detail.html',
  styleUrls: ['./gr-detail.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GrDetail implements OnInit {
  private readonly grService = inject(GoodsReceiptService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly showCompleteModal = signal(false);
  readonly gr = signal<GoodsReceiptDetail | null>(null);
  readonly completeSummary = signal<CompleteGrResponse | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadGoodsReceipt(id);
    } else {
      this.toast.error('inventory.gr.detail.toast.notFound');
      this.router.navigate(['/inventory/goods-receipts']);
    }
  }

  private loadGoodsReceipt(id: string): void {
    this.loading.set(true);
    this.grService.getById(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (res) => this.gr.set(res.data),
        error: () => {
          this.toast.error('inventory.gr.detail.toast.loadFailed');
          this.router.navigate(['/inventory/goods-receipts']);
        }
      });
  }

  openCompleteModal(): void {
    this.showCompleteModal.set(true);
  }

  closeCompleteModal(): void {
    this.showCompleteModal.set(false);
  }

  confirmComplete(): void {
    const currentGr = this.gr();
    if (!currentGr) return;

    this.submitting.set(true);
    this.grService.complete(currentGr.id, crypto.randomUUID())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: (res) => {
          this.completeSummary.set(res.data ?? null);
          this.toast.success('inventory.gr.detail.toast.completeSuccess');
          this.showCompleteModal.set(false);
          this.loadGoodsReceipt(currentGr.id);
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/inventory/goods-receipts']);
  }

  navigateStock(): void {
    const currentGr = this.gr();
    this.router.navigate(['/inventory/stock'], {
      queryParams: { warehouse_id: currentGr?.warehouse.id || undefined }
    });
  }

  navigateMovements(itemCode?: string | null): void {
    const currentGr = this.gr();
    this.router.navigate(['/inventory/stock/movements'], {
      queryParams: {
        warehouse_id: currentGr?.warehouse.id || undefined,
        item_code: itemCode || undefined
      }
    });
  }

  canComplete(): boolean {
    return this.gr()?.status === 'DRAFT';
  }

  statusTone(status?: string): 'neutral' | 'success' | 'warning' | 'danger' | 'info' {
    switch (status) {
      case 'COMPLETE': return 'success';
      case 'PARTIAL': return 'warning';
      case 'DRAFT': return 'neutral';
      case 'DISCREPANCY': return 'danger';
      default: return 'neutral';
    }
  }

  shortId(id?: string): string {
    if (!id) return '';
    return id.length <= 12 ? id : `${id.slice(0, 8)}...${id.slice(-4)}`;
  }

  formatDateTime(isoString?: string): string {
    if (!isoString) return '--';
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(isoString));
  }

  formatQuantity(value?: string | null): string {
    if (!value) return '0';
    return Number(value).toLocaleString('vi-VN', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 4
    });
  }
}
