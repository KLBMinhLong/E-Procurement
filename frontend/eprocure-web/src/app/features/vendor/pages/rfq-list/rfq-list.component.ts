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
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpPageChangeEvent } from '../../../../shared/shared.index';
import { resolveAwardedVendorName, RfqDetail, RfqListFilter, RfqStatus } from '../../models/vendor.model';
import { RfqService } from '../../services/rfq.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  OPEN: 'info',
  EVALUATING: 'warning',
  AWARDED: 'success',
  CLOSED: 'neutral',
  CANCELLED: 'danger'
};

type SortKey = 'rfqNumber' | 'status' | 'submissionDeadline' | 'createdAt';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'ep-rfq-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpIconComponent,
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

  readonly items = signal<RfqDetail[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly activeStatus = signal<RfqStatus | ''>('');
  readonly page = signal(1);
  readonly size = signal(20);
  readonly sortKey = signal<SortKey>('createdAt');
  readonly sortDirection = signal<SortDirection>('desc');

  readonly statusTone = STATUS_TONE;
  readonly statuses: RfqStatus[] = ['OPEN', 'EVALUATING', 'AWARDED', 'CLOSED', 'CANCELLED'];

  readonly filter = computed<RfqListFilter>(() => ({
    page: this.page(),
    size: this.size(),
    sort: `${this.sortKey()},${this.sortDirection()}`,
    status: (this.activeStatus() as RfqStatus) || undefined
  }));

  readonly openCount = computed(() => this.items().filter((r) => r.status === 'OPEN').length);
  readonly evaluatingCount = computed(() => this.items().filter((r) => r.status === 'EVALUATING').length);
  readonly awardedCount = computed(() => this.items().filter((r) => r.status === 'AWARDED').length);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.rfqService
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
    this.activeStatus.set(status as RfqStatus | '');
    this.page.set(1);
    this.loadData();
  }

  onSort(key: SortKey): void {
    if (this.sortKey() === key) {
      this.sortDirection.update((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      this.sortKey.set(key);
      this.sortDirection.set(key === 'createdAt' || key === 'submissionDeadline' ? 'desc' : 'asc');
    }
    this.page.set(1);
    this.loadData();
  }

  sortIcon(key: SortKey): string {
    if (this.sortKey() !== key) return 'chevron-down';
    return this.sortDirection() === 'asc' ? 'chevron-up' : 'chevron-down';
  }

  onPageChange(event: EpPageChangeEvent): void {
    this.page.set(event.page);
    this.size.set(event.size);
    this.loadData();
  }

  navigateCreate(): void {
    this.router.navigate(['/vendors/rfq/create']);
  }

  navigateDetail(rfqId: string): void {
    this.router.navigate(['/vendors/rfq', rfqId]);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(new Date(iso));
  }

  awardedVendorName(rfq: RfqDetail): string {
    return resolveAwardedVendorName(rfq) ?? '--';
  }

  invitationSummary(rfq: RfqDetail): string {
    const total = rfq.invitations.length;
    const quoted = rfq.invitations.filter((inv) => inv.hasSubmitted).length;
    return `${quoted}/${total}`;
  }
}
