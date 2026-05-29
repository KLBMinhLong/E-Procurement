import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { EpButtonComponent } from '../ep-button/ep-button.component';
import { EpIconComponent } from '../ep-icon/ep-icon.component';

@Component({
  selector: 'ep-modal',
  standalone: true,
  imports: [TranslatePipe, EpButtonComponent, EpIconComponent],
  templateUrl: './ep-modal.component.html',
  styleUrl: './ep-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpModalComponent {
  readonly open = input(false);
  readonly title = input.required<string>();
  readonly size = input<'md' | 'lg' | 'xl'>('md');
  readonly close = output<void>();
}
