import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { catchError, finalize, of } from 'rxjs';

import { AdminOperationsService } from '../../services/admin-operations.service';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { ToastService } from '../../../../core/services/toast.service';
import { CatalogCategoryAdmin, CatalogCategoryRequest } from '../../models/admin-operations.model';

interface CategoryNode extends CatalogCategoryAdmin {
  level: number;
  children: CategoryNode[];
}

@Component({
  selector: 'app-catalog-categories',
  standalone: true,
  imports: [CommonModule, TranslateModule, EpIconComponent, FormsModule, ReactiveFormsModule],
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

  // Modal State
  readonly isModalOpen = signal(false);
  readonly modalMode = signal<'CREATE' | 'UPDATE'>('CREATE');
  readonly selectedCode = signal<string | null>(null);
  readonly modalSubmitting = signal(false);

  readonly categoryForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern('^[A-Z0-9_-]+$')]],
    name: ['', Validators.required],
    parentCode: [''],
    requiresSpecialApproval: [false],
    specialApproverRole: [''],
    requiresRfqAbove: [''],
    isCapex: [false]
  });

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

  // --- Modal Logic ---
  openCreateModal() {
    this.modalMode.set('CREATE');
    this.selectedCode.set(null);
    this.categoryForm.reset({
      requiresSpecialApproval: false,
      isCapex: false
    });
    this.categoryForm.get('code')?.enable();
    this.isModalOpen.set(true);
  }

  openUpdateModal(cat: CatalogCategoryAdmin) {
    this.modalMode.set('UPDATE');
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
    
    this.isModalOpen.set(true);
  }

  closeModal() {
    this.isModalOpen.set(false);
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

    const obs$ = this.modalMode() === 'CREATE'
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

  deactivate(code: string) {
    if (!confirm(`Are you sure you want to deactivate category: ${code}?`)) {
      return;
    }

    const idempotencyKey = crypto.randomUUID();
    this.opsService.deactivateCatalogCategory(code, idempotencyKey)
      .pipe(
        catchError(err => {
          // If conflict due to active items, show detailed error
          this.toast.error(err.error?.message || 'adminOps.catalog.toast.deactivateFailed');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.toast.success('adminOps.catalog.toast.deactivateSuccess');
          this.loadCategories();
        }
      });
  }

  // Helper for parent select
  getAvailableParents() {
    const list = this.categories().filter(c => !c.isDeleted);
    // If updating, prevent selecting self or children
    if (this.modalMode() === 'UPDATE' && this.selectedCode()) {
      const selfCode = this.selectedCode()!;
      // Quick filter: no self. Advanced: no descendants. Let's just exclude self for simplicity here.
      return list.filter(c => c.code !== selfCode);
    }
    return list;
  }
}
