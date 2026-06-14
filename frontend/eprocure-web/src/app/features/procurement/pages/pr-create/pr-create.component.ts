import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, debounceTime, distinctUntilChanged, finalize, of, Subject, switchMap } from 'rxjs';

import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { ToastService } from '../../../../core/services/toast.service';

import { PurchaseRequestService } from '../../services/purchase-request.service';
import { CatalogService } from '../../services/catalog.service';
import {
  CatalogCategory,
  CatalogItem,
  CreatePrRequest,
  PrLineItemRequest,
  PrLineItemResponse,
  PrPriority
} from '../../models/purchase-request.model';

interface CatalogAutocompleteQuery {
  index: number;
  q: string;
}

@Component({
  selector: 'ep-pr-create',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpButtonComponent,
    EpFormFieldComponent,
    EpBreadcrumbComponent,
    EpAmountComponent,
    EpCardComponent,
    EpIconComponent,
    EpSkeletonComponent,
    EpModalComponent
  ],
  templateUrl: './pr-create.component.html',
  styleUrl: './pr-create.component.scss'
})
export class PrCreateComponent implements OnInit {
  private readonly prService = inject(PurchaseRequestService);
  private readonly catalogService = inject(CatalogService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  // ── State ──────────────────────────────────────────────────────────
  readonly isSubmitting = signal(false);
  readonly isEditLoading = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly hasSubmitAttempted = signal(false);
  readonly isUploadingFile = signal(false);
  readonly showCatalogPicker = signal(false);
  readonly catalogPickerTargetIndex = signal<number | null>(null);
  readonly catalogSearchQuery = signal('');
  readonly catalogItems = signal<CatalogItem[]>([]);
  readonly catalogCategories = signal<CatalogCategory[]>([]);
  readonly isCatalogLoading = signal(false);
  readonly autocompleteOpenMap = signal<Map<number, boolean>>(new Map());
  readonly autocompleteItemsMap = signal<Map<number, CatalogItem[]>>(new Map());
  readonly autocompleteLoadingMap = signal<Map<number, boolean>>(new Map());
  readonly uploadedAttachments = signal<{ id: string; fileName: string; fileSize: number }[]>([]);
  readonly formRevision = signal(0);
  private readonly autocompleteQuery$ = new Subject<CatalogAutocompleteQuery>();

  readonly priorities: PrPriority[] = ['NORMAL', 'URGENT', 'EMERGENCY'];

  readonly isEditMode = computed(() => Boolean(this.editingId()));

  readonly showUrgencyReason = computed(() => {
    this.formRevision();
    const p = this.form.get('priority')?.value as PrPriority;
    return p === 'URGENT' || p === 'EMERGENCY';
  });

  readonly lineItemTotals = computed(() => {
    this.formRevision();
    return this.lineItems.controls.map((ctrl) => this.calculateLineTotal(ctrl));
  });

  readonly totalAmount = computed(() => {
    this.formRevision();
    return this.lineItems.controls
      .reduce((total, ctrl) => total + this.calculateLineTotalNumber(ctrl), 0)
      .toFixed(4);
  });

  readonly showValidationSummary = computed(() => {
    this.formRevision();
    return this.hasSubmitAttempted() && this.form.invalid;
  });

  // ── Form ────────────────────────────────────────────────────────────
  readonly form: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(500)]],
    justification: ['', [Validators.required, Validators.minLength(50)]],
    priority: ['NORMAL' as PrPriority, Validators.required],
    urgencyReason: [null as string | null],
    needByDate: [null as string | null],
    lineItems: this.fb.array([this.createLineItemGroup()])
  });

  get lineItems(): FormArray {
    return this.form.get('lineItems') as FormArray;
  }

  // ── Lifecycle ──────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadCategories();
    this.bindAutocompleteSearch();
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.bumpFormRevision());
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const editId = params.get('edit');
        this.editingId.set(editId);
        if (editId) {
          this.loadForEdit(editId);
        }
      });
  }

  // ── Line item helpers ──────────────────────────────────────────────
  createLineItemGroup(item?: Partial<PrLineItemResponse>): FormGroup {
    return this.fb.group({
      itemCode: [item?.itemCode ?? null as string | null],
      itemName: [item?.itemName ?? '', [Validators.required, Validators.maxLength(300)]],
      description: [item?.description ?? null as string | null],
      categoryCode: [item?.categoryCode ?? '', Validators.required],
      quantityAmount: [item?.quantity?.amount ?? '1', [Validators.required, Validators.min(0.01)]],
      quantityUnit: [item?.quantity?.unit ?? 'EA', Validators.required],
      unitPriceAmount: [item?.unitPrice?.amount ?? '0', [Validators.required, Validators.min(0)]],
      glAccountCode: [item?.glAccountCode ?? '', [Validators.required, Validators.maxLength(10)]],
      specifications: [item?.specifications ?? null as string | null],
      isFromCatalog: [item?.isFromCatalog ?? false]
    });
  }

  addLineItem(): void {
    this.lineItems.push(this.createLineItemGroup());
    this.bumpFormRevision();
  }

  removeLineItem(index: number): void {
    if (this.lineItems.length > 1) {
      this.lineItems.removeAt(index);
      this.clearAutocompleteState();
      this.bumpFormRevision();
    }
  }

  autocompleteOpen(index: number): boolean {
    return this.autocompleteOpenMap().get(index) ?? false;
  }

  autocompleteItems(index: number): CatalogItem[] {
    return this.autocompleteItemsMap().get(index) ?? [];
  }

  autocompleteLoading(index: number): boolean {
    return this.autocompleteLoadingMap().get(index) ?? false;
  }

  onItemNameInput(index: number, value: string): void {
    const q = value.trim();
    if (q.length < 2) {
      this.setAutocompleteOpen(index, false);
      this.setAutocompleteItems(index, []);
      this.setAutocompleteLoading(index, false);
      this.autocompleteQuery$.next({ index, q });
      return;
    }

    this.setAutocompleteLoading(index, true);
    this.autocompleteQuery$.next({ index, q });
  }

  onItemNameFocus(index: number): void {
    if (this.autocompleteItems(index).length > 0) {
      this.setAutocompleteOpen(index, true);
    }
  }

  onItemNameBlur(index: number): void {
    window.setTimeout(() => this.setAutocompleteOpen(index, false), 120);
  }

  selectAutocompleteItem(index: number, item: CatalogItem): void {
    this.patchLineItemFromCatalog(index, item);
    this.setAutocompleteOpen(index, false);
  }

  getLineItemError(index: number, field: string): string | null {
    const ctrl = this.lineItems.at(index).get(field);
    if (ctrl?.invalid && (ctrl.touched || this.hasSubmitAttempted())) {
      if (ctrl.errors?.['required']) return 'validation.required';
      if (ctrl.errors?.['min']) return 'validation.min';
      if (ctrl.errors?.['maxlength']) return 'validation.maxLength';
    }
    return null;
  }

  lineItemTotal(index: number): string {
    return this.lineItemTotals()[index] ?? '0.0000';
  }

  // ── Catalog picker ─────────────────────────────────────────────────
  openCatalogPicker(index: number): void {
    this.catalogPickerTargetIndex.set(index);
    this.showCatalogPicker.set(true);
    this.catalogSearchQuery.set('');
    this.catalogItems.set([]);
  }

  closeCatalogPicker(): void {
    this.showCatalogPicker.set(false);
    this.catalogPickerTargetIndex.set(null);
  }

  onCatalogSearch(q: string): void {
    this.catalogSearchQuery.set(q);
    if (q.length < 2) { this.catalogItems.set([]); return; }
    this.isCatalogLoading.set(true);
    this.catalogService.searchItems({ q, size: 20 })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isCatalogLoading.set(false)))
      .subscribe({ next: (res) => this.catalogItems.set(res.data ?? []) });
  }

  selectCatalogItem(item: CatalogItem): void {
    const idx = this.catalogPickerTargetIndex();
    if (idx === null) return;
    this.patchLineItemFromCatalog(idx, item);
    this.closeCatalogPicker();
  }

  // ── Attachment upload ──────────────────────────────────────────────
  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.isUploadingFile.set(true);
    this.prService.uploadAttachment(file)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isUploadingFile.set(false)))
      .subscribe({
        next: (res) => {
          this.uploadedAttachments.update((list) => [
            ...list,
            { id: res.data.id, fileName: res.data.fileName, fileSize: res.data.fileSize }
          ]);
        }
      });
    input.value = '';
  }

  removeAttachment(id: string): void {
    this.uploadedAttachments.update((list) => list.filter((a) => a.id !== id));
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  // ── Submit ─────────────────────────────────────────────────────────
  onSubmit(): void {
    this.hasSubmitAttempted.set(true);
    this.form.markAllAsTouched();
    this.bumpFormRevision();
    if (this.form.invalid) {
      this.toastService.warningKey('pr.create.toast.validationFailed');
      return;
    }
    this.isSubmitting.set(true);

    const v = this.form.value;
    const lineItems: PrLineItemRequest[] = v.lineItems.map((li: any) => ({
      itemCode: li.itemCode || null,
      itemName: li.itemName,
      description: li.description || null,
      categoryCode: li.categoryCode,
      quantity: { amount: li.quantityAmount, unit: li.quantityUnit },
      unitPrice: { amount: li.unitPriceAmount, currency: 'VND' },
      glAccountCode: li.glAccountCode,
      specifications: li.specifications || null,
      isFromCatalog: li.isFromCatalog ?? false
    }));

    const request: CreatePrRequest = {
      title: v.title,
      justification: v.justification,
      priority: v.priority,
      urgencyReason: this.showUrgencyReason() ? v.urgencyReason : null,
      needByDate: v.needByDate || null,
      lineItems
    };

    const editId = this.editingId();
    if (editId) {
      this.prService.update(editId, request)
        .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isSubmitting.set(false)))
        .subscribe({
          next: () => {
            this.toastService.successKey('pr.create.toast.updateSuccess');
            this.router.navigate(['/procurement', editId]);
          }
        });
      return;
    }

    this.prService.create({
      ...request,
      attachmentIds: this.uploadedAttachments().map((a) => a.id)
    })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (res) => {
          this.toastService.successKey('pr.create.toast.createSuccess');
          this.router.navigate(['/procurement', res.data.id]);
        }
      });
  }

  onCancel(): void {
    this.router.navigate(['/procurement']);
  }

  // ── Private ────────────────────────────────────────────────────────
  private loadCategories(): void {
    this.catalogService.getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (res) => this.catalogCategories.set(res.data ?? []) });
  }

  private loadForEdit(id: string): void {
    this.isEditLoading.set(true);
    this.prService.getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isEditLoading.set(false)))
      .subscribe({
        next: (res) => {
          const data = res.data;
          this.form.patchValue({
            title: data.title,
            justification: data.justification,
            priority: data.priority,
            urgencyReason: data.urgencyReason,
            needByDate: this.toDateInput(data.needByDate)
          });
          this.lineItems.clear();
          data.lineItems.forEach((item) => this.lineItems.push(this.createLineItemGroup(item)));
          this.clearAutocompleteState();
          if (!this.lineItems.length) {
            this.addLineItem();
          }
          this.bumpFormRevision();
          this.uploadedAttachments.set(data.attachments?.map((att) => ({
            id: att.id,
            fileName: att.fileName,
            fileSize: att.fileSize
          })) ?? []);
        },
        error: () => this.router.navigate(['/procurement'])
      });
  }

  private toDateInput(value: string | null | undefined): string | null {
    return value ? value.slice(0, 10) : null;
  }

  private bindAutocompleteSearch(): void {
    this.autocompleteQuery$
      .pipe(
        debounceTime(300),
        distinctUntilChanged((prev, curr) => prev.index === curr.index && prev.q === curr.q),
        switchMap(({ index, q }) => {
          if (q.length < 2) {
            return of({ index, items: [] as CatalogItem[], open: false });
          }

          return this.catalogService.searchItems({ q, size: 8 }).pipe(
            catchError(() => of({ data: [] as CatalogItem[] })),
            finalize(() => this.setAutocompleteLoading(index, false)),
            switchMap((res) => of({ index, items: res.data ?? [], open: true }))
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(({ index, items, open }) => {
        this.setAutocompleteItems(index, items.slice(0, 8));
        this.setAutocompleteOpen(index, open);
      });
  }

  private patchLineItemFromCatalog(index: number, item: CatalogItem): void {
    const ctrl = this.lineItems.at(index);
    ctrl.patchValue({
      itemCode: item.itemCode,
      itemName: item.name,
      categoryCode: item.categoryCode,
      unitPriceAmount: item.unitPrice.amount,
      quantityUnit: item.unit,
      isFromCatalog: true
    });
    this.bumpFormRevision();
  }

  private calculateLineTotal(ctrl: AbstractControl): string {
    return this.calculateLineTotalNumber(ctrl).toFixed(4);
  }

  private calculateLineTotalNumber(ctrl: AbstractControl): number {
    const qty = Number(ctrl.get('quantityAmount')?.value || 0);
    const price = Number(ctrl.get('unitPriceAmount')?.value || 0);
    return Number.isFinite(qty) && Number.isFinite(price) ? qty * price : 0;
  }

  private bumpFormRevision(): void {
    this.formRevision.update((value) => value + 1);
  }

  private setAutocompleteOpen(index: number, open: boolean): void {
    this.autocompleteOpenMap.update((current) => {
      const next = new Map(current);
      next.set(index, open);
      return next;
    });
  }

  private setAutocompleteItems(index: number, items: CatalogItem[]): void {
    this.autocompleteItemsMap.update((current) => {
      const next = new Map(current);
      next.set(index, items);
      return next;
    });
  }

  private setAutocompleteLoading(index: number, loading: boolean): void {
    this.autocompleteLoadingMap.update((current) => {
      const next = new Map(current);
      next.set(index, loading);
      return next;
    });
  }

  private clearAutocompleteState(): void {
    this.autocompleteOpenMap.set(new Map());
    this.autocompleteItemsMap.set(new Map());
    this.autocompleteLoadingMap.set(new Map());
  }

  // ── Field error helpers ────────────────────────────────────────────
  fieldError(field: string): string | null {
    const ctrl = this.form.get(field);
    if (!ctrl?.invalid || (!ctrl.touched && !this.hasSubmitAttempted())) return null;
    if (ctrl.errors?.['required']) return 'validation.required';
    if (ctrl.errors?.['minlength']) return 'validation.minLength';
    if (ctrl.errors?.['maxlength']) return 'validation.maxLength';
    return null;
  }
}
