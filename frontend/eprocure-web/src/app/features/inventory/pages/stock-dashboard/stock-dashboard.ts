import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';

import { StockService } from '../../services/stock.service';
import { CatalogItemDetail, StockMovement } from '../../models/stock.model';

@Component({
  selector: 'app-stock-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './stock-dashboard.html',
  styleUrls: ['./stock-dashboard.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockDashboard {
  private readonly stockService = inject(StockService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly itemsBelowReorder = signal<CatalogItemDetail[]>([]);
  readonly recentMovements = signal<StockMovement[]>([]);

  // Simple metrics
  readonly metrics = signal({
    totalItems: 0,
    criticalItems: 0,
    recentMovementsCount: 0
  });

  ngOnInit(): void {
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.loading.set(true);

    forkJoin({
      catalog: this.stockService.searchCatalogItems({ page: 0, size: 10, below_reorder: true }),
      movements: this.stockService.listStockMovements({ page: 0, size: 5 })
    })
    .pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.loading.set(false))
    )
    .subscribe({
      next: (res) => {
        this.itemsBelowReorder.set(res.catalog.data);
        this.recentMovements.set(res.movements.data);
        
        this.metrics.set({
          totalItems: res.catalog.meta.totalElements || res.catalog.data.length,
          criticalItems: res.catalog.data.length, // Since we filtered by below_reorder
          recentMovementsCount: res.movements.meta.totalElements || res.movements.data.length
        });
      },
      error: (err) => {
        console.error('Failed to load dashboard data', err);
      }
    });
  }
}
