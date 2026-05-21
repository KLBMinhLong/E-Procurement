import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { EpButtonComponent } from '../../../shared/components/ep-button/ep-button.component';
import { EpIconComponent } from '../../../shared/components/ep-icon/ep-icon.component';

@Component({
  selector: 'ep-not-found',
  standalone: true,
  imports: [RouterLink, TranslatePipe, EpButtonComponent, EpIconComponent],
  templateUrl: './not-found.component.html',
  styleUrl: './not-found.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotFoundComponent {}
