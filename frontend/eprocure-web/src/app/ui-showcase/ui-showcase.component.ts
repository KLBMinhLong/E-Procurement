import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { PageMeta } from '../core/models/api-response.model';
import { EpAmountComponent } from '../shared/components/ep-amount/ep-amount.component';
import { EpApprovalActionComponent } from '../shared/components/ep-approval-action/ep-approval-action.component';
import { EpAvatarComponent } from '../shared/components/ep-avatar/ep-avatar.component';
import { EpBadgeComponent } from '../shared/components/ep-badge/ep-badge.component';
import { EpButtonComponent } from '../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFilterBarComponent } from '../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpFormFieldComponent } from '../shared/components/ep-form-field/ep-form-field.component';
import { EpLangSwitcherComponent } from '../shared/components/ep-lang-switcher/ep-lang-switcher.component';
import { EpModalComponent } from '../shared/components/ep-modal/ep-modal.component';
import { EpSkeletonComponent } from '../shared/components/ep-skeleton/ep-skeleton.component';
import { EpSlaBarComponent } from '../shared/components/ep-sla-bar/ep-sla-bar.component';
import { EpStatCardComponent } from '../shared/components/ep-stat-card/ep-stat-card.component';
import { EpTableColumn, EpTableComponent } from '../shared/components/ep-table/ep-table.component';

@Component({
  selector: 'ep-ui-showcase',
  standalone: true,
  imports: [
    TranslatePipe,
    EpAmountComponent,
    EpApprovalActionComponent,
    EpAvatarComponent,
    EpBadgeComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFilterBarComponent,
    EpFormFieldComponent,
    EpLangSwitcherComponent,
    EpModalComponent,
    EpSkeletonComponent,
    EpSlaBarComponent,
    EpStatCardComponent,
    EpTableComponent
  ],
  templateUrl: './ui-showcase.component.html',
  styleUrl: './ui-showcase.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UiShowcaseComponent {
  readonly modalOpen = signal(false);
  readonly query = signal('');
  readonly columns: EpTableColumn[] = [
    { key: 'code', labelKey: 'showcase.table.code', sortable: true },
    { key: 'status', labelKey: 'showcase.table.status', translationPrefix: 'status' },
    { key: 'amount', labelKey: 'showcase.table.amount', align: 'right' }
  ];
  readonly rows: Record<string, unknown>[] = [
    { code: 'PR-2026-00051', status: 'SUBMITTED', amount: '45,000,000 VND' },
    { code: 'PR-2026-00052', status: 'PENDING_APPROVAL', amount: '120,500,000 VND' }
  ];
  readonly meta: PageMeta = {
    page: 1,
    size: 10,
    totalElements: 2,
    totalPages: 1,
    isFirst: true,
    isLast: true
  };
}
