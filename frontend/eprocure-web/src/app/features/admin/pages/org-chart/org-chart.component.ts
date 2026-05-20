import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { AdminOrgService } from '../../services/admin-org.service';
import { AdminUserService } from '../../services/admin-user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AdminDepartment, AdminUserSummary } from '../../models/admin.model';

@Component({
  selector: 'ep-admin-org-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    ReactiveFormsModule,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpIconComponent,
    EpModalComponent,
    EpFormFieldComponent
  ],
  templateUrl: './org-chart.component.html',
  styleUrl: './org-chart.component.scss'
})
export class OrgChartComponent implements OnInit {
  private readonly orgService = inject(AdminOrgService);
  private readonly userService = inject(AdminUserService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  // ── State ──────────────────────────────────────────────────────────
  readonly departments = signal<AdminDepartment[]>([]);
  readonly managers = signal<AdminUserSummary[]>([]);
  readonly expandedNodeIds = signal<Set<string>>(new Set());
  readonly isLoading = signal(false);

  // ── Modal State & Reactive Form ──────────────────────────────
  readonly isModalOpen = signal(false);
  readonly modalMode = signal<'create' | 'edit'>('create');
  readonly selectedDeptId = signal<string | null>(null);
  readonly isSubmitting = signal(false);

  // Flat department select list for dropdown parent selections
  readonly flatSelectDepartments = signal<AdminDepartment[]>([]);

  readonly deptForm: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[A-Z0-9_]+$/)]],
    name: ['', [Validators.required]],
    parentCode: [''],
    managerId: ['']
  });

  // Flatten tree for list tree rendering with indentation
  readonly flatDepartments = computed(() => {
    const raw = this.departments();
    const result: { node: AdminDepartment; depth: number; isExpanded: boolean; hasChildren: boolean }[] = [];
    const expanded = this.expandedNodeIds();

    const traverse = (nodes: AdminDepartment[], depth: number) => {
      for (const node of nodes) {
        const hasChildren = !!node.children && node.children.length > 0;
        const isExpanded = expanded.has(node.id);
        result.push({
          node,
          depth,
          isExpanded,
          hasChildren
        });
        if (hasChildren && isExpanded) {
          traverse(node.children!, depth + 1);
        }
      }
    };

    traverse(raw, 0);
    return result;
  });

  ngOnInit(): void {
    this.loadData();
    this.loadManagers();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.orgService
      .getDepartments()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          const data = res.data ?? [];
          this.departments.set(data);

          // Populate flat departments for dropdown parents list
          const flatList: AdminDepartment[] = [];
          const flatten = (nodes: AdminDepartment[]) => {
            for (const node of nodes) {
              flatList.push(node);
              if (node.children && node.children.length > 0) {
                flatten(node.children);
              }
            }
          };
          flatten(data);
          this.flatSelectDepartments.set(flatList);

          // Proactively expand all nodes by default for nice presentation
          const initialExpanded = new Set<string>();
          for (const item of flatList) {
            initialExpanded.add(item.id);
          }
          this.expandedNodeIds.set(initialExpanded);
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Failed to load departments');
        }
      });
  }

  loadManagers(): void {
    this.userService
      .list({ page: 1, size: 100 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.managers.set(res.data ?? []);
        }
      });
  }

  toggleNode(nodeId: string): void {
    this.expandedNodeIds.update(set => {
      const newSet = new Set(set);
      if (newSet.has(nodeId)) {
        newSet.delete(nodeId);
      } else {
        newSet.add(nodeId);
      }
      return newSet;
    });
  }

  // ── Modal Handlers ───────────────────────────────────────────────
  openCreateModal(parentDept?: AdminDepartment): void {
    this.modalMode.set('create');
    this.selectedDeptId.set(null);
    this.deptForm.reset({
      code: '',
      name: '',
      parentCode: parentDept ? parentDept.code : '',
      managerId: ''
    });
    this.deptForm.get('code')?.enable();
    this.isModalOpen.set(true);
  }

  openEditModal(dept: AdminDepartment): void {
    this.modalMode.set('edit');
    this.selectedDeptId.set(dept.id);
    this.deptForm.reset({
      code: dept.code,
      name: dept.name,
      parentCode: dept.parentCode || '',
      managerId: dept.managerId || ''
    });
    this.deptForm.get('code')?.disable();
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }

  onSubmit(): void {
    this.deptForm.markAllAsTouched();
    if (this.deptForm.invalid) {
      return;
    }

    this.isSubmitting.set(true);
    const formValue = this.deptForm.getRawValue();
    const obs$ = this.modalMode() === 'create'
      ? this.orgService.createDepartment({
          code: formValue.code,
          name: formValue.name,
          parentCode: formValue.parentCode || null,
          managerId: formValue.managerId || null
        })
      : this.orgService.updateDepartment(this.selectedDeptId()!, {
          name: formValue.name,
          parentCode: formValue.parentCode || null,
          managerId: formValue.managerId || null
        });

    obs$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.successKey(
            this.modalMode() === 'create'
              ? 'org.toast.createSuccess'
              : 'org.toast.updateSuccess'
          );
          this.closeModal();
          this.loadData();
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Save department failed');
        }
      });
  }
}
