import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { TranslatePipe } from '@ngx-translate/core';
import { EpButtonComponent } from '../ep-button/ep-button.component';
import { EpIconComponent } from '../ep-icon/ep-icon.component';

@Component({
  selector: 'ep-pagination',
  standalone: true,
  imports: [TranslatePipe, EpButtonComponent, EpIconComponent],
  templateUrl: './ep-pagination.component.html',
  styleUrl: './ep-pagination.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpPaginationComponent {
  // Support both property names used in the app
  readonly page = input<number>();
  readonly currentPage = input<number>();
  
  readonly size = input<number>(20);
  readonly totalElements = input<number>();
  readonly totalPages = input.required<number>();

  readonly pageChange = output<number>();

  // Use either page or currentPage depending on what was provided
  readonly activePage = computed(() => this.page() ?? this.currentPage() ?? 1);

  readonly hasPrevious = computed(() => this.activePage() > 1);
  readonly hasNext = computed(() => this.activePage() < this.totalPages());

  // Generate an array of page numbers to display
  readonly pages = computed(() => {
    const total = this.totalPages();
    const current = this.activePage();
    const result: (number | string)[] = [];
    
    if (total <= 7) {
      for (let i = 1; i <= total; i++) result.push(i);
    } else {
      if (current <= 4) {
        for (let i = 1; i <= 5; i++) result.push(i);
        result.push('...', total);
      } else if (current >= total - 3) {
        result.push(1, '...');
        for (let i = total - 4; i <= total; i++) result.push(i);
      } else {
        result.push(1, '...', current - 1, current, current + 1, '...', total);
      }
    }
    
    return result;
  });

  onPageClick(p: number | string): void {
    if (typeof p === 'number' && p !== this.activePage()) {
      this.pageChange.emit(p);
    }
  }

  onPrevious(): void {
    if (this.hasPrevious()) {
      this.pageChange.emit(this.activePage() - 1);
    }
  }

  onNext(): void {
    if (this.hasNext()) {
      this.pageChange.emit(this.activePage() + 1);
    }
  }
}
