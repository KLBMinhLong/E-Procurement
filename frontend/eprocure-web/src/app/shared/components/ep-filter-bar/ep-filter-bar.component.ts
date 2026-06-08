import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { EpButtonComponent } from '../ep-button/ep-button.component';
import { EpIconComponent } from '../ep-icon/ep-icon.component';

export interface EpFilterField {
  key: string;
  label: string;
  type: 'text' | 'select' | 'date';
  placeholder?: string;
  options?: { label: string; value: string }[];
}

@Component({
  selector: 'ep-filter-bar',
  standalone: true,
  imports: [FormsModule, TranslatePipe, EpButtonComponent, EpIconComponent],
  templateUrl: './ep-filter-bar.component.html',
  styleUrl: './ep-filter-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpFilterBarComponent {
  readonly placeholderKey = input('shared.filter.placeholder');
  readonly value = input('');
  readonly valueChange = output<string>();
  readonly refresh = output<void>();

  // Support for multiple filter fields
  readonly fields = input<EpFilterField[]>([]);
  readonly totalItems = input<number>(0);
  readonly filterChange = output<Record<string, string>>();
  
  filterValues: Record<string, string> = {};

  onFieldValueChange(key: string, value: string): void {
    this.filterValues[key] = value;
    this.filterChange.emit(this.filterValues);
  }
}
