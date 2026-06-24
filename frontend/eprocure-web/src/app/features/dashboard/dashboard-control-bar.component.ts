import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { EpBreadcrumbComponent } from '../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../shared/components/ep-icon/ep-icon.component';
import { DashboardDepartmentOption, DashboardTab, DashboardTabItem } from './dashboard-ui.model';

@Component({
  selector: 'ep-dashboard-control-bar',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpFormFieldComponent,
    EpIconComponent
  ],
  templateUrl: './dashboard-control-bar.component.html',
  styleUrl: './dashboard-control-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardControlBarComponent implements OnChanges {
  @Input({ required: true }) filterForm!: FormGroup;
  @Input({ required: true }) tabs: DashboardTabItem[] = [];
  @Input({ required: true }) quarters: readonly number[] = [];
  @Input() activeTab: DashboardTab = 'executive';
  @Input() departmentOptions: DashboardDepartmentOption[] = [];
  @Input() departmentsLoading = false;
  @Input() departmentsLoadFailed = false;
  @Input() refreshing = false;
  @Input() lastUpdatedLabel: string | null = null;
  @Input() cachedAtLabel: string | null = null;
  @Input() dataStale = false;

  @Output() readonly applyFilters = new EventEmitter<void>();
  @Output() readonly refreshDashboard = new EventEmitter<void>();
  @Output() readonly tabChange = new EventEmitter<DashboardTab>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['departmentsLoading'] && this.filterForm) {
      const ctrl = this.filterForm.get('departmentId');
      if (ctrl) {
        if (this.departmentsLoading) {
          ctrl.disable({ emitEvent: false });
        } else {
          ctrl.enable({ emitEvent: false });
        }
      }
    }
  }
}
