import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'ep-skeleton',
  standalone: true,
  templateUrl: './ep-skeleton.component.html',
  styleUrl: './ep-skeleton.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpSkeletonComponent {
  readonly rows = input(3);
  readonly skeletonRows = computed(() => Array.from({ length: this.rows() }));
}
