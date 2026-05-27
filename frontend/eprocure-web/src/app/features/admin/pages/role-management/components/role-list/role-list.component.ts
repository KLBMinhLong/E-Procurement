import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminRole, AdminPermission } from '../../../../models/admin.model';
import { EpIconComponent } from '../../../../../../shared/components/ep-icon/ep-icon.component';
import { EpButtonComponent } from '../../../../../../shared/components/ep-button/ep-button.component';
import { EpSkeletonComponent } from '../../../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../../../shared/components/ep-empty-state/ep-empty-state.component';

@Component({
  selector: 'ep-admin-role-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpIconComponent,
    EpButtonComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent
  ],
  templateUrl: './role-list.component.html',
  styleUrl: './role-list.component.scss'
})
export class RoleListComponent {
  @Input({ required: true }) items: AdminRole[] = [];
  @Input({ required: true }) rolePermissionsMap = new Map<string, Set<string>>();
  @Input({ required: true }) permissions: AdminPermission[] = [];
  @Input() isLoading = false;

  @Output() editRole = new EventEmitter<AdminRole>();
  @Output() deleteRole = new EventEmitter<AdminRole>();

  // ── Expanded Role Detail State ─────────────────────────────────────────
  readonly expandedRoleCode = signal<string | null>(null);

  toggleExpand(roleCode: string): void {
    this.expandedRoleCode.update(current =>
      current === roleCode ? null : roleCode
    );
  }

  isExpanded(roleCode: string): boolean {
    return this.expandedRoleCode() === roleCode;
  }

  getRolePermissionCount(roleCode: string): number {
    return this.rolePermissionsMap.get(roleCode)?.size ?? 0;
  }

  getRolePermissionCodes(roleCode: string): string[] {
    return Array.from(this.rolePermissionsMap.get(roleCode) ?? []);
  }

  getPermissionName(code: string): string {
    return this.permissions.find(p => p.code === code)?.name ?? code;
  }

  getPermissionModule(code: string): string {
    return this.permissions.find(p => p.code === code)?.module ?? 'SYSTEM';
  }
}
