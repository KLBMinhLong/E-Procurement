import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormArray,
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { ToastService } from '../../../../core/services/toast.service';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import {
  ApprovalPriority,
  ApprovalRuleDetail,
  ApprovalRuleStepTemplate,
  ApprovalRuleType,
  ApprovalRuleUpsertRequest,
  ApprovalStepType
} from '../../models/approvals.model';
import { ApprovalsService } from '../../services/approvals.service';
import { AdminDepartment, AdminPermission } from '../../../admin/models/admin.model';
import { AdminOrgService } from '../../../admin/services/admin-org.service';
import { AdminRbacService } from '../../../admin/services/admin-rbac.service';
import { CatalogCategory } from '../../../procurement/models/purchase-request.model';
import { CatalogService } from '../../../procurement/services/catalog.service';

type StepFormGroup = FormGroup<{
  stepIndex: FormControl<number>;
  requiredPermission: FormControl<string>;
  stepType: FormControl<ApprovalStepType>;
  slaHours: FormControl<number>;
  required: FormControl<boolean>;
}>;

type RuleForm = FormGroup<{
  ruleName: FormControl<string>;
  priority: FormControl<number>;
  active: FormControl<boolean>;
  ruleType: FormControl<ApprovalRuleType>;
  minValue: FormControl<string>;
  maxValue: FormControl<string>;
  categories: FormControl<string[]>;
  departmentIds: FormControl<string[]>;
  priorities: FormControl<ApprovalPriority[]>;
  description: FormControl<string>;
  steps: FormArray<StepFormGroup>;
}>;

type RuleTypeFilter = ApprovalRuleType | 'ALL';
type RuleStatusFilter = 'ALL' | 'ACTIVE' | 'INACTIVE';

interface RuleSummaryCard {
  icon: string;
  labelKey: string;
  value: number;
  tone: 'neutral' | 'success' | 'warning' | 'info';
}

const DEFAULT_APPROVAL_PERMISSIONS: AdminPermission[] = [
  {
    code: 'PR_APPROVE_L1',
    name: 'PR_APPROVE_L1',
    description: null,
    module: 'PROCUREMENT'
  },
  {
    code: 'PR_APPROVE_L2',
    name: 'PR_APPROVE_L2',
    description: null,
    module: 'PROCUREMENT'
  },
  {
    code: 'PR_APPROVE_L3',
    name: 'PR_APPROVE_L3',
    description: null,
    module: 'PROCUREMENT'
  },
  {
    code: 'PR_APPROVE_FINANCE',
    name: 'PR_APPROVE_FINANCE',
    description: null,
    module: 'PROCUREMENT'
  },
  {
    code: 'PR_APPROVE_EMERGENCY',
    name: 'PR_APPROVE_EMERGENCY',
    description: null,
    module: 'PROCUREMENT'
  }
];

