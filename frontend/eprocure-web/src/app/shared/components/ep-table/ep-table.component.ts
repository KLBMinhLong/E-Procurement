import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { PageMeta } from '../../../core/models/api-response.model';
import { EpButtonComponent } from '../ep-button/ep-button.component';
import { EpIconComponent } from '../ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../ep-skeleton/ep-skeleton.component';

export interface EpTableColumn {
  key: string;
  labelKey: string;
  sortable?: boolean;
  align?: 'left' | 'right' | 'center';
  translationPrefix?: string;
}

export interface EpPageChangeEvent {
  page: number;
  size: number;
}

export interface EpSortChangeEvent {
  key: string;
  direction: 'asc' | 'desc';
}

@Component({
  selector: 'ep-table',
  standalone: true,
  imports: [TranslatePipe, EpButtonComponent, EpIconComponent, EpSkeletonComponent],
  templateUrl: './ep-table.component.html',
  styleUrl: './ep-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpTableComponent {
  readonly columns = input.required<EpTableColumn[]>();
  readonly data = input<Record<string, unknown>[]>([]);
  readonly meta = input<PageMeta | null>(null);
  readonly loading = input(false);

  readonly rowClick = output<Record<string, unknown>>();
  readonly pageChange = output<EpPageChangeEvent>();
  readonly sortChange = output<EpSortChangeEvent>();

  readonly hasRows = computed(() => this.data().length > 0);

  resolveCell(row: Record<string, unknown>, key: string): string {
    const value = row[key];
    return value === null || value === undefined ? '--' : String(value);
  }

  resolveTranslationKey(row: Record<string, unknown>, column: EpTableColumn): string {
    return `${column.translationPrefix}.${this.resolveCell(row, column.key)}`;
  }

  onPreviousPage(): void {
    const meta = this.meta();

    if (!meta || meta.isFirst) {
      return;
    }

    this.pageChange.emit({ page: meta.page - 1, size: meta.size });
  }

  onNextPage(): void {
    const meta = this.meta();

    if (!meta || meta.isLast) {
      return;
    }

    this.pageChange.emit({ page: meta.page + 1, size: meta.size });
  }

  onSort(column: EpTableColumn): void {
    if (!column.sortable) {
      return;
    }

    this.sortChange.emit({ key: column.key, direction: 'asc' });
  }
}
