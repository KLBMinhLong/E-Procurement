import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { GoodsReceiptService } from '../../services/goods-receipt.service';
import { GoodsReceiptDetail } from '../../models/goods-receipt.model';
import { ToastService } from '../../../../core/services/toast.service';
import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';

@Component({
  selector: 'app-gr-detail',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    EpCardComponent,
    EpButtonComponent,
    EpBadgeComponent,
    EpIconComponent,
    EpBreadcrumbComponent,
    EpSkeletonComponent
  ],
  templateUrl: './gr-detail.html',
  styleUrls: ['./gr-detail.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GrDetail {
  private readonly grService = inject(GoodsReceiptService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly gr = signal<GoodsReceiptDetail | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadGoodsReceipt(id);
    } else {
      this.toast.error('inventory.gr.detail.notFound');
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
        next: (res) => {
          this.gr.set(res.data);
        },
        error: (err) => {
          console.error('Failed to load GR', err);
          this.toast.error('inventory.gr.detail.loadFailed');
          this.router.navigate(['/inventory/goods-receipts']);
        }
      });
  }

  completeGr(): void {
    const currentGr = this.gr();
    if (!currentGr) return;

    if (!confirm('Are you sure you want to complete this goods receipt? This will trigger inventory movement and update stock balances.')) {
      return;
    }

    this.submitting.set(true);
    const idempotencyKey = crypto.randomUUID();

    this.grService.complete(currentGr.id, idempotencyKey)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: (res) => {
          this.toast.success('inventory.gr.detail.completeSuccess');
          this.loadGoodsReceipt(currentGr.id); // Reload
        },
        error: (err) => {
          console.error('Failed to complete GR', err);
          this.toast.error('inventory.gr.detail.completeFailed');
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/inventory/goods-receipts']);
  }

  statusTone(status?: string): 'neutral' | 'success' | 'warning' | 'danger' | 'info' {
    switch (status) {
      case 'COMPLETE':
        return 'success';
      case 'PARTIAL':
        return 'warning';
      case 'DRAFT':
        return 'neutral';
      case 'DISCREPANCY':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  shortId(id?: string): string {
    if (!id) return '';
    return id.split('-')[0];
  }

  formatDateTime(isoString?: string): string {
    if (!isoString) return '--';
    const date = new Date(isoString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
}
