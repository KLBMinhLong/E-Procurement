import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';

@Component({
  selector: 'ep-icon',
  standalone: true,
  imports: [LucideDynamicIcon],
  templateUrl: './ep-icon.component.html',
  styleUrl: './ep-icon.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpIconComponent {
  readonly name = input.required<string>();
  readonly size = input<number>(18);
  readonly title = input<string | null>(null);
}
