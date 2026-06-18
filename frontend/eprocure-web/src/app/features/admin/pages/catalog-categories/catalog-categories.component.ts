import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, finalize, of } from 'rxjs';

import { AdminOperationsService } from '../../services/admin-operations.service';
import { ToastService } from '../../../../core/services/toast.service';
import { CatalogCategoryAdmin, CatalogCategoryRequest } from '../../models/admin-operations.model';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpStatCardComponent } from '../../../../shared/components/ep-stat-card/ep-stat-card.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';

interface CategoryNode extends CatalogCategoryAdmin {
  level: number;
  children: CategoryNode[];
}

@Component({
  selector: 'ep-admin-catalog-categories',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpBreadcrumbComponent,
    EpStatCardComponent,
    EpFilterBarComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpBadgeComponent,
    EpModalComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpButtonComponent
  ],
  templateUrl: './catalog-categories.component.html',
  styleUrl: './catalog-categories.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CatalogCategoriesComponent implements OnInit {
  private readonly opsService = inject(AdminOperationsService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  // State
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly includeInactive = signal(false);

  readonly categories = signal<CatalogCategoryAdmin[]>([]);

  // Client-side filter
  readonly searchQuery = signal('');

  // Deactivate target
  readonly deactivateTarget = signal<CategoryNode | null>(null);

  // Modal state — supports create, edit, deactivate modes
  readonly activeModal = signal<'create' | 'edit' | 'deactivate' | null>(null);

  // Computed Tree (flattened for table display)
  readonly flatTree = computed(() => {
    const list = this.categories();
    if (!list.length) return [];

    const map = new Map<string, CategoryNode>();
    const roots: CategoryNode[] = [];

    // Initialize nodes
    list.forEach(c => map.set(c.code, { ...c, level: 0, children: [] }));

    // Build tree
    list.forEach(c => {
      const node = map.get(c.code)!;
      if (c.parentCode && map.has(c.parentCode)) {
        map.get(c.parentCode)!.children.push(node);
      } else {
        roots.push(node);
      }
    });

    // Flatten tree with levels
    const flattened: CategoryNode[] = [];
    const traverse = (node: CategoryNode, level: number) => {
      node.level = level;
      flattened.push(node);
      // Sort children by name
      node.children.sort((a, b) => a.name.localeCompare(b.name));
      node.children.forEach(child => traverse(child, level + 1));
    };

    roots.sort((a, b) => a.name.localeCompare(b.name));
    roots.forEach(root => traverse(root, 0));

    return flattened;
  });

  // Filtered tree — applies searchQuery client-side filter over flatTree
  readonly filteredTree = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) return this.flatTree();
    return this.flatTree().filter(
      node =>
        node.name.toLowerCase().includes(q) ||
        node.code.toLowerCase().includes(q)
    );
  });

  // Computed stats
  readonly activeCount = computed(() =>
    this.categories().filter(c => !c.isDeleted).length
  );

  readonly totalItems = computed(() =>
    this.categories().reduce((sum, c) => sum + c.itemCount, 0)
  );

  readonly specialApprovalCount = computed(() =>
    this.categories().filter(c => c.requiresSpecialApproval === true).length
  );

  // Form (create / edit category)
  readonly categoryForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern('^[A-Z0-9_-]+$')]],
    name: ['', Validators.required],
    parentCode: [''],
    requiresSpecialApproval: [false],
    specialApproverRole: [''],
    requiresRfqAbove: [''],
    isCapex: [false]
  });

  // Legacy modal signals kept for template compatibility
  readonly modalSubmitting = signal(false);
  readonly selectedCode = signal<string | null>(null);

  ngOnInit() {
    this.loadCategories();

    // Toggle special approver role field based on checkbox
    this.categoryForm.get('requiresSpecialApproval')?.valueChanges.subscribe(req => {
      const roleCtrl = this.categoryForm.get('specialApproverRole');
      if (req) {
        roleCtrl?.setValidators([Validators.required]);
      } else {
        roleCtrl?.clearValidators();
        roleCtrl?.setValue('');
      }
      roleCtrl?.updateValueAndValidity();
    });
  }

  toggleInactive(event: Event) {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.includeInactive.set(isChecked);
    this.loadCategories();
  }

  loadCategories() {
    this.loading.set(true);
    this.error.set(null);

    this.opsService.listCatalogCategories(this.includeInactive())
      .pipe(
        finalize(() => this.loading.set(false)),
        catchError(err => {
          this.error.set(err.error?.message || 'Failed to load categories');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.categories.set(res.data);
        }
      });
  }

  // --- Create / Edit Modal Logic ---

  openCreateModal() {
    this.selectedCode.set(null);
    this.categoryForm.reset({
      requiresSpecialApproval: false,
      isCapex: false
    });
    this.categoryForm.get('code')?.enable();
    this.activeModal.set('create');
  }

  openUpdateModal(cat: CatalogCategoryAdmin) {
    this.selectedCode.set(cat.code);

    this.categoryForm.patchValue({
      code: cat.code,
      name: cat.name,
      parentCode: cat.parentCode || '',
      requiresSpecialApproval: cat.requiresSpecialApproval,
      specialApproverRole: cat.specialApproverRole || '',
      requiresRfqAbove: cat.requiresRfqAbove || '',
      isCapex: cat.isCapex
    });
    this.categoryForm.get('code')?.disable(); // Code cannot be changed

    this.activeModal.set('edit');
  }

  closeModal() {
    this.activeModal.set(null);
  }

  submitCategory() {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    const val = this.categoryForm.getRawValue();
    const request: CatalogCategoryRequest = {
      code: val.code!,
      name: val.name!,
      parentCode: val.parentCode || null,
      requiresSpecialApproval: val.requiresSpecialApproval || false,
      specialApproverRole: val.specialApproverRole || null,
      requiresRfqAbove: val.requiresRfqAbove ? String(val.requiresRfqAbove) : null,
      isCapex: val.isCapex || false
    };

    this.modalSubmitting.set(true);
    const idempotencyKey = crypto.randomUUID();

    const mode = this.activeModal();
    const obs$ = mode === 'create'
      ? this.opsService.createCatalogCategory(request, idempotencyKey)
      : this.opsService.updateCatalogCategory(this.selectedCode()!, request, idempotencyKey);

    obs$.pipe(
      finalize(() => this.modalSubmitting.set(false)),
      catchError(err => {
        this.toast.error(err.error?.message || 'adminOps.catalog.toast.saveFailed');
        return of(null);
      })
    ).subscribe(res => {
      if (res?.success) {
        this.toast.success('adminOps.catalog.toast.saveSuccess');
        this.closeModal();
        this.loadCategories();
      }
    });
  }

  // --- Deactivate Modal Logic ---

  /** Opens the deactivate confirmation modal — replaces the browser confirm() dialog. */
  openDeactivateModal(cat: CategoryNode) {
    this.deactivateTarget.set(cat);
    this.activeModal.set('deactivate');
  }

  /** Executes deactivation after user confirms in the modal. */
  submitDeactivate() {
    const cat = this.deactivateTarget();
    if (!cat) return;

    const idempotencyKey = crypto.randomUUID();

    this.opsService.deactivateCatalogCategory(cat.code, idempotencyKey)
      .pipe(
        catchError(err => {
          if (err.status === 409) {
            // Conflict — category still has active items or children
            this.toast.error(err.error?.message || 'adminOps.catalog.toast.deactivateConflict');
          } else {
            this.toast.error(err.error?.message || 'adminOps.catalog.toast.deactivateFailed');
          }
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.toast.success('adminOps.catalog.toast.deactivateSuccess');
          this.closeModal();
          this.deactivateTarget.set(null);
          this.loadCategories();
        }
      });
  }

  // Helper for parent select
  getAvailableParents() {
    const list = this.categories().filter(c => !c.isDeleted);
    // If updating, prevent selecting self or children
    if (this.activeModal() === 'edit' && this.selectedCode()) {
      const selfCode = this.selectedCode()!;
      return list.filter(c => c.code !== selfCode);
    }
    return list;
  }
}
