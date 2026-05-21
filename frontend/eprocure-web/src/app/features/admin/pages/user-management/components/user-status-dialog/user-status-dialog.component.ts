import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminUserSummary } from '../../../../models/admin.model';
import { EpModalComponent } from '../../../../../../shared/components/ep-modal/ep-modal.component';
import { EpButtonComponent } from '../../../../../../shared/components/ep-button/ep-button.component';

@Component({
  selector: 'ep-admin-user-status-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, EpModalComponent, EpButtonComponent],
  templateUrl: './user-status-dialog.component.html',
  styleUrl: './user-status-dialog.component.scss'
})
export class UserStatusDialogComponent {
  @Input() isOpen = false;
  @Input() user: AdminUserSummary | null = null;

  @Output() closeDialog = new EventEmitter<void>();
  @Output() confirmToggle = new EventEmitter<string>();

  reason = '';

  onReasonInput(event: Event): void {
    this.reason = (event.target as HTMLTextAreaElement).value;
  }

  onConfirm(): void {
    this.confirmToggle.emit(this.reason);
    this.reason = ''; // Reset after emit
  }

  onClose(): void {
    this.closeDialog.emit();
    this.reason = ''; // Reset when closed
  }
}
