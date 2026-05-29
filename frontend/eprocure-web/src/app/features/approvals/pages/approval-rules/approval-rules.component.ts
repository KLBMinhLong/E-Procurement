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

type StepFormGroup = FormGroup<{
  stepIndex: FormControl<number>;
  approverRole: FormControl<string>;
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
  categories: FormControl<string>;
  departmentIds: FormControl<string>;
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

  readonly form: RuleForm = this.fb.group({
    ruleName: this.fb.control('', { validators: [Validators.required] }),
    priority: this.fb.control(100, { validators: [Validators.required, Validators.min(1)] }),
    active: this.fb.control(true),
    ruleType: this.fb.control<ApprovalRuleType>('VALUE', { validators: [Validators.required] }),
    minValue: this.fb.control(''),
    maxValue: this.fb.control(''),
    categories: this.fb.control(''),
    departmentIds: this.fb.control(''),
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

  get stepsArray(): FormArray<StepFormGroup> {
    return this.form.controls.steps;
  }

  ngOnInit(): void {
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
      categories: this.joinValues(rule.conditions?.categories),
      departmentIds: this.joinValues(rule.conditions?.departmentIds),
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
    return Boolean(first && last && (first.stepIndex !== last.stepIndex || first.approverRole !== last.approverRole));
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
      rule.steps.some((step) => step.approverRole.toLowerCase().includes(query)) ||
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
      left.stepIndex - right.stepIndex || left.approverRole.localeCompare(right.approverRole)
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
      categories: '',
      departmentIds: '',
      priorities: ['NORMAL', 'URGENT'],
      description: ''
    });
    this.stepsArray.clear();
    this.stepsArray.push(this.createStepGroup());
  }

  private createStepGroup(step?: Partial<ReturnType<ApprovalRulesComponent['toStepValue']>>): StepFormGroup {
    return this.fb.group({
      stepIndex: this.fb.control(step?.stepIndex ?? 1, { validators: [Validators.required, Validators.min(1)] }),
      approverRole: this.fb.control(step?.approverRole ?? 'MANAGER', { validators: [Validators.required] }),
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
        categories: this.parseCsv(value.categories),
        departmentIds: this.parseCsv(value.departmentIds),
        priorities: value.priorities.length ? value.priorities : null
      },
      steps: value.steps.map((step) => this.toStepValue(step)),
      description: this.blankToNull(value.description)
    };
  }

  private toStepValue(step: {
    stepIndex: number;
    approverRole: string;
    stepType: ApprovalStepType;
    slaHours: number;
    required: boolean;
  }) {
    return {
      stepIndex: step.stepIndex,
      approverRole: step.approverRole.trim().toUpperCase(),
      stepType: step.stepType,
      slaHours: step.slaHours,
      required: step.required
    };
  }

  private parseCsv(value: string): string[] | null {
    const items = value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
    return items.length ? items : null;
  }

  private joinValues(values: string[] | null | undefined): string {
    return values?.join(', ') ?? '';
  }

  private blankToNull(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }
}
