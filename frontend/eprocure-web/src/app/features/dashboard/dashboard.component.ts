import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { EpAmountComponent } from '../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent } from '../../shared/components/ep-badge/ep-badge.component';
import { EpCardComponent } from '../../shared/components/ep-card/ep-card.component';
import { EpFilterBarComponent } from '../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpSlaBarComponent } from '../../shared/components/ep-sla-bar/ep-sla-bar.component';
import { EpStatCardComponent } from '../../shared/components/ep-stat-card/ep-stat-card.component';
import { EpTableColumn, EpTableComponent } from '../../shared/components/ep-table/ep-table.component';

@Component({
  selector: 'ep-dashboard',
  standalone: true,
  imports: [
    TranslatePipe,
    EpAmountComponent,
    EpBadgeComponent,
    EpCardComponent,
    EpFilterBarComponent,
    EpSlaBarComponent,
    EpStatCardComponent,
    EpTableComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent {
  readonly query = signal('');
  readonly columns: EpTableColumn[] = [
    { key: 'code', labelKey: 'dashboard.table.code', sortable: true },
    { key: 'owner', labelKey: 'dashboard.table.owner' },
    { key: 'status', labelKey: 'dashboard.table.status', translationPrefix: 'status' },
    { key: 'amount', labelKey: 'dashboard.table.amount', align: 'right' }
  ];
  readonly rows = signal<Record<string, unknown>[]>([
    { code: 'PR-2026-00041', owner: 'Nguyen Van A', status: 'PENDING_APPROVAL', amount: '128,000,000 VND' },
    { code: 'PR-2026-00040', owner: 'Tran Thi B', status: 'APPROVED', amount: '86,450,000 VND' },
    { code: 'PR-2026-00039', owner: 'Le Minh C', status: 'CHANGES_REQUESTED', amount: '19,700,000 VND' }
  ]);
}
