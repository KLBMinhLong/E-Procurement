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
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpPageChangeEvent } from '../../../../shared/shared.index';
import { VendorListFilter, VendorStatus, VendorSummary } from '../../models/vendor.model';
import { VendorService } from '../../services/vendor.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  PENDING: 'warning',
  APPROVED: 'success',
  BLACKLISTED: 'danger',
  INACTIVE: 'neutral'
};

type SortKey = 'name' | 'vendorCode' | 'status' | 'overallScore';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'ep-vendor-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFilterBarComponent,
    EpIconComponent,
    EpSkeletonComponent,
    HasPermissionDirective
  ],
  templateUrl: './vendor-list.component.html',
  styleUrl: './vendor-list.component.scss'
})
export class VendorListComponent implements OnInit {
  private readonly vendorService = inject(VendorService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly items = signal<VendorSummary[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly searchQuery = signal('');
  readonly activeStatus = signal<VendorStatus | ''>('');
  readonly avlOnly = signal(false);
  readonly page = signal(1);
  readonly size = signal(20);
  readonly sortKey = signal<SortKey>('name');
  readonly sortDirection = signal<SortDirection>('asc');

  readonly statusTone = STATUS_TONE;
  readonly statuses: VendorStatus[] = ['PENDING', 'APPROVED', 'BLACKLISTED', 'INACTIVE'];

  readonly filter = computed<VendorListFilter>(() => ({
    page: this.page(),
    size: this.size(),
    sort: `${this.sortKey()},${this.sortDirection()}`,
    status: (this.activeStatus() as VendorStatus) || undefined,
    q: this.searchQuery().trim() || undefined,
    onAvlOnly: this.avlOnly() || undefined
  }));

  readonly hasActiveFilters = computed(() =>
    Boolean(this.activeStatus() || this.searchQuery().trim() || this.avlOnly())
  );

  readonly approvedCount = computed(() => this.items().filter((v) => v.status === 'APPROVED').length);
  readonly pendingCount = computed(() => this.items().filter((v) => v.status === 'PENDING').length);
  readonly avlCount = computed(() => this.items().filter((v) => v.isOnApprovedVendorList).length);
  readonly blacklistedCount = computed(() => this.items().filter((v) => v.status === 'BLACKLISTED').length);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.vendorService
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
    this.activeStatus.set(status as VendorStatus | '');
    this.resetPageAndLoad();
  }

  onAvlToggle(): void {
    this.avlOnly.update((v) => !v);
    this.resetPageAndLoad();
  }

  onSearch(value: string): void {
    this.searchQuery.set(value);
    this.resetPageAndLoad();
  }

  clearFilters(): void {
    this.activeStatus.set('');
    this.searchQuery.set('');
    this.avlOnly.set(false);
    this.resetPageAndLoad();
  }

  onSort(key: SortKey): void {
    if (this.sortKey() === key) {
      this.sortDirection.update((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      this.sortKey.set(key);
      this.sortDirection.set(key === 'overallScore' ? 'desc' : 'asc');
    }
    this.resetPageAndLoad();
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
    this.router.navigate(['/vendors', 'create']);
  }

  navigateDetail(vendorId: string): void {
    this.router.navigate(['/vendors', vendorId]);
  }

  shortId(value: string | null | undefined): string {
    if (!value) return '--';
    return value.length <= 12 ? value : `${value.slice(0, 8)}...${value.slice(-4)}`;
  }

  vendorScore(vendor: VendorSummary): string {
    if (vendor.overallScore == null) return '--';
    return vendor.overallScore.toFixed(1);
  }

  private resetPageAndLoad(): void {
    this.page.set(1);
    this.loadData();
  }
}
