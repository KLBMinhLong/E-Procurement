import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { EpIconComponent } from '../ep-icon/ep-icon.component';

@Component({
  selector: 'ep-stat-card',
  standalone: true,
  imports: [EpIconComponent],
  templateUrl: './ep-stat-card.component.html',
  styleUrl: './ep-stat-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpStatCardComponent {
  readonly icon = input('activity');
  readonly title = input.required<string>();
  readonly value = input.required<string>();
  readonly hint = input<string | null>(null);
}
