import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { EpButtonComponent } from '../ep-button/ep-button.component';
import { EpIconComponent } from '../ep-icon/ep-icon.component';

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
}
