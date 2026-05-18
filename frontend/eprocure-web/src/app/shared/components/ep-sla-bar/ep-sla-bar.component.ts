import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'ep-sla-bar',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './ep-sla-bar.component.html',
  styleUrl: './ep-sla-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpSlaBarComponent {
  readonly assignedAt = input.required<string>();
  readonly deadline = input.required<string>();

  readonly percent = computed(() => {
    const start = new Date(this.assignedAt()).getTime();
    const end = new Date(this.deadline()).getTime();
    const now = Date.now();
    const total = Math.max(end - start, 1);
    const elapsed = Math.min(Math.max(now - start, 0), total);

    return Math.round((elapsed / total) * 100);
  });

  readonly tone = computed(() => {
    const percent = this.percent();

    if (percent >= 85) {
      return 'danger';
    }

    if (percent >= 60) {
      return 'warning';
    }

    return 'success';
  });
}
