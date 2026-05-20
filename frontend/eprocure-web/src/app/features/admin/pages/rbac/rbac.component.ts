import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';

@Component({
  selector: 'ep-admin-rbac',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent
  ],
  templateUrl: './rbac.component.html',
  styleUrl: './rbac.component.scss'
})
export class RbacComponent {
  readonly isLoading = signal(false);
}
