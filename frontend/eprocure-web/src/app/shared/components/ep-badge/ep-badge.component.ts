import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

export type EpBadgeTone = 'neutral' | 'success' | 'warning' | 'danger' | 'info';

@Component({
  selector: 'ep-badge',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './ep-badge.component.html',
  styleUrl: './ep-badge.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpBadgeComponent {
  readonly tone = input<EpBadgeTone>('neutral');
  readonly labelKey = input<string | null>(null);
  readonly status = input<string | null>(null);

  readonly resolvedLabelKey = computed(() => this.labelKey() ?? `status.${this.status() ?? 'unknown'}`);
}
