import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';

@Component({
  selector: 'ep-budget-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent
  ],
  templateUrl: './budget-detail.component.html',
  styleUrl: './budget-detail.component.scss'
})
export class BudgetDetailComponent {}
