import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFilterBarComponent, EpFilterField } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpPaginationComponent } from '../../../../shared/components/ep-pagination/ep-pagination.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { PageMeta } from '../../../../core/models/api-response.model';
import { ToastService } from '../../../../core/services/toast.service';

import { GoodsReceiptDetail, GoodsReceiptStatus } from '../../models/goods-receipt.model';
import { GoodsReceiptService, GoodsReceiptListFilter } from '../../services/goods-receipt.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  DRAFT: 'neutral',
  PARTIAL: 'warning',
  COMPLETE: 'success',
  DISCREPANCY: 'danger'
};

@Component({
  selector: 'ep-gr-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpFilterBarComponent,
    EpIconComponent,
    EpPaginationComponent,
    EpSkeletonComponent,
    HasPermissionDirective
  ],
  templateUrl: './gr-list.html',
  styleUrl: './gr-list.scss'
})
export class GrList implements OnInit {
  private readonly router = inject(Router);
  private readonly grService = inject(GoodsReceiptService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastService = inject(ToastService);

  readonly grs = signal<GoodsReceiptDetail[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);

  readonly statusTone = STATUS_TONE;

  readonly filterFields: EpFilterField[] = [
    {
      key: 'po_id',
      label: 'inventory.gr.list.filterPo',
      type: 'text',
      placeholder: 'inventory.gr.list.filterPoPlaceholder'
    },
    {
      key: 'status',
      label: 'inventory.gr.list.filterStatus',
      type: 'select',
      options: [
        { label: 'shared.all', value: '' },
        { label: 'inventory.gr.status.DRAFT', value: 'DRAFT' },
        { label: 'inventory.gr.status.PARTIAL', value: 'PARTIAL' },
        { label: 'inventory.gr.status.COMPLETE', value: 'COMPLETE' },
        { label: 'inventory.gr.status.DISCREPANCY', value: 'DISCREPANCY' }
      ]
    }
  ];

  readonly state = signal<GoodsReceiptListFilter>({
    page: 1,
    size: 20,
    status: undefined,
    po_id: undefined
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.grService.list(this.state())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          this.grs.set(res.data);
          this.meta.set(res.meta || null);
        },
        error: () => {
          this.toastService.error('error.generic');
        }
      });
  }

  onFilterChange(filters: Record<string, string>): void {
    this.state.update(s => ({
      ...s,
      page: 1,
      po_id: filters['po_id'] || undefined,
      status: filters['status'] || undefined
    }));
    this.loadData();
  }

  onPageChange(page: number): void {
    this.state.update(s => ({ ...s, page }));
    this.loadData();
  }

  navigateToDetail(id: string): void {
    this.router.navigate(['/inventory/goods-receipts', id]);
  }

  navigateToCreate(): void {
    this.router.navigate(['/inventory/goods-receipts/create']);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(iso));
  }
}
