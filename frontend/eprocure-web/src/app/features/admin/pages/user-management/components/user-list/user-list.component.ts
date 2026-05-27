import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminUserSummary } from '../../../../models/admin.model';
import { PageMeta } from '../../../../../../core/models/api-response.model';
import { EpPageChangeEvent } from '../../../../../../shared/shared.index';
import { EpSkeletonComponent } from '../../../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpBadgeComponent, EpBadgeTone } from '../../../../../../shared/components/ep-badge/ep-badge.component';
import { EpAvatarComponent } from '../../../../../../shared/components/ep-avatar/ep-avatar.component';
import { EpButtonComponent } from '../../../../../../shared/components/ep-button/ep-button.component';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  ACTIVE: 'success',
  INACTIVE: 'neutral',
  LOCKED: 'danger',
  PENDING_VERIFY: 'warning'
};

@Component({
  selector: 'ep-admin-user-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpBadgeComponent,
    EpAvatarComponent,
    EpButtonComponent
  ],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss'
})
export class UserListComponent {
  @Input({ required: true }) items: AdminUserSummary[] = [];
  @Input() meta: PageMeta | null = null;
  @Input() isLoading = false;
  @Input() isStatusMutating: string | null = null;

  @Output() pageChange = new EventEmitter<EpPageChangeEvent>();
  @Output() requestEdit = new EventEmitter<AdminUserSummary>();
  @Output() requestResetPassword = new EventEmitter<AdminUserSummary>();
  @Output() requestToggleStatus = new EventEmitter<AdminUserSummary>();

  readonly statusTone = STATUS_TONE;
}
