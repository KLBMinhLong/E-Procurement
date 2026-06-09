import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { StockService } from '../../services/stock.service';
import { StockMovement } from '../../models/stock.model';
import { PageMeta } from '../../../../core/models/api-response.model';
import { ToastService } from '../../../../core/services/toast.service';

import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';

@Component({
  selector: 'app-stock-movements',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    TranslateModule,
    EpCardComponent,
    EpButtonComponent,
    EpFormFieldComponent,
    EpIconComponent
  ],
  templateUrl: './stock-movements.html',
  styleUrls: ['./stock-movements.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockMovements implements OnInit {
  private readonly stockService = inject(StockService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly movements = signal<StockMovement[]>([]);
  readonly meta = signal<PageMeta | null>(null);

  readonly filterForm: FormGroup = this.fb.group({
    item_code: [''],
    warehouse_id: [''],
    movement_type: ['']
  });

  private currentPage = 0;
  private readonly pageSize = 15;

  ngOnInit(): void {
    this.setupFilters();
    this.loadData();
  }

  private setupFilters(): void {
    this.filterForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.currentPage = 0;
        this.loadData();
      });
  }

  loadData(): void {
    this.loading.set(true);
    const filters = this.filterForm.value;
    
    this.stockService.listStockMovements({
      page: this.currentPage,
      size: this.pageSize,
      item_code: filters.item_code || undefined,
      warehouse_id: filters.warehouse_id || undefined,
      movement_type: filters.movement_type || undefined
    })
    .pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.loading.set(false))
    )
    .subscribe({
      next: (res) => {
        this.movements.set(res.data);
        this.meta.set(res.meta);
      },
      error: (err) => {
        console.error('Failed to load stock movements', err);
        this.toast.error('inventory.stock.movementsLoadFailed');
      }
    });
  }

  resetFilters(): void {
    this.filterForm.reset();
  }

  onPageChange(page: number): void {
    if (page < 0 || (this.meta() && page >= this.meta()!.totalPages)) return;
    this.currentPage = page;
    this.loadData();
  }
}
