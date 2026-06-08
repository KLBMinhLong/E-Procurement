import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  computed
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

import { Rfq, RfqListFilter, RfqStatus } from '../../models/vendor.model';
import { RfqService } from '../../services/rfq.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  OPEN: 'info',
  EVALUATING: 'warning',
  AWARDED: 'success',
  CLOSED: 'neutral',
  CANCELLED: 'danger'
};

@Component({
  selector: 'ep-rfq-list',
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
  templateUrl: './rfq-list.component.html',
  styleUrl: './rfq-list.component.scss'
})
export class RfqListComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly rfqService = inject(RfqService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastService = inject(ToastService);

  readonly rfqs = signal<Rfq[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);

  readonly statusTone = STATUS_TONE;

  readonly filterFields: EpFilterField[] = [
    {
      key: 'q',
      label: 'shared.search',
      type: 'text',
      placeholder: 'rfq.list.searchPlaceholder'
    },
    {
      key: 'status',
      label: 'rfq.list.filterStatus',
      type: 'select',
      options: [
        { label: 'shared.all', value: '' },
        { label: 'rfq.status.OPEN', value: 'OPEN' },
        { label: 'rfq.status.EVALUATING', value: 'EVALUATING' },
        { label: 'rfq.status.AWARDED', value: 'AWARDED' },
        { label: 'rfq.status.CLOSED', value: 'CLOSED' },
        { label: 'rfq.status.CANCELLED', value: 'CANCELLED' }
      ]
    }
  ];

  readonly state = signal<RfqListFilter>({
    page: 0,
    size: 20,
    sort: 'createdAt,desc',
    q: '',
    status: undefined
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.rfqService.list(this.state())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          this.rfqs.set(res.data);
          this.meta.set(res.meta || null);
        },
        error: () => {
          this.toastService.error('shared.error.loadFailed');
        }
      });
  }

  onFilterChange(filters: Record<string, string>): void {
    this.state.update(s => ({
      ...s,
      page: 0,
      q: filters['q'] || '',
      status: (filters['status'] as RfqStatus) || undefined
    }));
    this.loadData();
  }

  onPageChange(page: number): void {
    this.state.update(s => ({ ...s, page }));
    this.loadData();
  }

  onSort(field: string): void {
    this.state.update(s => {
      const currentField = s.sort.split(',')[0];
      const currentDir = s.sort.split(',')[1] || 'asc';
      let newDir = 'asc';
      if (currentField === field && currentDir === 'asc') {
        newDir = 'desc';
      }
      return { ...s, sort: `${field},${newDir}` };
    });
    this.loadData();
  }

  navigateToDetail(rfqId: string): void {
    this.router.navigate(['/vendors/rfq', rfqId]);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(iso));
  }

  formatMoney(amount: { amount: string; currency: string } | null | undefined): string {
    if (!amount) return '--';
    return `${new Intl.NumberFormat('vi-VN').format(Number(amount.amount))} ${amount.currency}`;
  }

  getSortIcon(field: string): string {
    const currentSort = this.state().sort;
    if (currentSort === `${field},asc`) return 'chevron-up';
    if (currentSort === `${field},desc`) return 'chevron-down';
    return 'chevrons-up-down';
  }
}
