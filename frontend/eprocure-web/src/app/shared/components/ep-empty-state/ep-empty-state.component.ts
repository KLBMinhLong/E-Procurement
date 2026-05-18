import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { EpIconComponent } from '../ep-icon/ep-icon.component';

@Component({
  selector: 'ep-empty-state',
  standalone: true,
  imports: [EpIconComponent],
  templateUrl: './ep-empty-state.component.html',
  styleUrl: './ep-empty-state.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpEmptyStateComponent {
  readonly icon = input('inbox');
  readonly title = input.required<string>();
  readonly message = input<string | null>(null);
}
