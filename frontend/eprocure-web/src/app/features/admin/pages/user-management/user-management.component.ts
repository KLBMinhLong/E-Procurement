import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';

@Component({
  selector: 'ep-admin-user-management',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpFilterBarComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent
  ],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss'
})
export class UserManagementComponent {
  readonly isLoading = signal(false);
  readonly searchQuery = signal('');

  onSearchChange(q: string): void {
    this.searchQuery.set(q);
  }

  loadData(): void {
    // Placeholder loading
  }
}
