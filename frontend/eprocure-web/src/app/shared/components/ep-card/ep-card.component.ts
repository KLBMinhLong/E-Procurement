import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'ep-card',
  standalone: true,
  templateUrl: './ep-card.component.html',
  styleUrl: './ep-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpCardComponent {
  readonly tone = input<'default' | 'raised'>('default');
}
