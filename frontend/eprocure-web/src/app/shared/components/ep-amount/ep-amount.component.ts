import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'ep-amount',
  standalone: true,
  templateUrl: './ep-amount.component.html',
  styleUrl: './ep-amount.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpAmountComponent {
  readonly value = input<string | number | null | undefined>(null);
  readonly currency = input('VND');

  readonly formattedValue = computed(() => {
    const value = this.value();

    if (value === null || value === undefined || value === '') {
      return '--';
    }

    const [integerPart, decimalPart] = String(value).split('.');
    const grouped = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    const decimals = decimalPart ? `.${decimalPart.slice(0, 4)}` : '';

    return `${grouped}${decimals} ${this.currency()}`;
  });
}
