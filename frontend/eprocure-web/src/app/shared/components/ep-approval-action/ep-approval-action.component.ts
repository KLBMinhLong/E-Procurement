import { ChangeDetectionStrategy, Component, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { HasPermissionDirective } from '../../../core/permissions/has-permission.directive';
import { EpButtonComponent } from '../ep-button/ep-button.component';

@Component({
  selector: 'ep-approval-action',
  standalone: true,
  imports: [TranslatePipe, HasPermissionDirective, EpButtonComponent],
  templateUrl: './ep-approval-action.component.html',
  styleUrl: './ep-approval-action.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpApprovalActionComponent {
  readonly approve = output<void>();
  readonly requestChanges = output<void>();
  readonly reject = output<void>();
}
