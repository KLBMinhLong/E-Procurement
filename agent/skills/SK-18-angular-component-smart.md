## SK-18 · Angular Component (Smart)

### Trigger
Agent tạo Angular component mới (feature component, page component).

### Inputs Required
- Feature path + route
- Service + models
- Permission codes
- Pagination requirements

### Rules
```
[R1] Không hardcode string tiếng Việt trong template — dùng | translate pipe
[R2] Không hardcode màu trong component — dùng CSS variable từ design system
[R3] Smart Component: inject Service, manage state
[R4] Dumb/Presentational Component: chỉ @Input/@Output, không inject Service
[R5] Dùng OnPush change detection cho performance
[R6] Unsubscribe: dùng takeUntilDestroyed() (Angular 16+) hoặc DestroyRef
[R7] Loading/Error state phải có trong mọi component gọi API
[R8] Pagination: dùng shared PaginationComponent
[R9] Form: ReactiveFormsModule (không dùng Template-driven)
[R10] Không có DELETE button — dùng Cancel / Archive / Deactivate
```

### Template — Smart Component
```typescript
import { Component, OnInit, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { {Entity}Service } from '../services/{entity}.service';
import { {Entity}FilterRequest, {Entity}Response } from '../models/{entity}.model';
import { PaginationComponent } from '@shared/components/pagination/pagination.component';
import { BreadcrumbComponent } from '@shared/components/breadcrumb/breadcrumb.component';

/**
 * Smart Component: {Entity}ListComponent
 * Page: /{feature}/{entities}
 */
@Component({
  selector: 'app-{entity}-list',
  standalone: true,
  imports: [CommonModule, TranslateModule, PaginationComponent, BreadcrumbComponent],
  templateUrl: './{entity}-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class {Entity}ListComponent implements OnInit {

  private readonly {entity}Service = inject({Entity}Service);

  // State
  items: {Entity}Response[] = [];
  totalCount = 0;
  isLoading = false;
  errorKey: string | null = null;

  // Pagination
  currentPage = 1;
  pageSize = 20;

  // Filter
  filter: {Entity}FilterRequest = {};

  // Breadcrumb
  breadcrumbs = [
    { labelKey: 'nav.home', routerLink: '/' },
    { labelKey: 'nav.{feature}', routerLink: '/{feature}' },
    { labelKey: '{feature}.{entities}.title' }
  ];

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.errorKey = null;

    this.{entity}Service.getList(this.filter, this.currentPage, this.pageSize)
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (response) => {
          this.items = response.data;
          this.totalCount = response.meta?.totalElements ?? 0;
          this.isLoading = false;
        },
        error: (err) => {
          this.errorKey = 'error.load_failed';
          this.isLoading = false;
        }
      });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadData();
  }

  onFilterChange(filter: {Entity}FilterRequest): void {
    this.filter = filter;
    this.currentPage = 0;
    this.loadData();
  }
}
```

### Template — HTML
```html
<!-- {entity}-list.component.html -->
<div class="page-container">

  <!-- Breadcrumb -->
  <app-breadcrumb [items]="breadcrumbs"></app-breadcrumb>

  <!-- Page Header -->
  <div class="page-header">
    <h1 class="page-title">{{ '{feature}.{entities}.title' | translate }}</h1>
    <button class="btn btn-primary" routerLink="/{feature}/{entities}/new"
            *hasPermission="'{ENTITY}_CREATE'">
      {{ 'common.button.create' | translate }}
    </button>
  </div>

  <!-- Loading -->
  <div class="loading-overlay" *ngIf="isLoading">
    <app-spinner></app-spinner>
  </div>

  <!-- Error -->
  <app-alert type="error" *ngIf="errorKey" [message]="errorKey | translate"></app-alert>

  <!-- Content -->
  <ng-container *ngIf="!isLoading && !errorKey">
    <app-{entity}-filter (filterChange)="onFilterChange($event)"></app-{entity}-filter>

    <app-{entity}-table [items]="items"></app-{entity}-table>

    <app-pagination
      [totalCount]="totalCount"
      [pageSize]="pageSize"
      [currentPage]="currentPage"
      (pageChange)="onPageChange($event)">
    </app-pagination>
  </ng-container>

</div>
```

### Checklist
```
[ ] OnPush change detection
[ ] Loading + error state
[ ] translate pipe cho text
[ ] takeUntilDestroyed cho unsubscribe
```
