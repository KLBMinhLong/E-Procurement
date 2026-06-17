import { DatePipe, DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { AdminOperationsService } from '../../services/admin-operations.service';
import {
  InfraKafkaHealth,
  InfraPostgresHealth,
  InfraRedisHealth,
  OverallHealthStatus,
  ServiceHealth,
  ServiceHealthStatus,
  SystemHealthData
} from '../../models/admin-operations.model';

@Component({
  selector: 'ep-system-health',
  standalone: true,
  imports: [DatePipe, DecimalPipe, TranslatePipe, EpButtonComponent, EpIconComponent],
  templateUrl: './system-health.component.html',
  styleUrl: './system-health.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SystemHealthComponent implements OnInit {
  private readonly adminOps = inject(AdminOperationsService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly healthData = signal<SystemHealthData | null>(null);

  readonly overallStatus = computed(() => this.healthData()?.overallStatus ?? 'UNKNOWN');
  readonly services = computed(() => this.healthData()?.services ?? []);
  readonly infra = computed(() => this.healthData()?.infrastructure ?? null);
  readonly checkedAt = computed(() => this.healthData()?.checkedAt ?? null);

  readonly upCount = computed(() => this.services().filter(s => s.status === 'UP').length);
  readonly downCount = computed(() => this.services().filter(s => s.status === 'DOWN').length);
  readonly degradedCount = computed(() => this.services().filter(s => s.status === 'DEGRADED').length);
  readonly totalServices = computed(() => this.services().length);

  readonly isStale = computed(() => {
    const checked = this.checkedAt();
    if (!checked) return false;
    const diff = Date.now() - new Date(checked).getTime();
    return diff > 5 * 60 * 1000; // stale if > 5 minutes old
  });

  readonly pgConnectionPercent = computed(() => {
    const pg = this.infra()?.postgresql;
    if (!pg || !pg.maxConnections) return 0;
    return Math.round((pg.connections / pg.maxConnections) * 100);
  });

  ngOnInit(): void {
    this.loadHealth();
  }

  loadHealth(): void {
    this.loading.set(true);
    this.error.set(null);

    this.adminOps.getSystemHealth()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.healthData.set(response.data);
          } else {
            this.error.set(response.message ?? 'Failed to load health data');
          }
        },
        error: () => {
          this.error.set('Could not connect to health endpoint');
        }
      });
  }

  statusIcon(status: ServiceHealthStatus | OverallHealthStatus | string): string {
    switch (status) {
      case 'UP': return 'check-circle';
      case 'DOWN': return 'x-circle';
      case 'DEGRADED': return 'alert-triangle';
      default: return 'help-circle';
    }
  }

  statusTone(status: ServiceHealthStatus | OverallHealthStatus | string): string {
    switch (status) {
      case 'UP': return 'success';
      case 'DOWN': return 'danger';
      case 'DEGRADED': return 'warning';
      default: return 'muted';
    }
  }

  responseTimeTone(ms: number): string {
    if (ms < 200) return 'success';
    if (ms < 1000) return 'warning';
    return 'danger';
  }

  pgConnectionTone(percent: number): string {
    if (percent < 60) return 'success';
    if (percent < 85) return 'warning';
    return 'danger';
  }

  kafkaLagTone(lag: number): string {
    if (lag < 100) return 'success';
    if (lag < 1000) return 'warning';
    return 'danger';
  }
}
