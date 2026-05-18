import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { EpButtonComponent } from '../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../shared/components/ep-empty-state/ep-empty-state.component';

@Component({
  selector: 'ep-forbidden',
  standalone: true,
  imports: [RouterLink, TranslatePipe, EpButtonComponent, EpEmptyStateComponent],
  templateUrl: './forbidden.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ForbiddenComponent {}