@Component({
  selector: 'ep-approval-rules',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpModalComponent,
    EpSkeletonComponent
  ],
  templateUrl: './approval-rules.component.html',
  styleUrl: './approval-rules.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ApprovalRulesComponent implements OnInit {
  private readonly approvalsService = inject(ApprovalsService);
  private readonly orgService = inject(AdminOrgService);
  private readonly rbacService = inject(AdminRbacService);
  private readonly catalogService = inject(CatalogService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly ruleTypes: ApprovalRuleType[] = ['VALUE', 'CATEGORY', 'DEPARTMENT', 'DEFAULT'];
  readonly stepTypes: ApprovalStepType[] = ['SEQUENTIAL', 'PARALLEL'];
  readonly priorityOptions: ApprovalPriority[] = ['NORMAL', 'URGENT', 'EMERGENCY'];
  readonly typeFilters: RuleTypeFilter[] = ['ALL', ...this.ruleTypes];
  readonly statusFilters: RuleStatusFilter[] = ['ALL', 'ACTIVE', 'INACTIVE'];

  readonly rules = signal<ApprovalRuleDetail[]>([]);
  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);
  readonly searchQuery = signal('');
  readonly activeTypeFilter = signal<RuleTypeFilter>('ALL');
  readonly activeStatusFilter = signal<RuleStatusFilter>('ALL');
  readonly selectedRule = signal<ApprovalRuleDetail | null>(null);
  readonly selectedChainRule = signal<ApprovalRuleDetail | null>(null);
  readonly isFormOpen = signal(false);
  readonly isDeactivateOpen = signal(false);
  readonly deactivateReason = signal('');
  readonly departments = signal<AdminDepartment[]>([]);
  readonly catalogCategories = signal<CatalogCategory[]>([]);
  readonly permissionOptions = signal<AdminPermission[]>(DEFAULT_APPROVAL_PERMISSIONS);

  readonly form: RuleForm = this.fb.group({
    ruleName: this.fb.control('', { validators: [Validators.required] }),
    priority: this.fb.control(100, { validators: [Validators.required, Validators.min(1)] }),
    active: this.fb.control(true),
    ruleType: this.fb.control<ApprovalRuleType>('VALUE', { validators: [Validators.required] }),
    minValue: this.fb.control(''),
    maxValue: this.fb.control(''),
    categories: this.fb.control<string[]>([]),
    departmentIds: this.fb.control<string[]>([]),
    priorities: this.fb.control<ApprovalPriority[]>(['NORMAL', 'URGENT']),
    description: this.fb.control(''),
    steps: this.fb.array<StepFormGroup>([this.createStepGroup()])
  });

  readonly filteredRules = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const type = this.activeTypeFilter();
    const status = this.activeStatusFilter();

    return this.rules().filter((rule) =>
      this.matchesQuery(rule, query) &&
      (type === 'ALL' || rule.ruleType === type) &&
      this.matchesStatus(rule, status)
    );
  });

  readonly activeCount = computed(() => this.rules().filter((rule) => this.isRuleActive(rule)).length);
  readonly inactiveCount = computed(() => this.rules().length - this.activeCount());
  readonly totalSteps = computed(() => this.rules().reduce((sum, rule) => sum + rule.steps.length, 0));
  readonly hasActiveFilters = computed(() =>
    Boolean(this.searchQuery().trim()) ||
    this.activeTypeFilter() !== 'ALL' ||
    this.activeStatusFilter() !== 'ALL'
  );
  readonly summaryCards = computed<RuleSummaryCard[]>(() => [
    {
      icon: 'workflow',
      labelKey: 'approvals.rules.stat.total',
      value: this.rules().length,
      tone: 'neutral'
    },
    {
      icon: 'badge-check',
      labelKey: 'approvals.rules.stat.active',
      value: this.activeCount(),
      tone: 'success'
    },
    {
      icon: 'circle-off',
      labelKey: 'approvals.rules.stat.inactive',
      value: this.inactiveCount(),
      tone: 'warning'
    },
    {
      icon: 'list-checks',
      labelKey: 'approvals.rules.stat.steps',
      value: this.totalSteps(),
      tone: 'info'
    }
  ]);
  readonly selectedChainSteps = computed(() => {
    const rule = this.selectedChainRule();
    return rule ? this.sortedSteps(rule) : [];
  });
  readonly flatDepartments = computed(() => this.flattenDepartments(this.departments()));
  readonly flatCategories = computed(() => this.flattenCategories(this.catalogCategories()));
  readonly approvalPermissionOptions = computed(() => {
    const options = new Map(DEFAULT_APPROVAL_PERMISSIONS.map((permission) => [permission.code, permission]));
    this.permissionOptions()
      .filter((permission) => permission.code.startsWith('PR_APPROVE_'))
      .forEach((permission) => options.set(permission.code, permission));
    return [...options.values()].sort((left, right) => left.code.localeCompare(right.code));
  });

  get stepsArray(): FormArray<StepFormGroup> {
    return this.form.controls.steps;
  }

  ngOnInit(): void {
    this.loadReferenceData();
    this.loadRules();
  }

  loadRules(): void {
    this.isLoading.set(true);
    this.approvalsService.getRules()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (response) => this.rules.set(response.data ?? []),
        error: () => this.toastService.errorKey('approvals.rules.toast.loadFailed')
      });
  }

  openCreateModal(): void {
    this.selectedRule.set(null);
    this.resetForm();
    this.isFormOpen.set(true);
  }

  openEditModal(rule: ApprovalRuleDetail): void {
    this.selectedRule.set(rule);
    this.form.reset({
      ruleName: rule.ruleName,
      priority: rule.priority,
      active: this.isRuleActive(rule),
      ruleType: rule.ruleType,
      minValue: rule.conditions?.minValue ?? '',
      maxValue: rule.conditions?.maxValue ?? '',
      categories: rule.conditions?.categories ?? [],
      departmentIds: rule.conditions?.departmentIds ?? [],
      priorities: rule.conditions?.priorities?.length ? rule.conditions.priorities : [],
      description: rule.description ?? ''
    });
    this.stepsArray.clear();
    rule.steps.forEach((step) => this.stepsArray.push(this.createStepGroup(step)));
    if (this.stepsArray.length === 0) {
      this.stepsArray.push(this.createStepGroup());
    }
    this.isFormOpen.set(true);
  }

  closeFormModal(): void {
    this.isFormOpen.set(false);
    this.selectedRule.set(null);
  }

  openChainModal(rule: ApprovalRuleDetail): void {
    this.selectedChainRule.set(rule);
  }

  closeChainModal(): void {
    this.selectedChainRule.set(null);
  }

  addStep(): void {
    this.stepsArray.push(this.createStepGroup({ stepIndex: this.stepsArray.length + 1 }));
  }

  removeStep(index: number): void {
    if (this.stepsArray.length <= 1) {
      return;
    }
    this.stepsArray.removeAt(index);
    this.reindexSteps();
  }

  togglePriority(priority: ApprovalPriority, checked: boolean): void {
    const current = this.form.controls.priorities.value;
    const next = checked
      ? Array.from(new Set([...current, priority]))
      : current.filter((item) => item !== priority);
    this.form.controls.priorities.setValue(next);
  }

  toggleCategory(code: string, checked: boolean): void {
    this.toggleArrayValue(this.form.controls.categories, code, checked);
  }

  toggleDepartment(id: string, checked: boolean): void {
    this.toggleArrayValue(this.form.controls.departmentIds, id, checked);
  }

  isCategorySelected(code: string): boolean {
    return this.form.controls.categories.value.includes(code);
  }

  isDepartmentSelected(id: string): boolean {
    return this.form.controls.departmentIds.value.includes(id);
  }

  categoryLabel(code: string): string {
    const category = this.flatCategories().find((item) => item.code === code);
    return category ? `${category.code} - ${category.name}` : code;
  }

  departmentLabel(id: string): string {
    const department = this.flatDepartments().find((item) => item.id === id);
    return department ? `${department.code} - ${department.name}` : id;
  }

  submitForm(): void {
    if (this.form.invalid || this.stepsArray.length === 0) {
      this.form.markAllAsTouched();
      this.toastService.errorKey('approvals.rules.toast.validationFailed');
      return;
    }

    this.isSubmitting.set(true);
    const selected = this.selectedRule();
    const request = this.toRequest();
    const submit$ = selected
      ? this.approvalsService.updateRule(selected.id, request)
      : this.approvalsService.createRule(request);

    submit$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.successKey(selected ? 'approvals.rules.toast.updateSuccess' : 'approvals.rules.toast.createSuccess');
          this.closeFormModal();
          this.loadRules();
        },
        error: () => this.toastService.errorKey('approvals.rules.toast.saveFailed')
      });
  }

  openDeactivateModal(rule: ApprovalRuleDetail): void {
    this.selectedRule.set(rule);
    this.deactivateReason.set('');
    this.isDeactivateOpen.set(true);
  }

  closeDeactivateModal(): void {
    this.isDeactivateOpen.set(false);
    this.selectedRule.set(null);
    this.deactivateReason.set('');
  }

  confirmDeactivate(): void {
    const rule = this.selectedRule();
    const reason = this.deactivateReason().trim();
    if (!rule || !reason) {
      this.toastService.errorKey('approvals.rules.toast.reasonRequired');
      return;
    }

    this.isSubmitting.set(true);
    this.approvalsService.deactivateRule(rule.id, reason)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.successKey('approvals.rules.toast.deactivateSuccess');
          this.closeDeactivateModal();
          this.loadRules();
        },
        error: () => this.toastService.errorKey('approvals.rules.toast.deactivateFailed')
      });
  }

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
  }

  setTypeFilter(type: RuleTypeFilter): void {
    this.activeTypeFilter.set(type);
  }

  setStatusFilter(status: RuleStatusFilter): void {
    this.activeStatusFilter.set(status);
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.activeTypeFilter.set('ALL');
    this.activeStatusFilter.set('ALL');
  }

  setRuleType(type: ApprovalRuleType): void {
    this.form.controls.ruleType.setValue(type);
  }

  stepGroups(): StepFormGroup[] {
    return this.stepsArray.controls;
  }

  isRuleActive(rule: ApprovalRuleDetail): boolean {
    return rule.active ?? rule.isActive ?? false;
  }

  hasRuleConditions(rule: ApprovalRuleDetail): boolean {
    return Boolean(
      rule.conditions.minValue ||
      rule.conditions.maxValue ||
      rule.conditions.categories?.length ||
      rule.conditions.departmentIds?.length ||
      rule.conditions.priorities?.length
    );
  }

  firstStep(rule: ApprovalRuleDetail): ApprovalRuleStepTemplate | null {
    return this.sortedSteps(rule)[0] ?? null;
  }

  lastStep(rule: ApprovalRuleDetail): ApprovalRuleStepTemplate | null {
    const steps = this.sortedSteps(rule);
    return steps[steps.length - 1] ?? null;
  }

  hasDistinctLastStep(rule: ApprovalRuleDetail): boolean {
    const first = this.firstStep(rule);
    const last = this.lastStep(rule);
    return Boolean(first && last && (first.stepIndex !== last.stepIndex || first.requiredPermission !== last.requiredPermission));
  }

  parallelStepCount(rule: ApprovalRuleDetail): number {
    return rule.steps.filter((step) => step.stepType === 'PARALLEL').length;
  }

  totalSlaHours(rule: ApprovalRuleDetail): number {
    return rule.steps.reduce((sum, step) => sum + step.slaHours, 0);
  }

  private matchesQuery(rule: ApprovalRuleDetail, query: string): boolean {
    if (!query) {
      return true;
    }
    return rule.ruleName.toLowerCase().includes(query) ||
      rule.ruleType.toLowerCase().includes(query) ||
      rule.steps.some((step) => step.requiredPermission.toLowerCase().includes(query)) ||
      (rule.description?.toLowerCase().includes(query) ?? false);
  }

  private matchesStatus(rule: ApprovalRuleDetail, status: RuleStatusFilter): boolean {
    if (status === 'ALL') {
      return true;
    }
    return status === 'ACTIVE' ? this.isRuleActive(rule) : !this.isRuleActive(rule);
  }

  private sortedSteps(rule: ApprovalRuleDetail): ApprovalRuleStepTemplate[] {
    return [...rule.steps].sort((left, right) =>
      left.stepIndex - right.stepIndex || left.requiredPermission.localeCompare(right.requiredPermission)
    );
  }

  private resetForm(): void {
    this.form.reset({
      ruleName: '',
      priority: 100,
      active: true,
      ruleType: 'VALUE',
      minValue: '',
      maxValue: '',
      categories: [],
      departmentIds: [],
      priorities: ['NORMAL', 'URGENT'],
      description: ''
    });
    this.stepsArray.clear();
    this.stepsArray.push(this.createStepGroup());
  }

  private createStepGroup(step?: Partial<ReturnType<ApprovalRulesComponent['toStepValue']>>): StepFormGroup {
    return this.fb.group({
      stepIndex: this.fb.control(step?.stepIndex ?? 1, { validators: [Validators.required, Validators.min(1)] }),
      requiredPermission: this.fb.control(step?.requiredPermission ?? 'PR_APPROVE_L1', { validators: [Validators.required] }),
      stepType: this.fb.control<ApprovalStepType>(step?.stepType ?? 'SEQUENTIAL', { validators: [Validators.required] }),
      slaHours: this.fb.control(step?.slaHours ?? 48, { validators: [Validators.required, Validators.min(1)] }),
      required: this.fb.control(step?.required ?? true)
    });
  }

  private reindexSteps(): void {
    this.stepsArray.controls.forEach((group, index) => group.controls.stepIndex.setValue(index + 1));
  }

  private toRequest(): ApprovalRuleUpsertRequest {
    const value = this.form.getRawValue();
    return {
      ruleName: value.ruleName.trim(),
      priority: value.priority,
      active: value.active,
      ruleType: value.ruleType,
      conditions: {
        minValue: this.blankToNull(value.minValue),
        maxValue: this.blankToNull(value.maxValue),
        categories: value.categories,
        departmentIds: value.departmentIds,
        priorities: value.priorities
      },
      steps: value.steps.map((step) => this.toStepValue(step)),
      description: this.blankToNull(value.description)
    };
  }

  private toStepValue(step: {
    stepIndex: number;
    requiredPermission: string;
    stepType: ApprovalStepType;
    slaHours: number;
    required: boolean;
  }) {
    return {
      stepIndex: step.stepIndex,
      requiredPermission: step.requiredPermission.trim().toUpperCase(),
      stepType: step.stepType,
      slaHours: step.slaHours,
      required: step.required
    };
  }

  private blankToNull(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  private loadReferenceData(): void {
    this.orgService.getDepartments()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.departments.set(response.data ?? []),
        error: () => this.departments.set([])
      });

    this.catalogService.getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.catalogCategories.set(response.data ?? []),
        error: () => this.catalogCategories.set([])
      });

    this.rbacService.getPermissions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.permissionOptions.set(response.data?.length ? response.data : DEFAULT_APPROVAL_PERMISSIONS),
        error: () => this.permissionOptions.set(DEFAULT_APPROVAL_PERMISSIONS)
      });
  }

  private flattenDepartments(nodes: AdminDepartment[]): AdminDepartment[] {
    return nodes.flatMap((node) => [node, ...this.flattenDepartments(node.children ?? [])]);
  }

  private flattenCategories(nodes: CatalogCategory[]): CatalogCategory[] {
    return nodes.flatMap((node) => [node, ...this.flattenCategories(node.children ?? [])]);
  }

  private toggleArrayValue(control: FormControl<string[]>, value: string, checked: boolean): void {
    const current = control.value;
    const next = checked
      ? Array.from(new Set([...current, value]))
      : current.filter((item) => item !== value);
    control.setValue(next);
  }
}
