import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type EpAmountValue =
  | string
  | number
  | {
      amount?: string | number | null;
      currency?: string | null;
    }
  | null
  | undefined;

@Component({
  selector: 'ep-amount',
  standalone: true,
  templateUrl: './ep-amount.component.html',
  styleUrl: './ep-amount.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpAmountComponent {
  readonly value = input<EpAmountValue>(null);
  readonly currency = input('VND');

  readonly formattedNumber = computed(() => {
    const rawValue = this.value();
    const value = this.amountValue(rawValue);

    if (value === null || value === undefined || value === '') {
      return '--';
    }

    const [integerPart, decimalPart] = String(value).split('.');
    const grouped = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    const decimals = decimalPart ? `.${decimalPart.slice(0, 4)}` : '';

    return `${grouped}${decimals}`;
  });

  readonly resolvedCurrency = computed(() => {
    return this.currencyValue(this.value());
  });

  private amountValue(value: EpAmountValue): string | number | null | undefined {
    if (this.isAmountObject(value)) {
      return value.amount;
    }
    return value;
  }

  private currencyValue(value: EpAmountValue): string {
    if (this.isAmountObject(value) && value.currency) {
      return value.currency;
    }
    return this.currency();
  }

  private isAmountObject(value: EpAmountValue): value is { amount?: string | number | null; currency?: string | null } {
    return typeof value === 'object' && value !== null && 'amount' in value;
  }
}
