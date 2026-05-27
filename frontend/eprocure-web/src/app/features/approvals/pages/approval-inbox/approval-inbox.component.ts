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

import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpStatCardComponent } from '../../../../shared/components/ep-stat-card/ep-stat-card.component';
import { EpSlaBarComponent } from '../../../../shared/components/ep-sla-bar/ep-sla-bar.component';
import { PageMeta } from '../../../../core/models/api-response.model';
import { ApprovalsService } from '../../services/approvals.service';
import {
  ApprovalInboxFilter,
  ApprovalInboxCount,
  ApprovalTaskSummary
} from '../../models/approvals.model';

const PRIORITY_TONE: Record<string, EpBadgeTone> = {
  NORMAL: 'neutral',
  URGENT: 'warning',
  EMERGENCY: 'danger'
};

@Component({
  selector: 'ep-approval-inbox',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpAmountComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpButtonComponent,
    EpFilterBarComponent,
    EpBreadcrumbComponent,
    EpStatCardComponent,
    EpSlaBarComponent
  ],
  templateUrl: './approval-inbox.component.html',
  styleUrl: './approval-inbox.component.scss'
})
export class ApprovalInboxComponent implements OnInit {
  private readonly approvalsService = inject(ApprovalsService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // ── State ──────────────────────────────────────────────────────────
  readonly items = signal<ApprovalTaskSummary[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly searchQuery = signal('');
  readonly activePriority = signal<string>('');
  readonly activeEntityType = signal<string>('');
  readonly overdueOnly = signal<boolean>(false);
  readonly counts = signal<ApprovalInboxCount>({ total: 0, overdue: 0, emergency: 0 });

  readonly filter = computed<ApprovalInboxFilter>(() => ({
    page: 1,
    size: 20,
    sort: 'assignedAt,desc',
    priority: (this.activePriority() || undefined) as 'NORMAL' | 'URGENT' | 'EMERGENCY' | undefined,
    entity_type: this.activeEntityType() || undefined,
    is_overdue: this.overdueOnly() ? true : undefined
  }));

  readonly priorityTone = PRIORITY_TONE;
  readonly priorities = ['NORMAL', 'URGENT', 'EMERGENCY'];
  readonly entityTypes = ['PURCHASE_REQUEST'];

  // ── Lifecycle ─────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadCounts();
    this.loadData();
  }

  // ── Event handlers ────────────────────────────────────────────────
  onSearchChange(q: string): void {
    this.searchQuery.set(q);
    this.loadData();
  }

  onPriorityChange(priority: string): void {
    this.activePriority.set(priority);
    this.loadData();
  }

  onEntityTypeChange(type: string): void {
    this.activeEntityType.set(type);
    this.loadData();
  }

  onOverdueToggle(): void {
    this.overdueOnly.update((prev) => !prev);
    this.loadData();
  }

  onPageChange(event: { page: number; size: number }): void {
    this.loadData({ page: event.page, size: event.size });
  }

  onRowClick(row: Record<string, unknown>): void {
    this.router.navigate(['/approvals', row['id']]);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // ── Private ───────────────────────────────────────────────────────
  loadCounts(): void {
    this.approvalsService
      .getInboxCount()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          if (res.data) {
            this.counts.set(res.data);
          }
        }
      });
  }

  loadData(overrides: Partial<ApprovalInboxFilter> = {}): void {
    this.isLoading.set(true);
    const f: ApprovalInboxFilter = { ...this.filter(), ...overrides };

    this.approvalsService
      .getInbox(f)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          let list = res.data ?? [];
          if (this.searchQuery()) {
            const query = this.searchQuery().toLowerCase();
            list = list.filter(
              (item) =>
                item.entityNumber?.toLowerCase().includes(query) ||
                item.entityTitle?.toLowerCase().includes(query)
            );
          }
          this.items.set(list);
          this.meta.set(res.meta ?? null);
        },
        error: () => {
          this.items.set([]);
        }
      });
  }
}
