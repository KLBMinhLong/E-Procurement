import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { EpIconComponent } from '../ep-icon/ep-icon.component';

export type EpButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type EpButtonSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'ep-button',
  standalone: true,
  imports: [EpIconComponent],
  templateUrl: './ep-button.component.html',
  styleUrl: './ep-button.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpButtonComponent {
  readonly variant = input<EpButtonVariant>('primary');
  readonly size = input<EpButtonSize>('md');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly icon = input<string | null>(null);
  readonly loading = input(false);
  readonly disabled = input(false);
  readonly ariaLabel = input<string | null>(null);
}
