import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';

@Component({
  selector: 'ep-admin-org-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent
  ],
  templateUrl: './org-chart.component.html',
  styleUrl: './org-chart.component.scss'
})
export class OrgChartComponent {
  readonly isLoading = signal(false);
}
